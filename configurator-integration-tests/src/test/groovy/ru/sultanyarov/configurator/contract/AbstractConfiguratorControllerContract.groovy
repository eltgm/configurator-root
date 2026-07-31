package ru.sultanyarov.configurator.contract

import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorBatchSearchRequest
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorBatchSearchResponse
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorIntersectionRequest
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorIntersectionResponse
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorResponse
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ErrorResponse
import spock.lang.Specification

abstract class AbstractConfiguratorControllerContract extends Specification implements ApiTestSupport {

    def "should return union of direct manual and automatic compatibility grouped in type order"() {
        given:
        prepareConfiguratorData()

        when:
        def result = get("/domains/1/configurator/compatible", [componentId: 1L])

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ConfiguratorResponse)
        responseBody.baseComponentId == 1L
        responseBody.compatibleByType*.componentTypeId == [20L, 30L]
        responseBody.compatibleByType*.componentTypeName == ["Motherboard", "Cooler"]
        responseBody.compatibleByType[0].components*.id == [2L, 3L]
        responseBody.compatibleByType[0].components*.name == [
                "Automatic and manual board",
                "Manual board"
        ]
        responseBody.compatibleByType[0].components*.brand == ["Board Brand", null]
        responseBody.compatibleByType[1].components*.id == [5L]

        and: "every matching manual and automatic reason is explained"
        def both = responseBody.compatibleByType[0].components[0]
        both.explanations*.source*.toString() == ["MANUAL", "AUTOMATIC"]
        with(both.explanations[0]) {
            linkId == 801L
            comment == "Duplicate automatic source"
            ruleSetId == null
            conditions == null
        }
        with(both.explanations[1]) {
            linkId == null
            ruleSetId == 701L
            ruleSetName == "Socket and power"
            conditions*.leftAttributeDefinitionId == [101L, 102L]
            conditions*.leftAttributeName == ["socket", "power"]
            conditions*.leftValue == ["AM5", "100"]
            conditions*.operator*.toString() == ["EQUALS", "LTE"]
            conditions*.rightAttributeDefinitionId == [201L, 202L]
            conditions*.rightAttributeName == ["socket", "power_limit"]
            conditions*.rightValue == ["AM5", "200"]
        }

        and: "manual-only components contain their link details"
        def manualBoard = responseBody.compatibleByType[0].components[1]
        manualBoard.explanations*.source*.toString() == ["MANUAL"]
        manualBoard.explanations[0].linkId == 802L
        manualBoard.explanations[0].comment == "Manual mismatch override"
        def manualCooler = responseBody.compatibleByType[1].components[0]
        manualCooler.explanations*.source*.toString() == ["MANUAL"]
        manualCooler.explanations[0].linkId == 803L
        manualCooler.explanations[0].comment == "Manual cross-type compatibility"

        and: "automatic/manual duplicate, archived candidate and disabled-only match are absent"
        responseBody.compatibleByType*.components.flatten()*.id == [2L, 3L, 5L]
    }

    def "should return empty compatibility groups for active component without links or rules"() {
        given:
        prepareConfiguratorData()

        when:
        def result = get("/domains/1/configurator/compatible", [componentId: 8L])

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ConfiguratorResponse)
        responseBody.baseComponentId == 8L
        responseBody.compatibleByType == []
    }

    def "should include transitively compatible components with shortest path when requested"() {
        given:
        prepareConfiguratorData()

        when:
        def result = get(
                "/domains/1/configurator/compatible",
                [componentId: 1L, includeTransitive: true]
        )

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ConfiguratorResponse)
        responseBody.compatibleByType*.componentTypeId == [20L, 30L]
        responseBody.compatibleByType[0].components*.id == [2L, 3L]
        responseBody.compatibleByType[1].components*.id == [5L, 9L]

        and: "direct components keep their direct explanations"
        responseBody.compatibleByType[1].components[0].explanations*.source*.toString() == ["MANUAL"]
        responseBody.compatibleByType[1].components[0].explanations[0].linkId == 803L

        and: "transitive-only component contains the deterministic shortest path"
        def transitive = responseBody.compatibleByType[1].components[1]
        transitive.name == "Transitive cooler"
        transitive.explanations*.source*.toString() == ["TRANSITIVE"]
        transitive.explanations[0].pathComponentIds == [1L, 3L, 9L]
        transitive.explanations[0].linkId == null
        transitive.explanations[0].ruleSetId == null
        transitive.explanations[0].conditions == null

        and: "the base and disconnected active component are not returned"
        responseBody.compatibleByType*.components.flatten()*.id == [2L, 3L, 5L, 9L]
    }

    def "should search direct compatibility independently for multiple components in request order"() {
        given:
        prepareConfiguratorData()
        def request = new ConfiguratorBatchSearchRequest([3L, 1L])

        when:
        def result = post("/domains/1/configurator/compatible/search", request)

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ConfiguratorBatchSearchResponse)
        responseBody.results*.baseComponentId == [3L, 1L]

        and: "each selected component has its own direct compatibility set"
        responseBody.results[0].compatibleByType*.componentTypeId == [10L, 30L]
        responseBody.results[0].compatibleByType[0].components*.id == [1L]
        responseBody.results[0].compatibleByType[1].components*.id == [9L]
        responseBody.results[1].compatibleByType*.componentTypeId == [20L, 30L]
        responseBody.results[1].compatibleByType[0].components*.id == [2L, 3L]
        responseBody.results[1].compatibleByType[1].components*.id == [5L]

        and: "selected components may appear in each other's independent results"
        responseBody.results[0].compatibleByType*.components.flatten()*.id.contains(1L)
        responseBody.results[1].compatibleByType*.components.flatten()*.id.contains(3L)
    }

    def "should apply transitive mode to every component in batch search"() {
        given:
        prepareConfiguratorData()
        def request = new ConfiguratorBatchSearchRequest([1L, 8L])
                .includeTransitive(true)

        when:
        def result = post("/domains/1/configurator/compatible/search", request)

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ConfiguratorBatchSearchResponse)
        responseBody.results*.baseComponentId == [1L, 8L]
        responseBody.results[0].compatibleByType*.components.flatten()*.id == [2L, 3L, 5L, 9L]
        responseBody.results[0].compatibleByType[1].components[1]
                .explanations[0].pathComponentIds == [1L, 3L, 9L]
        responseBody.results[1].compatibleByType == []
    }

    def "should validate batch configurator request collection"() {
        given:
        prepareConfiguratorData()

        expect:
        post("/domains/1/configurator/compatible/search", [componentIds: []]).status == 400
        post(
                "/domains/1/configurator/compatible/search",
                [componentIds: (1L..51L).toList()]
        ).status == 400
        post(
                "/domains/1/configurator/compatible/search",
                [componentIds: [1L, 1L]]
        ).status == 400
        post(
                "/domains/1/configurator/compatible/search",
                [componentIds: [0L]]
        ).status == 400
    }

    def "should reject whole batch for missing archived or foreign base component"() {
        given:
        prepareConfiguratorData()

        expect:
        post(
                "/domains/1/configurator/compatible/search",
                [componentIds: [1L, 999999L]]
        ).status == 404
        post(
                "/domains/1/configurator/compatible/search",
                [componentIds: [1L, 4L]]
        ).status == 400
        post(
                "/domains/1/configurator/compatible/search",
                [componentIds: [1L, 7L]]
        ).status == 400
        post(
                "/domains/999999/configurator/compatible/search",
                [componentIds: [1L]]
        ).status == 404
    }

    def "should intersect direct compatibility and preserve evidence for every base"() {
        given:
        prepareConfiguratorData()
        def request = new ConfiguratorIntersectionRequest([2L, 3L])

        when:
        def result = post("/domains/1/configurator/compatible/intersection", request)

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ConfiguratorIntersectionResponse)
        responseBody.componentIds == [2L, 3L]
        responseBody.compatibleByType*.componentTypeId == [10L]
        responseBody.compatibleByType[0].components*.id == [1L]

        and: "compatibility evidence follows the requested base component order"
        def common = responseBody.compatibleByType[0].components[0]
        common.compatibilityByBase*.baseComponentId == [2L, 3L]
        common.compatibilityByBase[0].explanations*.source*.toString() == [
                "MANUAL",
                "AUTOMATIC"
        ]
        common.compatibilityByBase[0].explanations[0].linkId == 801L
        common.compatibilityByBase[0].explanations[1].ruleSetId == 701L
        common.compatibilityByBase[1].explanations*.linkId == [802L]

        and: "selected base components are excluded from the intersection"
        !responseBody.compatibleByType*.components.flatten()*.id.any { it in [2L, 3L] }
    }

    def "should intersect transitive compatibility with paths from every base"() {
        given:
        prepareConfiguratorData()
        def request = new ConfiguratorIntersectionRequest([2L, 9L])
                .includeTransitive(true)

        when:
        def result = post("/domains/1/configurator/compatible/intersection", request)

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ConfiguratorIntersectionResponse)
        responseBody.compatibleByType*.componentTypeId == [10L, 20L, 30L]
        responseBody.compatibleByType*.components.flatten()*.id == [1L, 3L, 5L]

        and: "a candidate can be direct for one base and transitive for another"
        def processor = responseBody.compatibleByType[0].components[0]
        processor.compatibilityByBase*.baseComponentId == [2L, 9L]
        processor.compatibilityByBase[0].explanations*.source*.toString() == [
                "MANUAL",
                "AUTOMATIC"
        ]
        processor.compatibilityByBase[1].explanations*.source*.toString() == ["TRANSITIVE"]
        processor.compatibilityByBase[1].explanations[0].pathComponentIds == [9L, 3L, 1L]
    }

    def "should return an empty successful intersection when no candidate matches every base"() {
        given:
        prepareConfiguratorData()

        when:
        def result = post(
                "/domains/1/configurator/compatible/intersection",
                new ConfiguratorIntersectionRequest([8L, 2L])
        )

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, ConfiguratorIntersectionResponse)
        responseBody.componentIds == [8L, 2L]
        responseBody.compatibleByType == []
    }

    def "should validate intersection component collection and reject the whole request"() {
        given:
        prepareConfiguratorData()

        expect:
        post(
                "/domains/1/configurator/compatible/intersection",
                [componentIds: [1L]]
        ).status == 400
        post(
                "/domains/1/configurator/compatible/intersection",
                [componentIds: [1L, 1L]]
        ).status == 400
        post(
                "/domains/1/configurator/compatible/intersection",
                [componentIds: (1L..51L).toList()]
        ).status == 400
        post(
                "/domains/1/configurator/compatible/intersection",
                [componentIds: [1L, 999999L]]
        ).status == 404
        post(
                "/domains/1/configurator/compatible/intersection",
                [componentIds: [1L, 4L]]
        ).status == 400
        post(
                "/domains/1/configurator/compatible/intersection",
                [componentIds: [1L, 7L]]
        ).status == 400
    }

    def "should return not found for missing configurator domain"() {
        given:
        prepareConfiguratorData()

        when:
        def result = get("/domains/999999/configurator/compatible", [componentId: 1L])

        then:
        result.status == 404
        objectMapper.readValue(result.body, ErrorResponse).message ==
                "Domain with id 999999 not found"
    }

    def "should return not found for missing configurator base component"() {
        given:
        prepareConfiguratorData()

        when:
        def result = get("/domains/1/configurator/compatible", [componentId: 999999L])

        then:
        result.status == 404
        objectMapper.readValue(result.body, ErrorResponse).message ==
                "Component with id 999999 not found"
    }

    def "should reject configurator base component from another domain"() {
        given:
        prepareConfiguratorData()

        when:
        def result = get("/domains/1/configurator/compatible", [componentId: 7L])

        then:
        result.status == 400
        objectMapper.readValue(result.body, ErrorResponse).message ==
                "Component with id 7 does not belong to domain with id 1"
    }

    def "should reject archived configurator base component"() {
        given:
        prepareConfiguratorData()

        when:
        def result = get("/domains/1/configurator/compatible", [componentId: 4L])

        then:
        result.status == 400
        objectMapper.readValue(result.body, ErrorResponse).message ==
                "Archived component with id 4 cannot be used as configurator base"
    }

    def "should validate configurator identifiers and required query parameter"() {
        given:
        prepareConfiguratorData()

        expect:
        get("/domains/0/configurator/compatible", [componentId: 1L]).status == 400
        get("/domains/1/configurator/compatible", [componentId: 0L]).status == 400
        get("/domains/1/configurator/compatible").status == 400
    }

    private void prepareConfiguratorData() {
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-configurator-test-data.sql"
        )
    }
}
