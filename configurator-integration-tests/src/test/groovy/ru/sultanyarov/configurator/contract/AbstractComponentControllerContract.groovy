package ru.sultanyarov.configurator.contract

import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest
import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeValueInput
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ErrorResponse
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateComponentRequest
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

    def "should fully update component and replace attribute values while preserving images"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = new UpdateComponentRequest(
                1L,
                "Keychron Q1 Pro",
                [
                        new AttributeValueInput(101L, "ISO"),
                        new AttributeValueInput(102L, "TKL")
                ]
        )
                .brand("Updated brand")
                .description("Updated description")

        when:
        def result = put("/components/1", updateRequest)

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, Component)
        responseBody.getId() == 1L
        responseBody.getComponentTypeId() == 1L
        responseBody.getName() == "Keychron Q1 Pro"
        responseBody.getBrand() == "Updated brand"
        responseBody.getDescription() == "Updated description"
        responseBody.getArchived() == false
        responseBody.getCreatedAt() != null
        responseBody.getAttributes().size() == 2
        responseBody.getAttributes()*.getAttributeDefinitionId().toSet() == [101L, 102L].toSet()
        responseBody.getAttributes()*.getValue().toSet() == ["ISO", "TKL"].toSet()
        responseBody.getImages().size() == 1
        responseBody.getImages().first().getId() == 501L
        responseBody.getImages().first().getUrl() == "/files/components/1/main.jpg"
    }

    def "should clear nullable fields when updating component"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = new UpdateComponentRequest(
                1L,
                "Keychron Q1",
                [
                        new AttributeValueInput(101L, "ANSI"),
                        new AttributeValueInput(102L, "75%")
                ]
        )

        when:
        def result = put("/components/1", updateRequest)

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, Component)
        responseBody.getBrand() == null
        responseBody.getDescription() == null
    }

    def "should trim component name when updating"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = new UpdateComponentRequest(
                1L,
                "Updated component ",
                [
                        new AttributeValueInput(101L, "ANSI"),
                        new AttributeValueInput(102L, "75%")
                ]
        )

        when:
        def result = put("/components/1", updateRequest)

        then:
        result.status == 200
        objectMapper.readValue(result.body, Component).getName() == "Updated component"
    }

    def "should return not found when updating non-existent component"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest()

        expect:
        put("/components/999999", updateRequest).status == 404
    }

    def "should return bad request when changing component type"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().componentTypeId(2L)

        when:
        def result = put("/components/1", updateRequest)

        then:
        result.status == 400
        objectMapper.readValue(result.body, ErrorResponse).getMessage() != null
    }

    def "should return conflict when updated name belongs to another component of the same type"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().name("Existing component name")

        expect:
        put("/components/1", updateRequest).status == 409
    }

    def "should return bad request when update contains duplicate attribute ids"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().attributes([
                new AttributeValueInput(101L, "ANSI"),
                new AttributeValueInput(101L, "ISO"),
                new AttributeValueInput(102L, "75%")
        ])

        expect:
        put("/components/1", updateRequest).status == 400
    }

    def "should return bad request when update contains invalid attribute value"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().attributes([
                new AttributeValueInput(101L, "INVALID"),
                new AttributeValueInput(102L, "75%")
        ])

        expect:
        put("/components/1", updateRequest).status == 400
    }

    def "should return bad request when update misses required attributes"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().attributes([
                new AttributeValueInput(101L, "ANSI")
        ])

        expect:
        put("/components/1", updateRequest).status == 400
    }

    def "should return bad request when update contains attribute of another component type"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().attributes([
                new AttributeValueInput(101L, "ANSI"),
                new AttributeValueInput(102L, "75%"),
                new AttributeValueInput(201L, "Linear")
        ])

        expect:
        put("/components/1", updateRequest).status == 400
    }

    def "should return bad request when update attributes are omitted"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = [
                componentTypeId: 1L,
                name: "Updated component"
        ]

        expect:
        put("/components/1", updateRequest).status == 400
    }

    def "should return bad request when updated name is blank"() {
        given:
        prepareComponentUpdateData()
        def updateRequest = validUpdateRequest().name("   ")

        expect:
        put("/components/1", updateRequest).status == 400
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

    private void prepareComponentUpdateData() {
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-second-test-component-type.sql",
                "/sql/insert-test-component-creation-attributes.sql",
                "/sql/insert-second-test-attribute-definition.sql",
                "/sql/insert-test-component.sql",
                "/sql/insert-test-component-update-data.sql"
        )
    }

    private static UpdateComponentRequest validUpdateRequest() {
        return new UpdateComponentRequest(
                1L,
                "Updated component",
                [
                        new AttributeValueInput(101L, "ANSI"),
                        new AttributeValueInput(102L, "75%")
                ]
        )
    }
}
