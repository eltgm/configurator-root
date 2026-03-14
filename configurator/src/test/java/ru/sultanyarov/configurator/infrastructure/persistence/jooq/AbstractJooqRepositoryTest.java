package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import org.h2.jdbcx.JdbcDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;
import java.util.UUID;

abstract class AbstractJooqRepositoryTest {

    protected DSLContext dslContext;

    @BeforeEach
    void setUpDslContext() {
        dslContext = DSL.using(createDataSource(), SQLDialect.H2);
    }

    private DataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("sa");

        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
                new ClassPathResource("db/migration/V1__init.sql"),
                new ClassPathResource("db/migration/V2__CON1-26-remove-constraint.sql")
        );
        populator.execute(dataSource);
        return dataSource;
    }
}
