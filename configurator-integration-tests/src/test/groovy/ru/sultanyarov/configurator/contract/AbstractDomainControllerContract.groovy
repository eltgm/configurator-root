package ru.sultanyarov.configurator.contract

import ru.sultanyarov.configurator.api.inbounds.rest.dto.ApiErrorCode
import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleSet
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleOperator
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentType
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationPage
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateDomainRequest
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Domain
import ru.sultanyarov.configurator.api.inbounds.rest.dto.DomainPage
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ErrorResponse
import ru.sultanyarov.configurator.api.inbounds.rest.dto.GraphResponse
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateDomainRequest
import spock.lang.Specification

abstract class AbstractDomainControllerContract extends Specification implements ApiTestSupport {

    def "should create complete demo PC domain and reject duplicate creation"() {
        given:
        runSqlScripts("/sql/clear-db.sql")

        when:
        def result = post("/domains/demo", null)

        then:
        result.status == 201
        def domain = objectMapper.readValue(result.body, Domain)
        domain.id != null
        domain.name == "Сборка ПК"
        domain.description
        domain.createdAt != null

        and: "the catalog contains six ordered types and twelve attribute definitions"
        def typesResult = get("/domains/${domain.id}/component-types")
        typesResult.status == 200
        def types = objectMapper.readerForListOf(ComponentType).readValue(typesResult.body)
        types*.code == ["CPU", "MOTHERBOARD", "MEMORY", "GPU", "PSU", "CASE"]
        types*.orderIndex == [0, 1, 2, 3, 4, 5]
        def attributes = types.collectMany { type ->
            def attributesResult = get("/component-types/${type.id}/attributes")
            assert attributesResult.status == 200
            objectMapper.readerForListOf(AttributeDefinition).readValue(attributesResult.body)
        }
        attributes.size() == 12
        attributes.every { it.getIsRequired() }

        and: "the catalog contains compatible and intentionally incompatible components"
        def componentResult = get("/domains/${domain.id}/components", [page: 0, size: 100])
        componentResult.status == 200
        def componentPage = objectMapper.readValue(componentResult.body, ComponentPage)
        componentPage.totalItems == 12
        componentPage.items*.name.containsAll([
                "Ryzen 5 7600",
                "Core i5-14600K",
                "GeForce RTX 4070 SUPER",
                "Radeon RX 7800 XT",
                "Pop Air",
                "MasterBox Q300L Compact"
        ])
        componentPage.items.every { !it.archived }

        and: "five automatic rules and ten manual graph edges are available"
        def rulesResult = get("/domains/${domain.id}/compatibility/rules")
        rulesResult.status == 200
        def rules = objectMapper.readerForListOf(CompatibilityRuleSet).readValue(rulesResult.body)
        rules.size() == 5
        rules.every { it.enabled && it.conditions.size() == 1 }
        rules*.conditions*.operator.flatten().count(CompatibilityRuleOperator.EQUALS) == 3
        rules*.conditions*.operator.flatten().count(CompatibilityRuleOperator.LTE) == 2
        def graphResult = get("/domains/${domain.id}/compatibility/graph")
        graphResult.status == 200
        def graph = objectMapper.readValue(graphResult.body, GraphResponse)
        graph.nodes.size() == 12
        graph.edges.size() == 10

        and: "the validated six-component saved build can be opened by the UI"
        def configurationsResult = get("/domains/${domain.id}/configurations", [page: 0, size: 10])
        configurationsResult.status == 200
        def configurations = objectMapper.readValue(configurationsResult.body, ConfigurationPage)
        configurations.totalItems == 1
        configurations.items.first().name == "Игровой ПК 1440p"
        configurations.items.first().components.size() == 6

        when:
        def duplicateResult = post("/domains/demo", null)

        then:
        duplicateResult.status == 409
        def error = objectMapper.readValue(duplicateResult.body, ErrorResponse)
        error.code == ApiErrorCode.ENTITY_ALREADY_EXISTS
        error.details.isEmpty()
        def domainsResult = get("/domains", [page: 0, size: 100])
        domainsResult.status == 200
        objectMapper.readValue(domainsResult.body, DomainPage).totalItems == 1
    }

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
