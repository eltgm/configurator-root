package ru.sultanyarov.configurator.contract

import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityLink
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleConditionInput
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleOperator
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleSet
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateCompatibilityLinkRequest
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ErrorResponse
import ru.sultanyarov.configurator.api.inbounds.rest.dto.GraphResponse
import ru.sultanyarov.configurator.api.inbounds.rest.dto.SaveCompatibilityRuleSetRequest
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

    def "should create normalized attribute compatibility rule set"() {
        given:
        prepareCompatibilityRuleData()
        def request = ruleRequest(
                "  Frequency rule  ",
                20L,
                10L,
                new CompatibilityRuleConditionInput(
                        202L,
                        CompatibilityRuleOperator.GT,
                        102L
                )
        )

        when:
        def result = post("/domains/1/compatibility/rules", request)

        then:
        result.status == 201
        def responseBody = objectMapper.readValue(result.body, CompatibilityRuleSet)
        responseBody.id != null
        responseBody.domainId == 1L
        responseBody.name == "Frequency rule"
        responseBody.componentTypeAId == 10L
        responseBody.componentTypeBId == 20L
        responseBody.enabled
        responseBody.createdAt != null
        responseBody.conditions.size() == 1
        with(responseBody.conditions.first()) {
            id != null
            ruleSetId == responseBody.id
            leftAttributeDefinitionId == 102L
            operator == CompatibilityRuleOperator.LT
            rightAttributeDefinitionId == 202L
            orderIndex == 0
            createdAt != null
        }
    }

    def "should return compatibility rule sets in stable order and empty list for empty domain"() {
        given:
        prepareCompatibilityRuleData()

        expect:
        get("/domains/1/compatibility/rules").body == "[]"

        when:
        def first = createCompatibilityRule("First")
        def second = createCompatibilityRule("Second")
        def result = get("/domains/1/compatibility/rules")

        then:
        result.status == 200
        def responseBody = objectMapper.readerForListOf(CompatibilityRuleSet).readValue(result.body)
        responseBody*.id == [first.id, second.id]
        responseBody*.name == ["First", "Second"]
    }

    def "should get compatibility rule set only in its domain scope"() {
        given:
        prepareCompatibilityRuleData()
        def created = createCompatibilityRule("Scoped")

        expect:
        objectMapper.readValue(
                get("/domains/1/compatibility/rules/${created.id}").body,
                CompatibilityRuleSet
        ) == created

        when:
        def foreignScope = get("/domains/2/compatibility/rules/${created.id}")

        then:
        foreignScope.status == 404
        objectMapper.readValue(foreignScope.body, ErrorResponse).message ==
                "Compatibility rule set with id ${created.id} not found in domain with id 2"
    }

    def "should fully replace compatibility rule set and preserve aggregate identity"() {
        given:
        prepareCompatibilityRuleData()
        def created = createCompatibilityRule("Original")
        def replacement = new SaveCompatibilityRuleSetRequest(
                "Updated",
                10L,
                20L,
                false,
                [
                        new CompatibilityRuleConditionInput(
                                102L,
                                CompatibilityRuleOperator.GTE,
                                202L
                        ).orderIndex(8)
                ]
        )

        when:
        def result = put("/domains/1/compatibility/rules/${created.id}", replacement)

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, CompatibilityRuleSet)
        responseBody.id == created.id
        responseBody.createdAt == created.createdAt
        responseBody.name == "Updated"
        !responseBody.enabled
        responseBody.conditions.size() == 1
        responseBody.conditions.first().id != created.conditions.first().id
        responseBody.conditions.first().operator == CompatibilityRuleOperator.GTE
        responseBody.conditions.first().orderIndex == 8

        and:
        objectMapper.readValue(
                get("/domains/1/compatibility/rules/${created.id}").body,
                CompatibilityRuleSet
        ) == responseBody
    }

    def "should physically delete compatibility rule set with its conditions"() {
        given:
        prepareCompatibilityRuleData()
        def created = createCompatibilityRule("Delete")

        when:
        def deleteResult = delete("/domains/1/compatibility/rules/${created.id}")

        then:
        deleteResult.status == 204
        deleteResult.body.isEmpty()
        get("/domains/1/compatibility/rules/${created.id}").status == 404
        delete("/domains/1/compatibility/rules/${created.id}").status == 404
    }

    def "should return conflict for duplicate compatibility rule business key"() {
        given:
        prepareCompatibilityRuleData()
        createCompatibilityRule("Duplicate")

        when:
        def result = post(
                "/domains/1/compatibility/rules",
                ruleRequest("Duplicate", 10L, 20L, stringEqualityCondition())
        )

        then:
        result.status == 409
        objectMapper.readValue(result.body, ErrorResponse).message ==
                "Compatibility rule set 'Duplicate' already exists for component types 10 and 20 in domain 1"
    }

    def "should create only one compatibility rule for concurrent duplicate requests"() {
        given:
        prepareCompatibilityRuleData()
        def ready = new CountDownLatch(2)
        def start = new CountDownLatch(1)
        def executor = Executors.newFixedThreadPool(2)
        def request = ruleRequest("Concurrent", 10L, 20L, stringEqualityCondition())
        def futures = (1..2).collect {
            executor.submit({
                ready.countDown()
                start.await()
                post("/domains/1/compatibility/rules", request)
            } as Callable<TestResponse>)
        }

        when:
        ready.await()
        start.countDown()
        def statuses = futures.collect { it.get().status }.sort()

        then:
        statuses == [201, 409]
        def rules = objectMapper.readerForListOf(CompatibilityRuleSet)
                .readValue(get("/domains/1/compatibility/rules").body)
        rules*.name == ["Concurrent"]

        cleanup:
        executor.shutdownNow()
    }

    def "should return conflict when replacement duplicates another compatibility rule"() {
        given:
        prepareCompatibilityRuleData()
        createCompatibilityRule("First")
        def second = createCompatibilityRule("Second")

        when:
        def result = put(
                "/domains/1/compatibility/rules/${second.id}",
                ruleRequest("First", 20L, 10L, new CompatibilityRuleConditionInput(
                        101L,
                        CompatibilityRuleOperator.EQUALS,
                        101L
                ))
        )

        then:
        result.status == 409
    }

    def "should reject invalid compatibility rule semantics"() {
        given:
        prepareCompatibilityRuleData()

        expect:
        post("/domains/1/compatibility/rules", request).status == expectedStatus

        where:
        request                                                                                    | expectedStatus
        ruleRequest("Same type", 10L, 10L, stringEqualityCondition())                              | 400
        ruleRequest("Foreign type", 10L, 30L, stringEqualityCondition())                           | 400
        ruleRequest("Missing type", 10L, 999999L, stringEqualityCondition())                        | 404
        ruleRequest("Wrong side", 10L, 20L, new CompatibilityRuleConditionInput(
                202L, CompatibilityRuleOperator.EQUALS, 102L))                                     | 400
        ruleRequest("Mismatched types", 10L, 20L, new CompatibilityRuleConditionInput(
                101L, CompatibilityRuleOperator.EQUALS, 202L))                                     | 400
        ruleRequest("Invalid operator", 10L, 20L, new CompatibilityRuleConditionInput(
                101L, CompatibilityRuleOperator.GT, 101L))                                         | 400
        new SaveCompatibilityRuleSetRequest(
                "Duplicate conditions",
                10L,
                20L,
                true,
                [stringEqualityCondition(), stringEqualityCondition().orderIndex(1)]
        )                                                                                          | 400
    }

    def "should enforce compatibility rule transport validation"() {
        given:
        prepareCompatibilityRuleData()

        expect:
        post("/domains/1/compatibility/rules", body).status == 400

        where:
        body << [
                [
                        componentTypeAId: 10L,
                        componentTypeBId: 20L,
                        enabled         : true,
                        conditions      : [[
                                leftAttributeDefinitionId : 101L,
                                operator                  : "EQUALS",
                                rightAttributeDefinitionId: 101L
                        ]]
                ],
                [
                        name            : "   ",
                        componentTypeAId: 10L,
                        componentTypeBId: 20L,
                        enabled         : true,
                        conditions      : [[
                                leftAttributeDefinitionId : 101L,
                                operator                  : "EQUALS",
                                rightAttributeDefinitionId: 101L
                        ]]
                ],
                [
                        name            : "Rule",
                        componentTypeAId: 0L,
                        componentTypeBId: 20L,
                        enabled         : true,
                        conditions      : [[
                                leftAttributeDefinitionId : 101L,
                                operator                  : "EQUALS",
                                rightAttributeDefinitionId: 101L
                        ]]
                ],
                [
                        name            : "Rule",
                        componentTypeAId: 10L,
                        componentTypeBId: 20L,
                        enabled         : true,
                        conditions      : []
                ],
                [
                        name            : "Rule",
                        componentTypeAId: 10L,
                        componentTypeBId: 20L,
                        enabled         : true,
                        conditions      : [[
                                leftAttributeDefinitionId : 101L,
                                operator                  : "EQUALS",
                                rightAttributeDefinitionId: 101L,
                                orderIndex                : -1
                        ]]
                ]
        ]
    }

    def "should return not found for compatibility rule operations in missing domains"() {
        given:
        prepareCompatibilityRuleData()

        expect:
        get("/domains/999999/compatibility/rules").status == 404
        post(
                "/domains/999999/compatibility/rules",
                ruleRequest("Rule", 10L, 20L, stringEqualityCondition())
        ).status == 404
        get("/domains/999999/compatibility/rules/1").status == 404
        delete("/domains/999999/compatibility/rules/1").status == 404
    }

    def "should reject non-positive compatibility rule path identifiers"() {
        given:
        prepareCompatibilityRuleData()

        expect:
        get("/domains/0/compatibility/rules").status == 400
        get("/domains/1/compatibility/rules/0").status == 400
        put(
                "/domains/1/compatibility/rules/0",
                ruleRequest("Rule", 10L, 20L, stringEqualityCondition())
        ).status == 400
        delete("/domains/1/compatibility/rules/0").status == 400
    }

    private Long createCompatibilityLink() {
        def result = post(
                "/domains/1/compatibility",
                new CreateCompatibilityLinkRequest(1L, 3L)
        )
        assert result.status == 201
        return objectMapper.readValue(result.body, CompatibilityLink).getId()
    }

    private CompatibilityRuleSet createCompatibilityRule(String name) {
        def result = post(
                "/domains/1/compatibility/rules",
                ruleRequest(name, 10L, 20L, stringEqualityCondition())
        )
        assert result.status == 201
        return objectMapper.readValue(result.body, CompatibilityRuleSet)
    }

    private static SaveCompatibilityRuleSetRequest ruleRequest(
            String name,
            Long componentTypeAId,
            Long componentTypeBId,
            CompatibilityRuleConditionInput condition
    ) {
        return new SaveCompatibilityRuleSetRequest(
                name,
                componentTypeAId,
                componentTypeBId,
                true,
                [condition]
        )
    }

    private static CompatibilityRuleConditionInput stringEqualityCondition() {
        return new CompatibilityRuleConditionInput(
                101L,
                CompatibilityRuleOperator.EQUALS,
                101L
        )
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

    private void prepareCompatibilityRuleData() {
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-compatibility-rule-test-data.sql"
        )
    }
}
