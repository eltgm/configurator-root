package ru.sultanyarov.configurator.external

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.restassured.RestAssured
import io.restassured.http.ContentType
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import ru.sultanyarov.configurator.contract.ApiTestSupport
import ru.sultanyarov.configurator.contract.TestResponse
import spock.lang.Shared
import spock.lang.Specification

abstract class BaseExternalIntegrationSpec extends Specification implements ApiTestSupport {

    @Shared
    String baseUrl = System.getProperty("test.baseUrl", "http://localhost:8080")

    @Shared
    String dbUrl = System.getProperty("test.dbUrl", "jdbc:postgresql://localhost:5432/configurator")

    @Shared
    String dbUser = System.getProperty("test.dbUser", "configurator")

    @Shared
    String dbPassword = System.getProperty("test.dbPassword", "configurator")

    final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Override
    void runSqlScripts(String... scripts) {
        def dataSource = new DriverManagerDataSource(dbUrl, dbUser, dbPassword)
        dataSource.setDriverClassName("org.postgresql.Driver")
        def populator = new ResourceDatabasePopulator()
        scripts.each { script ->
            populator.addScript(new ClassPathResource(script.startsWith("/") ? script.substring(1) : script))
        }
        populator.execute(dataSource)
    }

    @Override
    TestResponse get(String path) {
        def response = RestAssured.given()
                .baseUri(baseUrl)
                .accept(ContentType.JSON)
                .when()
                .get(path)
                .then()
                .extract()
                .response()
        return new TestResponse(response.statusCode(), response.asString())
    }

    @Override
    TestResponse get(String path, Map<String, ?> queryParams) {
        def response = RestAssured.given()
                .baseUri(baseUrl)
                .accept(ContentType.JSON)
                .queryParams(queryParams.collectEntries { key, value -> [(key): String.valueOf(value)] })
                .when()
                .get(path)
                .then()
                .extract()
                .response()
        return new TestResponse(response.statusCode(), response.asString())
    }

    @Override
    TestResponse post(String path, Object body) {
        def response = RestAssured.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(objectMapper.writeValueAsString(body))
                .when()
                .post(path)
                .then()
                .extract()
                .response()
        return new TestResponse(response.statusCode(), response.asString())
    }

    @Override
    TestResponse put(String path, Object body) {
        def response = RestAssured.given()
                .baseUri(baseUrl)
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(objectMapper.writeValueAsString(body))
                .when()
                .put(path)
                .then()
                .extract()
                .response()
        return new TestResponse(response.statusCode(), response.asString())
    }

    @Override
    TestResponse delete(String path) {
        def response = RestAssured.given()
                .baseUri(baseUrl)
                .accept(ContentType.JSON)
                .when()
                .delete(path)
                .then()
                .extract()
                .response()
        return new TestResponse(response.statusCode(), response.asString())
    }
}
