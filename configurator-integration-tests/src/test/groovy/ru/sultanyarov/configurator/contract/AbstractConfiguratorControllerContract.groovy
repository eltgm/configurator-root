package ru.sultanyarov.configurator.contract

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
