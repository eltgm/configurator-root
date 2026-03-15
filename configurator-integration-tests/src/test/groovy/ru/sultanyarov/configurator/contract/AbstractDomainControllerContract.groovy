package ru.sultanyarov.configurator.contract

import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateDomainRequest
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Domain
import ru.sultanyarov.configurator.api.inbounds.rest.dto.DomainPage
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateDomainRequest
import spock.lang.Specification

abstract class AbstractDomainControllerContract extends Specification implements ApiTestSupport {

    def "should create domain successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def createRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")

        when:
        def result = post("/domains", createRequest)

        then:
        result.status == 201
        def responseBody = objectMapper.readValue(result.body, Domain)
        responseBody.getId() != null
        responseBody.getName() == "Test Domain"
        responseBody.getDescription() == "Test Description"
        responseBody.getCreatedAt() != null
    }

    def "should return bad request when creating domain with empty name"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def createRequest = new CreateDomainRequest()
                .name("")
                .description("Test Description")

        expect:
        post("/domains", createRequest).status == 400
    }

    def "should return bad request when creating domain with null name"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def createRequest = new CreateDomainRequest()
                .description("Test Description")

        expect:
        post("/domains", createRequest).status == 400
    }

    def "should get domain by id successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def createRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")
        def createdDomain = objectMapper.readValue(post("/domains", createRequest).body, Domain)

        when:
        def getResult = get("/domains/${createdDomain.getId()}")

        then:
        getResult.status == 200
        def responseBody = objectMapper.readValue(getResult.body, Domain)
        responseBody.getId() == createdDomain.getId()
        responseBody.getName() == "Test Domain"
        responseBody.getDescription() == "Test Description"
        responseBody.getCreatedAt() != null
    }

    def "should return not found when getting non-existent domain"() {
        expect:
        get("/domains/999999").status == 404
    }

    def "should update domain successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def createRequest = new CreateDomainRequest()
                .name("Original Domain")
                .description("Original Description")
        def createdDomain = objectMapper.readValue(post("/domains", createRequest).body, Domain)

        def updateRequest = new UpdateDomainRequest()
                .name("Updated Domain")
                .description("Updated Description")

        when:
        def updateResult = put("/domains/${createdDomain.getId()}", updateRequest)

        then:
        updateResult.status == 200
        def responseBody = objectMapper.readValue(updateResult.body, Domain)
        responseBody.getId() == createdDomain.getId()
        responseBody.getName() == "Updated Domain"
        responseBody.getDescription() == "Updated Description"
        responseBody.getCreatedAt() != null
    }

    def "should return not found when updating non-existent domain"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def updateRequest = new UpdateDomainRequest()
                .name("Updated Domain")
                .description("Updated Description")

        expect:
        put("/domains/999999", updateRequest).status == 404
    }

    def "should return bad request when updating domain with empty name"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def createdDomain = objectMapper.readValue(post("/domains", new CreateDomainRequest()
                .name("Original Domain")
                .description("Original Description")).body, Domain)

        def updateRequest = new UpdateDomainRequest()
                .name("")
                .description("Updated Description")

        expect:
        put("/domains/${createdDomain.getId()}", updateRequest).status == 400
    }

    def "should delete domain successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def createdDomain = objectMapper.readValue(post("/domains", new CreateDomainRequest()
                .name("Domain to Delete")
                .description("Description")).body, Domain)

        when:
        def deleteResult = delete("/domains/${createdDomain.getId()}")

        then:
        deleteResult.status == 204
        get("/domains/${createdDomain.getId()}").status == 404
    }

    def "should return not found when deleting non-existent domain"() {
        expect:
        delete("/domains/999999").status == 404
    }

    def "should get domains with pagination"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        5.times { i ->
            post("/domains", new CreateDomainRequest()
                    .name("Domain ${i}")
                    .description("Description ${i}"))
        }

        when:
        def result = get("/domains", [page: "0", size: "3"])

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, DomainPage)
        responseBody.getItems().size() == 3
        responseBody.getPage() == 0
        responseBody.getSize() == 3
        responseBody.getTotalItems() >= 5
    }
}
