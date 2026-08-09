package ru.sultanyarov.configurator.contract

import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationExport
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationPage
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ModelConfiguration
import spock.lang.Specification

abstract class AbstractConfigurationControllerContract extends Specification implements ApiTestSupport {

    def "should create configuration with direct manual compatibility"() {
        given:
        prepareData()

        when:
        def result = post("/domains/1/configurations", request("  Manual build  ", "   ", [1L, 3L]))

        then:
        result.status == 201
        def body = objectMapper.readValue(result.body, ModelConfiguration)
        body.id != null
        body.domainId == 1L
        body.name == "Manual build"
        body.description == null
        body.createdAt != null
        body.components*.id == [1L, 3L]
        body.components*.componentTypeName == ["Processor", "Motherboard"]
        body.components.every { !it.archived }
    }

    def "should create configuration using automatic compatibility only"() {
        given:
        prepareData()
        runSqlScripts("/sql/remove-manual-compatibility-1-2.sql")

        expect:
        post("/domains/1/configurations", request("Automatic build", null, [1L, 2L])).status == 201
    }

    def "should reject components of the same type"() {
        given:
        prepareData()

        expect:
        post("/domains/1/configurations", request("Invalid build", null, [2L, 3L])).status == 409
    }

    def "should reject archived component"() {
        given:
        prepareData()

        expect:
        post("/domains/1/configurations", request("Archived build", null, [1L, 4L])).status == 409
    }

    def "should reject component from another domain"() {
        given:
        prepareData()

        expect:
        post("/domains/1/configurations", request("Foreign build", null, [1L, 7L])).status == 400
    }

    def "should reject incompatible and transitive-only component sets"() {
        given:
        prepareData()

        expect:
        post("/domains/1/configurations", request("Incompatible", null, [1L, 8L])).status == 409
        post("/domains/1/configurations", request("Transitive", null, [1L, 3L, 9L])).status == 409
        objectMapper.readValue(
                get("/domains/1/configurations").body,
                ConfigurationPage
        ).totalItems == 0
    }

    def "should reject empty component set and blank name"() {
        given:
        prepareData()

        expect:
        post("/domains/1/configurations", request("Empty", null, [])).status == 400
        post("/domains/1/configurations", request("   ", null, [1L])).status == 400
        post("/domains/1/configurations", request("Duplicate", null, [1L, 1L])).status == 400
    }

    def "should fully update configuration and preserve immutable metadata"() {
        given:
        prepareData()
        def original = createConfiguration("Initial", [1L, 3L])

        when:
        def result = put(
                "/configurations/${original.id}",
                request("  Updated build  ", "  Updated description  ", [1L, 2L])
        )

        then:
        result.status == 200
        def body = objectMapper.readValue(result.body, ModelConfiguration)
        body.id == original.id
        body.domainId == original.domainId
        body.createdAt == original.createdAt
        body.name == "Updated build"
        body.description == "Updated description"
        body.components*.id == [1L, 2L]

        and: "the complete replacement is visible on a subsequent read"
        def persisted = objectMapper.readValue(
                get("/configurations/${original.id}").body,
                ModelConfiguration
        )
        persisted.name == "Updated build"
        persisted.components*.id == [1L, 2L]
    }

    def "should normalize blank description during configuration update"() {
        given:
        prepareData()
        def original = createConfiguration("Initial", [1L, 3L])

        when:
        def result = put(
                "/configurations/${original.id}",
                request("Updated", "   ", [1L, 3L])
        )

        then:
        result.status == 200
        objectMapper.readValue(result.body, ModelConfiguration).description == null
    }

    def "should strictly reject archived component already present in configuration"() {
        given:
        prepareData()
        def original = createConfiguration("Initial", [1L, 3L])
        runSqlScripts("/sql/archive-configurator-component-3.sql")

        when:
        def result = put(
                "/configurations/${original.id}",
                request("Must not persist", null, [1L, 3L])
        )

        then:
        result.status == 409

        and: "validation failure leaves the original configuration unchanged"
        def persisted = objectMapper.readValue(
                get("/configurations/${original.id}").body,
                ModelConfiguration
        )
        persisted.name == "Initial"
        persisted.components*.id == [1L, 3L]
        persisted.components.find { it.id == 3L }.archived
    }

    def "should reject invalid component sets without partially updating configuration"() {
        given:
        prepareData()
        def original = createConfiguration("Initial", [1L, 3L])

        expect:
        put("/configurations/${original.id}", request("Foreign", null, [1L, 7L])).status == 400
        put("/configurations/${original.id}", request("Missing", null, [1L, 999L])).status == 404
        put("/configurations/${original.id}", request("Archived", null, [1L, 4L])).status == 409
        put("/configurations/${original.id}", request("Same type", null, [2L, 3L])).status == 409
        put("/configurations/${original.id}", request("Incompatible", null, [1L, 8L])).status == 409
        put("/configurations/${original.id}", request("Empty", null, [])).status == 400
        put("/configurations/${original.id}", request("   ", null, [1L])).status == 400
        put("/configurations/${original.id}", request("Duplicate", null, [1L, 1L])).status == 400

        and:
        def persisted = objectMapper.readValue(
                get("/configurations/${original.id}").body,
                ModelConfiguration
        )
        persisted.name == "Initial"
        persisted.components*.id == [1L, 3L]
    }

    def "should hide missing and foreign-owned configuration during update"() {
        given:
        prepareData()
        runSqlScripts("/sql/insert-foreign-owned-configuration.sql")

        expect:
        put("/configurations/999", request("Missing", null, [1L])).status == 404
        put("/configurations/900", request("Foreign", null, [1L])).status == 404
        put("/configurations/0", request("Invalid", null, [1L])).status == 400
    }

    def "should return newest owned configurations with pagination"() {
        given:
        prepareData()
        def first = createConfiguration("First", [1L, 3L])
        def second = createConfiguration("Second", [1L, 2L])

        when:
        def result = get("/domains/1/configurations", [page: 0, size: 1])

        then:
        result.status == 200
        def page = objectMapper.readValue(result.body, ConfigurationPage)
        page.page == 0
        page.size == 1
        page.totalItems == 2
        page.items*.id == [second.id]
        get("/configurations/${first.id}").status == 200
    }

    def "should keep archived components visible in saved configuration"() {
        given:
        prepareData()
        def configuration = createConfiguration("Archived later", [1L, 3L])
        runSqlScripts("/sql/archive-configurator-component-3.sql")

        when:
        def result = get("/configurations/${configuration.id}")

        then:
        result.status == 200
        def body = objectMapper.readValue(result.body, ModelConfiguration)
        body.components.find { it.id == 3L }.archived
    }

    def "should export versioned configuration as JSON attachment"() {
        given:
        prepareData()
        def configuration = createConfiguration("Exported", [1L, 3L])

        when:
        def result = get("/configurations/${configuration.id}/export/json")

        then:
        result.status == 200
        result.headers["Content-Disposition"] == "attachment; filename=\"configuration-${configuration.id}.json\""
        def body = objectMapper.readValue(result.body, ConfigurationExport)
        body.schemaVersion == 1
        body.exportedAt != null
        body.configuration.id == configuration.id
        body.configuration.components*.id == [1L, 3L]
    }

    def "should return not found for missing domain component and configuration"() {
        given:
        prepareData()

        expect:
        get("/domains/999/configurations").status == 404
        post("/domains/1/configurations", request("Missing component", null, [999L])).status == 404
        get("/configurations/999").status == 404
        get("/configurations/999/export/json").status == 404
    }

    def "should hide configuration owned by another user"() {
        given:
        prepareData()
        runSqlScripts("/sql/insert-foreign-owned-configuration.sql")

        expect:
        get("/configurations/900").status == 404
        get("/configurations/900/export/json").status == 404
        objectMapper.readValue(
                get("/domains/1/configurations").body,
                ConfigurationPage
        ).totalItems == 0
    }

    private void prepareData() {
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-configurator-test-data.sql")
    }

    private ModelConfiguration createConfiguration(String name, List<Long> componentIds) {
        def response = post("/domains/1/configurations", request(name, null, componentIds))
        assert response.status == 201
        return objectMapper.readValue(response.body, ModelConfiguration)
    }

    private static Map<String, ?> request(String name, String description, List<Long> componentIds) {
        return [name: name, description: description, componentIds: componentIds]
    }
}
