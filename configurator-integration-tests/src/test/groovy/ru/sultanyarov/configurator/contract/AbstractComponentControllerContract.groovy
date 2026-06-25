package ru.sultanyarov.configurator.contract

import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest
import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeValueInput
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ErrorResponse
import spock.lang.Specification

abstract class AbstractComponentControllerContract extends Specification implements ApiTestSupport {

    def "should create component successfully"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")
                .brand("Keychron")
                .description("75% keyboard kit")
                .attributes([
                        new AttributeValueInput(101L, "ANSI"),
                        new AttributeValueInput(102L, "75%")
                ])

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 201
        def responseBody = objectMapper.readValue(result.body, Component)
        responseBody.getId() != null
        responseBody.getComponentTypeId() == 1L
        responseBody.getName() == "Keychron Q1"
        responseBody.getBrand() == "Keychron"
        responseBody.getDescription() == "75% keyboard kit"
        responseBody.getArchived() == false
        responseBody.getCreatedAt() != null
        responseBody.getImages() != null
        responseBody.getImages().isEmpty()
        responseBody.getAttributes().size() == 2
        responseBody.getAttributes()*.getAttributeDefinitionId().containsAll([101L, 102L])
        responseBody.getAttributes()*.getName().containsAll(["layout", "form_factor"])
        responseBody.getAttributes()*.getLabel().containsAll(["Layout", "Form factor"])
        responseBody.getAttributes()*.getValue().containsAll(["ANSI", "75%"])
    }

    def "should return bad request when creating component with blank name"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql")
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("   ")

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 400
        def errorResponse = objectMapper.readValue(result.body, ErrorResponse)
        errorResponse.getMessage() != null
    }

    def "should return not found when creating component for non-existent component type"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def createRequest = new CreateComponentRequest()
                .componentTypeId(999999L)
                .name("Keychron Q1")

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 404
    }

    def "should return conflict when creating component with duplicate name in same component type"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 409
    }

    def "should return bad request when request contains duplicate attribute ids"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")
                .attributes([
                        new AttributeValueInput(101L, "ANSI"),
                        new AttributeValueInput(101L, "ISO")
                ])

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 400
    }

    def "should return bad request when request contains invalid attribute value"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")
                .attributes([
                        new AttributeValueInput(101L, "INVALID"),
                        new AttributeValueInput(102L, "75%")
                ])

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 400
    }

    def "should return bad request when required attributes are missing"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")
                .attributes([
                        new AttributeValueInput(101L, "ANSI")
                ])

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 400
    }

    def "should return not found when request contains non-existent attribute"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")
                .attributes([
                        new AttributeValueInput(999999L, "ANSI"),
                        new AttributeValueInput(102L, "75%")
                ])

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 400
    }

    def "should return bad request when attribute belongs to another component type"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-second-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql",
                "/sql/insert-second-test-attribute-definition.sql"
        )
        def createRequest = new CreateComponentRequest()
                .componentTypeId(1L)
                .name("Keychron Q1")
                .attributes([
                        new AttributeValueInput(101L, "ANSI"),
                        new AttributeValueInput(102L, "75%"),
                        new AttributeValueInput(201L, "Linear")
                ])

        when:
        def result = post("/components", createRequest)

        then:
        result.status == 400
    }

    def "should get components by domain with pagination"() {
        given:
        prepareComponentSearchData()

        when:
        def result = get("/domains/1/components", [page: 1, size: 2])

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ComponentPage)
        responseBody.getPage() == 1
        responseBody.getSize() == 2
        responseBody.getTotalItems() == 3
        responseBody.getItems()*.getId() == [3L]
        responseBody.getItems().every { it.getComponentTypeId() in [1L, 2L] }
    }

    def "should filter domain components by component type"() {
        given:
        prepareComponentSearchData()

        when:
        def result = get("/domains/1/components", [componentTypeId: 1, page: 0, size: 10])

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ComponentPage)
        responseBody.getTotalItems() == 2
        responseBody.getItems()*.getId() == [1L, 2L]
        responseBody.getItems().every { it.getComponentTypeId() == 1L }
    }

    def "should filter domain components by exact name"() {
        given:
        prepareComponentSearchData()

        when:
        def result = get("/domains/1/components", [name: "Keychron K2", page: 0, size: 10])

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ComponentPage)
        responseBody.getTotalItems() == 1
        responseBody.getItems().size() == 1
        responseBody.getItems().first().getId() == 2L
        responseBody.getItems().first().getName() == "Keychron K2"
    }

    def "should return not found when getting components for non-existent domain"() {
        given:
        runSqlScripts("/sql/clear-db.sql")

        expect:
        get("/domains/999999/components", [page: 0, size: 10]).status == 404
    }

    def "should return bad request when component type belongs to another domain"() {
        given:
        prepareComponentSearchData()

        expect:
        get("/domains/1/components", [componentTypeId: 3, page: 0, size: 10]).status == 400
    }

    private void prepareComponentSearchData() {
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-second-test-component-type.sql",
                "/sql/insert-test-component.sql",
                "/sql/insert-component-search-data.sql"
        )
    }
}
