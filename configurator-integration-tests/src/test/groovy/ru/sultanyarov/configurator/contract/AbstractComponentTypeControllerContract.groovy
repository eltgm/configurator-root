package ru.sultanyarov.configurator.contract

import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentType
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentTypeRequest
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateDomainRequest
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Domain
import spock.lang.Specification

abstract class AbstractComponentTypeControllerContract extends Specification implements ApiTestSupport {

    def "should create component type successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def domain = createDomain()
        def createRequest = new CreateComponentTypeRequest()
                .name("Test Component Type")
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)

        when:
        def result = post("/domains/${domain.getId()}/component-types", createRequest)

        then:
        result.status == 201
        def responseBody = objectMapper.readValue(result.body, ComponentType)
        responseBody.getId() != null
        responseBody.getDomainId() == domain.getId()
        responseBody.getName() == "Test Component Type"
        responseBody.getCode() == "TEST_CODE"
        responseBody.getDescription() == "Test Description"
        responseBody.getOrderIndex() == 1
    }

    def "should return bad request when creating component type with empty name"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def domain = createDomain()
        def createRequest = new CreateComponentTypeRequest()
                .name("")
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)

        expect:
        post("/domains/${domain.getId()}/component-types", createRequest).status == 400
    }

    def "should return bad request when creating component type with null name"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def domain = createDomain()
        def createRequest = new CreateComponentTypeRequest()
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)

        expect:
        post("/domains/${domain.getId()}/component-types", createRequest).status == 400
    }

    def "should return not found when creating component type for non-existent domain"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def createRequest = new CreateComponentTypeRequest()
                .name("Test Component Type")
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)

        expect:
        post("/domains/999999/component-types", createRequest).status == 404
    }

    def "should return conflict when creating component type with duplicate name in same domain"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def domain = createDomain()
        post("/domains/${domain.getId()}/component-types", new CreateComponentTypeRequest()
                .name("Test Component Type")
                .code("TEST_CODE_1")
                .description("Test Description 1")
                .orderIndex(1))

        def duplicateRequest = new CreateComponentTypeRequest()
                .name("Test Component Type")
                .code("TEST_CODE_2")
                .description("Test Description 2")
                .orderIndex(2)

        expect:
        post("/domains/${domain.getId()}/component-types", duplicateRequest).status == 409
    }

    def "should get component types by domain id successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def domain = createDomain()
        3.times { i ->
            post("/domains/${domain.getId()}/component-types", new CreateComponentTypeRequest()
                    .name("Component Type ${i}")
                    .code("CODE_${i}")
                    .description("Description ${i}")
                    .orderIndex(i))
        }

        when:
        def result = get("/domains/${domain.getId()}/component-types")

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ComponentType[].class)
        responseBody.length == 3
        responseBody.each { componentType ->
            componentType.getDomainId() == domain.getId()
        }
        responseBody*.getName().sort() == ["Component Type 0", "Component Type 1", "Component Type 2"].sort()
    }

    def "should return not found when getting component types for non-existent domain"() {
        expect:
        get("/domains/999999/component-types").status == 404
    }

    def "should get component type by id successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def domain = createDomain()
        def created = objectMapper.readValue(post("/domains/${domain.getId()}/component-types", new CreateComponentTypeRequest()
                .name("Test Component Type")
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)).body, ComponentType)

        when:
        def getResult = get("/component-types/${created.getId()}")

        then:
        getResult.status == 200
        def responseBody = objectMapper.readValue(getResult.body, ComponentType)
        responseBody.getId() == created.getId()
        responseBody.getDomainId() == domain.getId()
        responseBody.getName() == "Test Component Type"
        responseBody.getCode() == "TEST_CODE"
        responseBody.getDescription() == "Test Description"
        responseBody.getOrderIndex() == 1
    }

    def "should return not found when getting non-existent component type"() {
        expect:
        get("/component-types/999999").status == 404
    }

    def "should update component type successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def domain = createDomain()
        def created = objectMapper.readValue(post("/domains/${domain.getId()}/component-types", new CreateComponentTypeRequest()
                .name("Original Component Type")
                .code("ORIGINAL_CODE")
                .description("Original Description")
                .orderIndex(1)).body, ComponentType)

        def updateRequest = new CreateComponentTypeRequest()
                .name("Updated Component Type")
                .code("UPDATED_CODE")
                .description("Updated Description")
                .orderIndex(2)

        when:
        def result = put("/component-types/${created.getId()}", updateRequest)

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ComponentType)
        responseBody.getId() == created.getId()
        responseBody.getDomainId() == domain.getId()
        responseBody.getName() == "Updated Component Type"
        responseBody.getCode() == "UPDATED_CODE"
        responseBody.getDescription() == "Updated Description"
        responseBody.getOrderIndex() == 2
    }

    def "should return not found when updating non-existent component type"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def updateRequest = new CreateComponentTypeRequest()
                .name("Updated Component Type")
                .code("UPDATED_CODE")
                .description("Updated Description")
                .orderIndex(2)

        expect:
        put("/component-types/999999", updateRequest).status == 404
    }

    def "should return bad request when updating component type with empty name"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def domain = createDomain()
        def created = objectMapper.readValue(post("/domains/${domain.getId()}/component-types", new CreateComponentTypeRequest()
                .name("Original Component Type")
                .code("ORIGINAL_CODE")
                .description("Original Description")
                .orderIndex(1)).body, ComponentType)

        def updateRequest = new CreateComponentTypeRequest()
                .name("")
                .code("UPDATED_CODE")
                .description("Updated Description")
                .orderIndex(2)

        expect:
        put("/component-types/${created.getId()}", updateRequest).status == 400
    }

    def "should delete component type successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def domain = createDomain()
        def created = objectMapper.readValue(post("/domains/${domain.getId()}/component-types", new CreateComponentTypeRequest()
                .name("Component Type To Delete")
                .code("DELETE_CODE")
                .description("Description")
                .orderIndex(1)).body, ComponentType)

        when:
        def result = delete("/component-types/${created.getId()}")

        then:
        result.status == 204
        get("/component-types/${created.getId()}").status == 404
    }

    def "should return not found when deleting non-existent component type"() {
        expect:
        delete("/component-types/999999").status == 404
    }

    private Domain createDomain() {
        return objectMapper.readValue(post("/domains", new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")).body, Domain)
    }
}
