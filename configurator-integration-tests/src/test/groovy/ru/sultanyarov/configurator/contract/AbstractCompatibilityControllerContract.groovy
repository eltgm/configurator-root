package ru.sultanyarov.configurator.contract

import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityLink
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateCompatibilityLinkRequest
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ErrorResponse
import ru.sultanyarov.configurator.api.inbounds.rest.dto.GraphResponse
import spock.lang.Specification

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

abstract class AbstractCompatibilityControllerContract extends Specification implements ApiTestSupport {

    def "should return sorted compatibility graph with active nodes and scoped edges"() {
        given:
        prepareCompatibilityGraphData()

        when:
        def result = get("/domains/1/compatibility/graph")

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, GraphResponse)
        responseBody.nodes*.id == [1L, 2L, 3L, 7L]
        responseBody.nodes*.name == [
                "Keychron Q1",
                "Keychron K2",
                "Gateron Yellow",
                "Isolated Component"
        ]
        responseBody.nodes*.componentTypeId == [1L, 1L, 2L, 2L]
        responseBody.nodes*.componentTypeName == [
                "Test Component Type",
                "Test Component Type",
                "Second Component Type",
                "Second Component Type"
        ]
        responseBody.nodes*.brand == ["Keychron", "Keychron", "Gateron", null]

        and:
        responseBody.edges*.id == [701L, 704L]
        responseBody.edges*.source == [2L, 1L]
        responseBody.edges*.target == [3L, 3L]
        responseBody.edges*.comment == ["Same domain", null]
    }

    def "should return empty compatibility graph for existing domain without components"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql"
        )

        when:
        def result = get("/domains/1/compatibility/graph")

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, GraphResponse)
        responseBody.nodes.isEmpty()
        responseBody.edges.isEmpty()
    }

    def "should return not found for compatibility graph of non-existent domain"() {
        given:
        prepareCompatibilityGraphData()

        when:
        def result = get("/domains/999999/compatibility/graph")

        then:
        result.status == 404
        objectMapper.readValue(result.body, ErrorResponse).getMessage() ==
                "Domain with id 999999 not found"
    }

    def "should return bad request for non-positive compatibility graph domain id"() {
        given:
        prepareCompatibilityGraphData()

        expect:
        get("/domains/0/compatibility/graph").status == 400
    }

    def "should create normalized compatibility link and trim comment"() {
        given:
        prepareCompatibilityData()
        def request = new CreateCompatibilityLinkRequest(3L, 1L)
                .comment("  Same socket  ")

        when:
        def result = post("/domains/1/compatibility", request)

        then:
        result.status == 201
        def responseBody = objectMapper.readValue(result.body, CompatibilityLink)
        responseBody.getId() != null
        responseBody.getDomainId() == 1L
        responseBody.getComponentAId() == 1L
        responseBody.getComponentBId() == 3L
        responseBody.getComment() == "Same socket"
    }

    def "should allow compatibility between different components of same type"() {
        given:
        prepareCompatibilityData()

        when:
        def result = post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(1L, 2L)
        )

        then:
        result.status == 201
        def responseBody = objectMapper.readValue(result.body, CompatibilityLink)
        responseBody.getComponentAId() == 1L
        responseBody.getComponentBId() == 2L
    }

    def "should normalize blank compatibility comment to null"() {
        given:
        prepareCompatibilityData()

        when:
        def result = post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(1L, 3L).comment("   ")
        )

        then:
        result.status == 201
        objectMapper.readValue(result.body, CompatibilityLink).getComment() == null
    }

    def "should return conflict for duplicate undirected compatibility link"() {
        given:
        prepareCompatibilityData()
        post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(1L, 3L)
        ).status == 201

        when:
        def result = post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(3L, 1L)
        )

        then:
        result.status == 409
        objectMapper.readValue(result.body, ErrorResponse).getMessage()
                .contains("Compatibility link between components 1 and 3 already exists")
    }

    def "should create only one compatibility link for concurrent duplicate requests"() {
        given:
        prepareCompatibilityData()
        def executor = Executors.newFixedThreadPool(2)
        def start = new CountDownLatch(1)
        def futures = (1..2).collect {
            executor.submit({
                start.await()
                post(
                        "/domains/1/compatibility",
                        new CreateCompatibilityLinkRequest(1L, 3L)
                )
            } as Callable<TestResponse>)
        }

        when:
        start.countDown()
        def statuses = futures.collect { it.get().status }.sort()

        then:
        statuses == [201, 409]

        cleanup:
        executor.shutdownNow()
    }

    def "should return conflict when compatibility component is archived"() {
        given:
        prepareCompatibilityData()

        when:
        def result = post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(1L, 5L)
        )

        then:
        result.status == 409
        objectMapper.readValue(result.body, ErrorResponse).getMessage()
                .contains("archived component with id 5")
    }

    def "should return bad request for compatibility self-link"() {
        given:
        prepareCompatibilityData()

        when:
        def result = post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(1L, 1L)
        )

        then:
        result.status == 400
        objectMapper.readValue(result.body, ErrorResponse).getMessage()
                .contains("cannot be compatible with itself")
    }

    def "should return bad request when compatibility component belongs to another domain"() {
        given:
        prepareCompatibilityData()

        when:
        def result = post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(1L, 4L)
        )

        then:
        result.status == 400
        objectMapper.readValue(result.body, ErrorResponse).getMessage() ==
                "Component with id 4 does not belong to domain with id 1"
    }

    def "should return not found when compatibility domain does not exist"() {
        given:
        prepareCompatibilityData()

        when:
        def result = post(
                "/domains/999999/compatibility",
                new CreateCompatibilityLinkRequest(1L, 3L)
        )

        then:
        result.status == 404
        objectMapper.readValue(result.body, ErrorResponse).getMessage() ==
                "Domain with id 999999 not found"
    }

    def "should return not found when compatibility component does not exist"() {
        given:
        prepareCompatibilityData()

        when:
        def result = post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(1L, 999999L)
        )

        then:
        result.status == 404
        objectMapper.readValue(result.body, ErrorResponse).getMessage() ==
                "Component with id 999999 not found"
    }

    def "should return bad request for non-positive compatibility domain id"() {
        given:
        prepareCompatibilityData()

        expect:
        post(
                "/domains/0/compatibility",
                new CreateCompatibilityLinkRequest(1L, 3L)
        ).status == 400
    }

    def "should return bad request for non-positive compatibility component id"() {
        given:
        prepareCompatibilityData()

        expect:
        post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(0L, 3L)
        ).status == 400
    }

    def "should return bad request when compatibility component id is missing"() {
        given:
        prepareCompatibilityData()

        expect:
        post("/domains/1/compatibility", [componentAId: 1L]).status == 400
    }

    def "should return bad request when compatibility comment exceeds limit"() {
        given:
        prepareCompatibilityData()

        expect:
        post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(1L, 3L).comment("x".repeat(1001))
        ).status == 400
    }

    def "should physically delete compatibility link and allow pair recreation"() {
        given:
        prepareCompatibilityData()
        def linkId = createCompatibilityLink()

        when:
        def deleteResult = delete("/domains/1/compatibility/${linkId}")

        then:
        deleteResult.status == 204
        deleteResult.body.isEmpty()

        and:
        post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(1L, 3L)
        ).status == 201
    }

    def "should return not found when deleting compatibility link repeatedly"() {
        given:
        prepareCompatibilityData()
        def linkId = createCompatibilityLink()
        delete("/domains/1/compatibility/${linkId}").status == 204

        when:
        def result = delete("/domains/1/compatibility/${linkId}")

        then:
        result.status == 404
        objectMapper.readValue(result.body, ErrorResponse).getMessage() ==
                "Compatibility link with id ${linkId} not found in domain with id 1"
    }

    def "should hide compatibility link that belongs to another domain scope"() {
        given:
        prepareCompatibilityData()
        def linkId = createCompatibilityLink()

        when:
        def result = delete("/domains/2/compatibility/${linkId}")

        then:
        result.status == 404
        objectMapper.readValue(result.body, ErrorResponse).getMessage() ==
                "Compatibility link with id ${linkId} not found in domain with id 2"

        and:
        delete("/domains/1/compatibility/${linkId}").status == 204
    }

    def "should return not found when deleting compatibility link from non-existent domain"() {
        given:
        prepareCompatibilityData()

        when:
        def result = delete("/domains/999999/compatibility/1")

        then:
        result.status == 404
        objectMapper.readValue(result.body, ErrorResponse).getMessage() ==
                "Domain with id 999999 not found"
    }

    def "should return not found when deleting non-existent compatibility link"() {
        given:
        prepareCompatibilityData()

        when:
        def result = delete("/domains/1/compatibility/999999")

        then:
        result.status == 404
        objectMapper.readValue(result.body, ErrorResponse).getMessage() ==
                "Compatibility link with id 999999 not found in domain with id 1"
    }

    def "should return bad request for non-positive compatibility deletion domain id"() {
        given:
        prepareCompatibilityData()

        expect:
        delete("/domains/0/compatibility/1").status == 400
    }

    def "should return bad request for non-positive compatibility link id"() {
        given:
        prepareCompatibilityData()

        expect:
        delete("/domains/1/compatibility/0").status == 400
    }

    private Long createCompatibilityLink() {
        def result = post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(1L, 3L)
        )
        assert result.status == 201
        return objectMapper.readValue(result.body, CompatibilityLink).getId()
    }

    private void prepareCompatibilityData() {
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-second-test-component-type.sql",
                "/sql/insert-test-component.sql",
                "/sql/insert-compatibility-test-data.sql"
        )
    }

    private void prepareCompatibilityGraphData() {
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-second-test-component-type.sql",
                "/sql/insert-test-component.sql",
                "/sql/insert-compatibility-test-data.sql",
                "/sql/insert-compatibility-graph-data.sql"
        )
    }
}
