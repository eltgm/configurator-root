package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import java.util.UUID;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

abstract class AbstractJooqRepositoryTest {

  protected DSLContext dslContext;

  @BeforeEach
  void setUpDslContext() {
    dslContext = DSL.using(createDataSource(), SQLDialect.H2);
  }

  private DataSource createDataSource() {
    JdbcDataSource dataSource = new JdbcDataSource();
    dataSource.setURL(
        "jdbc:h2:mem:"
            + UUID.randomUUID()
            + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    dataSource.setUser("sa");
    dataSource.setPassword("sa");

    ResourceDatabasePopulator populator =
        new ResourceDatabasePopulator(
            new ClassPathResource("db/migration/V1__init.sql"),
            new ClassPathResource("db/migration/V2__CON1-26-remove-constraint.sql"),
            new ClassPathResource("db/migration/V3__CON1-68-enforce-component-name-uniqueness.sql"),
            new ClassPathResource("db/migration/V4__CON1-75-create-compatibility-rules.sql"),
            new ClassPathResource("db/migration/V5__CON1-82-create-system-user.sql"),
            new ClassPathResource("db/migration/V6__CON1-100-store-component-image-object-key.sql"),
            new ClassPathResource("db/migration/V7__create-domain-attribute-catalog.sql"));
    populator.execute(dataSource);
    return dataSource;
  }
}
