package ru.sultanyarov.configurator.it

import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Specification

@SpringBootTest(classes = ru.sultanyarov.configurator.ConfiguratorApplication)
@Testcontainers
abstract class BaseIntegrationSpec extends Specification {

    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("configurator")
            .withUsername("configurator")
            .withPassword("configurator")

    static {
        postgres.start()
        System.setProperty("DB_URL", postgres.jdbcUrl)
        System.setProperty("DB_USER", postgres.username)
        System.setProperty("DB_PASSWORD", postgres.password)
    }
}
