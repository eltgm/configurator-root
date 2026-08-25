package ru.sultanyarov.configurator.contract

import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentTypeAttributeSettingsRequest
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateAttributeDefinitionRequest
import spock.lang.Specification

abstract class AbstractAttributesControllerContract extends Specification implements ApiTestSupport {

    def "should create attribute definition successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql")
        def createRequest = new CreateAttributeDefinitionRequest()
                .name("test_attribute")
                .label("Test Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(true)
                .orderIndex(1)

        when:
        def result = post("/component-types/1/attributes", createRequest)

        then:
        result.status == 201
        def responseBody = objectMapper.readValue(result.body, AttributeDefinition)
        responseBody.getId() != null
        responseBody.getDomainId() == 1L
        responseBody.getComponentTypeId() == 1L
        responseBody.getName() == "test_attribute"
        responseBody.getLabel() == "Test Attribute"
        responseBody.getDataType() == AttributeDefinition.DataTypeEnum.STRING
        responseBody.getIsRequired()
        responseBody.getOrderIndex() == 1
    }

    def "should create attribute definition with enum values successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql")
        def createRequest = new CreateAttributeDefinitionRequest()
                .name("enum_attribute")
                .label("Enum Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.ENUM)
                .enumValues(["VALUE1", "VALUE2", "VALUE3"])
                .isRequired(true)
                .orderIndex(2)

        when:
        def result = post("/component-types/1/attributes", createRequest)

        then:
        result.status == 201
        def responseBody = objectMapper.readValue(result.body, AttributeDefinition)
        responseBody.getId() != null
        responseBody.getComponentTypeId() == 1L
        responseBody.getName() == "enum_attribute"
        responseBody.getLabel() == "Enum Attribute"
        responseBody.getDataType() == AttributeDefinition.DataTypeEnum.ENUM
        responseBody.getIsRequired()
        responseBody.getOrderIndex() == 2
        responseBody.getEnumValues() != null
        responseBody.getEnumValues().size() == 3
        responseBody.getEnumValues().containsAll(["VALUE1", "VALUE2", "VALUE3"])
    }

    def "should return bad request when creating attribute definition with empty name"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql")
        def createRequest = new CreateAttributeDefinitionRequest()
                .name("")
                .label("Test Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(true)
                .orderIndex(1)

        expect:
        post("/component-types/1/attributes", createRequest).status == 400
    }

    def "should return bad request when creating attribute definition with null name"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql")
        def createRequest = new CreateAttributeDefinitionRequest()
                .label("Test Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(true)
                .orderIndex(1)

        expect:
        post("/component-types/1/attributes", createRequest).status == 400
    }

    def "should return not found when creating attribute definition for non-existent component type"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def createRequest = new CreateAttributeDefinitionRequest()
                .name("test_attribute")
                .label("Test Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(true)
                .orderIndex(1)

        expect:
        post("/component-types/999999/attributes", createRequest).status == 404
    }

    def "should return conflict when creating attribute definition with duplicate name in same component type"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql")
        post("/component-types/1/attributes", new CreateAttributeDefinitionRequest()
                .name("duplicate_attribute")
                .label("Duplicate Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(true)
                .orderIndex(1))
        def duplicateRequest = new CreateAttributeDefinitionRequest()
                .name("duplicate_attribute")
                .label("Another Duplicate Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.NUMBER)
                .isRequired(false)
                .orderIndex(2)

        expect:
        post("/component-types/1/attributes", duplicateRequest).status == 409
    }

    def "should get attribute definitions by component type id successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql", "/sql/insert-test-attribute-definition.sql")

        when:
        def result = get("/component-types/1/attributes")

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, AttributeDefinition[].class)
        responseBody.length == 3
        responseBody.each { attrDef ->
            attrDef.getComponentTypeId() == 1L
        }
        responseBody*.getName().sort() == ["attribute_one", "attribute_two", "original_attribute"].sort()
    }

    def "should return empty array when getting attribute definitions for component type with no attributes"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql")

        when:
        def result = get("/component-types/1/attributes")

        then:
        result.status == 200
        def responseBody = objectMapper.readValue(result.body, AttributeDefinition[].class)
        responseBody.length == 0
    }

    def "should return not found when getting attribute definitions for non-existent component type"() {
        given:
        runSqlScripts("/sql/clear-db.sql")

        expect:
        get("/component-types/999999/attributes").status == 404
    }

    def "should update attribute definition successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql", "/sql/insert-test-attribute-definition.sql")
        def updateRequest = new CreateAttributeDefinitionRequest()
                .name("updated_attribute")
                .label("Updated Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.NUMBER)
                .isRequired(false)
                .orderIndex(2)

        when:
        def updateResult = put("/attributes/1", updateRequest)

        then:
        updateResult.status == 200
        def responseBody = objectMapper.readValue(updateResult.body, AttributeDefinition)
        responseBody.getId() == 1L
        responseBody.getDomainId() == 1L
        responseBody.getComponentTypeId() == null
        responseBody.getComponentTypeIds() == [1L]
        responseBody.getName() == "updated_attribute"
        responseBody.getLabel() == "Updated Attribute"
        responseBody.getDataType() == AttributeDefinition.DataTypeEnum.NUMBER
        responseBody.getIsRequired() == null
        responseBody.getOrderIndex() == null

        and: "type-specific settings remain unchanged by a shared definition update"
        def linkedDefinition = objectMapper.readValue(get("/component-types/1/attributes").body, AttributeDefinition[].class)[0]
        linkedDefinition.getIsRequired()
        linkedDefinition.getOrderIndex() == 1
    }

    def "should update attribute definition while keeping its current name"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql", "/sql/insert-test-attribute-definition.sql")
        def updateRequest = new CreateAttributeDefinitionRequest()
                .name("original_attribute")
                .label("Relabeled Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(false)
                .orderIndex(7)

        when:
        def updateResult = put("/attributes/1", updateRequest)

        then:
        updateResult.status == 200
        def responseBody = objectMapper.readValue(updateResult.body, AttributeDefinition)
        responseBody.getId() == 1L
        responseBody.getName() == "original_attribute"
        responseBody.getLabel() == "Relabeled Attribute"
        responseBody.getIsRequired() == null
        responseBody.getOrderIndex() == null
        responseBody.getComponentTypeIds() == [1L]
    }

    def "should reject attribute data type change when persisted values exist"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-attribute-definition.sql",
                "/sql/insert-test-component.sql",
                "/sql/insert-attribute-value-for-definition.sql"
        )
        def updateRequest = new CreateAttributeDefinitionRequest()
                .name("original_attribute")
                .label("Original Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.NUMBER)
                .isRequired(true)
                .orderIndex(1)

        expect:
        put("/attributes/1", updateRequest).status == 400
    }

    def "should update attribute definition with enum values successfully"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql")
        def createdAttribute = objectMapper.readValue(post("/component-types/1/attributes", new CreateAttributeDefinitionRequest()
                .name("original_enum_attribute")
                .label("Original Enum Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(true)
                .orderIndex(1)).body, AttributeDefinition)

        def updateRequest = new CreateAttributeDefinitionRequest()
                .name("updated_enum_attribute")
                .label("Updated Enum Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.ENUM)
                .enumValues(["OPTION_A", "OPTION_B", "OPTION_C"])
                .isRequired(false)
                .orderIndex(3)

        when:
        def updateResult = put("/attributes/${createdAttribute.getId()}", updateRequest)

        then:
        updateResult.status == 200
        def responseBody = objectMapper.readValue(updateResult.body, AttributeDefinition)
        responseBody.getId() == createdAttribute.getId()
        responseBody.getComponentTypeId() == null
        responseBody.getComponentTypeIds() == [1L]
        responseBody.getName() == "updated_enum_attribute"
        responseBody.getLabel() == "Updated Enum Attribute"
        responseBody.getDataType() == AttributeDefinition.DataTypeEnum.ENUM
        responseBody.getIsRequired() == null
        responseBody.getOrderIndex() == null
        responseBody.getEnumValues() != null
        responseBody.getEnumValues().size() == 3
        responseBody.getEnumValues().containsAll(["OPTION_A", "OPTION_B", "OPTION_C"])
    }

    def "should return not found when updating non-existent attribute definition"() {
        given:
        runSqlScripts("/sql/clear-db.sql")
        def updateRequest = new CreateAttributeDefinitionRequest()
                .name("updated_attribute")
                .label("Updated Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(false)
                .orderIndex(1)

        expect:
        put("/attributes/999999", updateRequest).status == 404
    }

    def "should return bad request when updating attribute definition with empty name"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql", "/sql/insert-test-attribute-definition.sql")
        def updateRequest = new CreateAttributeDefinitionRequest()
                .name("")
                .label("Updated Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(false)
                .orderIndex(2)

        expect:
        put("/attributes/1", updateRequest).status == 400
    }

    def "should return conflict when updating attribute definition with duplicate name in same component type"() {
        given:
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql", "/sql/insert-test-attribute-definition.sql")
        def updateRequest = new CreateAttributeDefinitionRequest()
                .name("attribute_one")
                .label("Updated Attribute Two")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.NUMBER)
                .isRequired(false)
                .orderIndex(3)

        expect:
        put("/attributes/3", updateRequest).status == 409
    }

    def "should manage a reusable domain attribute catalog and independent type settings"() {
        given:
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-second-test-component-type.sql"
        )
        def createRequest = new CreateAttributeDefinitionRequest()
                .name("shared_voltage")
                .label("Voltage")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.NUMBER)

        when: "a definition is created in the domain catalog"
        def createResult = post("/domains/1/attributes", createRequest)
        def created = objectMapper.readValue(createResult.body, AttributeDefinition)

        then:
        createResult.status == 201
        created.getDomainId() == 1L
        created.getComponentTypeId() == null
        created.getComponentTypeIds().isEmpty()

        when: "the same definition is attached to two component types"
        def firstAttach = put(
                "/component-types/1/attributes/${created.getId()}",
                new ComponentTypeAttributeSettingsRequest().isRequired(true).orderIndex(4)
        )
        def secondAttach = put(
                "/component-types/2/attributes/${created.getId()}",
                new ComponentTypeAttributeSettingsRequest().isRequired(false).orderIndex(9)
        )

        then:
        firstAttach.status == 200
        secondAttach.status == 200
        def firstTypeAttribute = objectMapper.readValue(get("/component-types/1/attributes").body, AttributeDefinition[].class)[0]
        def secondTypeAttribute = objectMapper.readValue(get("/component-types/2/attributes").body, AttributeDefinition[].class)[0]
        firstTypeAttribute.getId() == created.getId()
        firstTypeAttribute.getIsRequired()
        firstTypeAttribute.getOrderIndex() == 4
        !secondTypeAttribute.getIsRequired()
        secondTypeAttribute.getOrderIndex() == 9

        and: "the catalog reports both usages"
        def catalog = objectMapper.readValue(get("/domains/1/attributes").body, AttributeDefinition[].class)
        catalog.length == 1
        catalog[0].getComponentTypeIds().sort() == [1L, 2L]

        when: "the definition is detached from one component type"
        def detachResult = delete("/component-types/2/attributes/${created.getId()}")

        then:
        detachResult.status == 204
        objectMapper.readValue(get("/component-types/2/attributes").body, AttributeDefinition[].class).length == 0
        objectMapper.readValue(get("/domains/1/attributes").body, AttributeDefinition[].class)[0]
                .getComponentTypeIds() == [1L]
    }

    def "should cascade catalog attribute deletion and reject deletion used by compatibility rules"() {
        given: "an unused catalog attribute linked to a component type"
        runSqlScripts(
                "/sql/clear-db.sql",
                "/sql/insert-test-domain.sql",
                "/sql/insert-test-component-type.sql",
                "/sql/insert-test-attribute-definition.sql"
        )

        when:
        def deleteResult = delete("/attributes/1")

        then:
        deleteResult.status == 204
        objectMapper.readValue(get("/domains/1/attributes").body, AttributeDefinition[].class)*.id == [2L, 3L]
        objectMapper.readValue(get("/component-types/1/attributes").body, AttributeDefinition[].class)*.id == [2L, 3L]

        when: "the attribute participates in an automatic compatibility rule"
        runSqlScripts("/sql/clear-db.sql", "/sql/insert-configurator-test-data.sql")

        then:
        delete("/attributes/101").status == 409
    }
}
