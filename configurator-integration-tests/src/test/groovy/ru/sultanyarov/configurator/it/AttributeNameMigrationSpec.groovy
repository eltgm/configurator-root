package ru.sultanyarov.configurator.it

import groovy.sql.Sql
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.FlywayException
import org.testcontainers.postgresql.PostgreSQLContainer
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class AttributeNameMigrationSpec extends Specification {
    @Shared
    PostgreSQLContainer postgres = new PostgreSQLContainer('postgres:17-alpine')

    Sql sql
    String schema

    def setupSpec() { postgres.start() }
    def cleanupSpec() { postgres.stop() }

    def setup() {
        schema = 'migration_' + UUID.randomUUID().toString().replace('-', '')
        sql = Sql.newInstance(postgres.jdbcUrl, postgres.username, postgres.password, 'org.postgresql.Driver')
        sql.execute('CREATE SCHEMA ' + schema)
        sql.execute('SET search_path TO ' + schema)
    }

    def cleanup() {
        sql.execute('DROP SCHEMA ' + schema + ' CASCADE')
        sql.close()
    }

    private Flyway flyway(String target = 'latest') {
        Flyway.configure().dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                .defaultSchema(schema).schemas(schema).target(target).load()
    }

    def 'domain deletion migration preserves existing configurations and replaces cascade with restrict'() {
        given:
        flyway('8').migrate()
        sql.execute("INSERT INTO domain (id, name, created_by_user_id) VALUES (1, 'Preserved', -1)")
        sql.execute("INSERT INTO configuration (id, domain_id, name, created_by_user_id) VALUES (1, 1, 'Build', -1)")

        when:
        flyway().migrate()
        sql.execute('DELETE FROM domain WHERE id = 1')

        then:
        def error = thrown(java.sql.SQLException)
        error.SQLState == '23503'
        sql.firstRow('SELECT count(*) AS n FROM configuration').n == 1
        sql.firstRow('SELECT count(*) AS n FROM domain').n == 1
        sql.firstRow('SELECT count(*) AS n FROM component_image_cleanup').n == 0
    }

    def 'fresh install enforces exact domain scoped names at the database boundary'() {
        given:
        flyway().migrate()
        sql.execute("INSERT INTO domain (id, name, created_by_user_id) VALUES (1, 'One', -1), (2, 'Two', -1)")
        sql.execute("""INSERT INTO attribute_definition (id, domain_id, name, label, data_type)
            VALUES (101, 1, 'socket', 'Socket', 'STRING'),
                   (201, 2, 'socket', 'Socket', 'STRING'),
                   (102, 1, 'Socket', 'Socket', 'STRING')""")

        when:
        sql.execute("INSERT INTO attribute_definition (domain_id, name, label, data_type) VALUES (1, 'socket', 'Other', 'NUMBER')")

        then:
        def error = thrown(java.sql.SQLException)
        error.SQLState == '23505'
        error.message.contains('ux_attribute_definition_domain_name')
        sql.firstRow('SELECT count(*) AS n FROM attribute_definition').n == 3
    }

    def 'upgrade merges equivalent definitions and preserves links values and both rule sides'() {
        given:
        seedDuplicates()
        // Equal link settings can be coalesced; settings for different types stay independent.
        sql.execute('INSERT INTO component_type_attribute (component_type_id, attribute_definition_id, is_required, order_index) VALUES (10, 201, true, 0)')

        when:
        flyway().migrate()

        then:
        sql.rows('SELECT id FROM attribute_definition ORDER BY id')*.id == [101L, 301L]
        sql.rows('SELECT component_type_id, attribute_definition_id, is_required, order_index FROM component_type_attribute ORDER BY component_type_id') == [
                [component_type_id: 10L, attribute_definition_id: 101L, is_required: true, order_index: 0],
                [component_type_id: 20L, attribute_definition_id: 101L, is_required: false, order_index: 5]
        ]
        sql.rows('SELECT id, attribute_definition_id, value_string FROM attribute_value ORDER BY id') == [
                [id: 1L, attribute_definition_id: 101L, value_string: 'AM5'],
                [id: 2L, attribute_definition_id: 101L, value_string: 'AM4']
        ]
        sql.firstRow('SELECT archived FROM component WHERE id = 2').archived
        sql.rows('SELECT id, left_attribute_definition_id, right_attribute_definition_id, operator, order_index FROM compatibility_rule_condition ORDER BY id') == [
                [id: 1L, left_attribute_definition_id: 101L, right_attribute_definition_id: 101L, operator: 'EQUALS', order_index: 0],
                [id: 2L, left_attribute_definition_id: 101L, right_attribute_definition_id: 101L, operator: 'NOT_EQUALS', order_index: 1]
        ]
        flyway().migrate().migrationsExecuted == 0
    }

    def 'upgrade converts the old demo layout from twelve definitions to nine with twelve links'() {
        given:
        flyway('7').migrate()
        sql.execute("INSERT INTO domain (id, name, created_by_user_id) VALUES (1, 'Legacy demo', -1)")
        (1..6).each { type -> sql.execute('INSERT INTO component_type (id, domain_id, name) VALUES (?, 1, ?)', [type, "Type " + type]) }
        def definitions = [
                [1, 'socket'], [1, 'tdp'], [2, 'socket'], [2, 'memory_standard'], [2, 'form_factor'],
                [3, 'memory_standard'], [3, 'capacity_gb'], [4, 'recommended_power'], [4, 'length_mm'],
                [5, 'power'], [6, 'form_factor'], [6, 'max_gpu_length_mm']
        ]
        definitions.eachWithIndex { definition, index ->
            sql.execute("INSERT INTO attribute_definition (id, domain_id, name, label, data_type) VALUES (?, 1, ?, ?, 'STRING')",
                    [index + 101, definition[1], definition[1]])
            sql.execute('INSERT INTO component_type_attribute (component_type_id, attribute_definition_id) VALUES (?, ?)', [definition[0], index + 101])
        }

        when:
        flyway().migrate()

        then:
        sql.firstRow('SELECT count(*) AS n FROM attribute_definition').n == 9
        sql.firstRow('SELECT count(*) AS n FROM component_type_attribute').n == 12
        sql.rows('SELECT attribute_definition_id FROM component_type_attribute WHERE attribute_definition_id IN (101, 104, 105) GROUP BY attribute_definition_id HAVING count(*) = 2').size() == 3
    }

    @Unroll
    def 'upgrade refuses #reason without partially changing the database'() {
        given:
        seedDuplicates()
        sql.execute(change)
        def definitions = sql.rows('SELECT * FROM attribute_definition ORDER BY id')
        def values = sql.rows('SELECT * FROM attribute_value ORDER BY id')
        def links = sql.rows('SELECT * FROM component_type_attribute ORDER BY component_type_id, attribute_definition_id')
        def conditions = sql.rows('SELECT * FROM compatibility_rule_condition ORDER BY id')

        when:
        flyway().migrate()

        then:
        def error = thrown(FlywayException)
        error.message.contains(diagnostic)
        error.message.contains('socket')
        sql.rows('SELECT * FROM attribute_definition ORDER BY id') == definitions
        sql.rows('SELECT * FROM attribute_value ORDER BY id') == values
        sql.rows('SELECT * FROM component_type_attribute ORDER BY component_type_id, attribute_definition_id') == links
        sql.rows('SELECT * FROM compatibility_rule_condition ORDER BY id') == conditions
        sql.firstRow('SELECT max(version::integer) AS version FROM flyway_schema_history WHERE success').version == 7

        where:
        reason          | change                                                                                                                                                                                   | diagnostic
        'labels'        | "UPDATE attribute_definition SET label = 'Different' WHERE id = 201"                                                                                                                       | 'Incompatible attribute definitions'
        'data types'    | "UPDATE attribute_definition SET data_type = 'STRING' WHERE id = 201"                                                                                                                      | 'Incompatible attribute definitions'
        'enum values'   | "UPDATE attribute_definition SET enum_values_json = '[\"AM5\"]' WHERE id = 201"                                                                                                            | 'Incompatible attribute definitions'
        'link settings' | 'INSERT INTO component_type_attribute (component_type_id, attribute_definition_id, is_required, order_index) VALUES (10, 201, false, 0)'                                                  | 'Conflicting attribute link settings'
        'values'        | "INSERT INTO attribute_value (id, component_id, attribute_definition_id, value_string) VALUES (3, 1, 201, 'AM4')"                                                                            | 'Conflicting attribute values'
        'conditions'    | "INSERT INTO compatibility_rule_condition (id, rule_set_id, left_attribute_definition_id, right_attribute_definition_id, operator, order_index) VALUES (3, 1, 101, 101, 'EQUALS', 2)" | 'Conflicting attribute rule conditions'
    }

    private void seedDuplicates() {
        flyway('7').migrate()
        sql.execute("INSERT INTO domain (id, name, created_by_user_id) VALUES (1, 'One', -1), (2, 'Two', -1)")
        sql.execute("INSERT INTO component_type (id, domain_id, name) VALUES (10, 1, 'CPU'), (20, 1, 'Board')")
        sql.execute("""INSERT INTO attribute_definition (id, domain_id, name, label, data_type, enum_values_json)
            VALUES (101, 1, 'socket', 'Socket', 'ENUM', '["AM4","AM5"]'),
                   (201, 1, 'socket', 'Socket', 'ENUM', '["AM5","AM4"]'),
                   (301, 2, 'socket', 'Other domain', 'STRING', NULL)""")
        sql.execute('''INSERT INTO component_type_attribute (component_type_id, attribute_definition_id, is_required, order_index)
            VALUES (10, 101, true, 0), (20, 201, false, 5)''')
        sql.execute("INSERT INTO component (id, component_type_id, name, archived) VALUES (1, 10, 'CPU', false), (2, 20, 'Board', true)")
        sql.execute("INSERT INTO attribute_value (id, component_id, attribute_definition_id, value_string) VALUES (1, 1, 101, 'AM5'), (2, 2, 201, 'AM4')")
        sql.execute("INSERT INTO compatibility_rule_set (id, domain_id, name, component_type_a_id, component_type_b_id) VALUES (1, 1, 'Socket rule', 10, 20)")
        sql.execute("""INSERT INTO compatibility_rule_condition (id, rule_set_id, left_attribute_definition_id, right_attribute_definition_id, operator, order_index)
            VALUES (1, 1, 101, 201, 'EQUALS', 0), (2, 1, 201, 101, 'NOT_EQUALS', 1)""")
    }
}
