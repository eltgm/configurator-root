package ru.sultanyarov.configurator.it

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.sultanyarov.configurator.domain.dto.CreateAttributeDefinitionRequest
import ru.sultanyarov.configurator.domain.model.AttributeDefinition
import ru.sultanyarov.configurator.domain.model.DataType
import spock.lang.Shared

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AttributesControllerIntegrationSpec extends BaseIntegrationSpec {

    @Autowired
    WebApplicationContext webApplicationContext

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Sql(scripts = ["/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql"])
    def "should create attribute definition successfully"() {
        given: "a valid create attribute definition request"
        def createRequest = new CreateAttributeDefinitionRequest()
                .name("test_attribute")
                .label("Test Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(true)
                .orderIndex(1)

        when: "POST request is sent to create attribute definition"
        def result = mockMvc.perform(post("/component-types/1/attributes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        then: "attribute definition is created successfully"
        def responseBody = objectMapper.readValue(result.response.contentAsString, AttributeDefinition)
        responseBody.id() != null
        responseBody.componentTypeId() == 1L
        responseBody.name() == "test_attribute"
        responseBody.label() == "Test Attribute"
        responseBody.dataType() == DataType.STRING
        responseBody.isRequired()
        responseBody.orderIndex() == 1
    }

    @Sql(scripts = ["/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql"])
    def "should create attribute definition with enum values successfully"() {
        given: "a valid create attribute definition request with enum values"
        def createRequest = new CreateAttributeDefinitionRequest()
                .name("enum_attribute")
                .label("Enum Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.ENUM)
                .enumValues(["VALUE1", "VALUE2", "VALUE3"])
                .isRequired(true)
                .orderIndex(2)

        when: "POST request is sent to create attribute definition"
        def result = mockMvc.perform(post("/component-types/1/attributes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        then: "attribute definition is created successfully with enum values"
        def responseBody = objectMapper.readValue(result.response.contentAsString, AttributeDefinition)
        responseBody.id() != null
        responseBody.componentTypeId() == 1L
        responseBody.name() == "enum_attribute"
        responseBody.label() == "Enum Attribute"
        responseBody.dataType() == DataType.ENUM
        responseBody.isRequired()
        responseBody.orderIndex() == 2
        responseBody.enumValues() != null
        responseBody.enumValues().size() == 3
        responseBody.enumValues().containsAll(["VALUE1", "VALUE2", "VALUE3"])
    }

    @Sql(scripts = ["/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql"])
    def "should return bad request when creating attribute definition with empty name"() {
        given: "an invalid create attribute definition request with empty name"
        def createRequest = new CreateAttributeDefinitionRequest()
                .name("")
                .label("Test Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(true)
                .orderIndex(1)

        when: "POST request is sent to create attribute definition"
        mockMvc.perform(post("/component-types/1/attributes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())

        then: "attribute definition is not created"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql"])
    def "should return bad request when creating attribute definition with null name"() {
        given: "an invalid create attribute definition request with null name"
        def createRequest = new CreateAttributeDefinitionRequest()
                .label("Test Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(true)
                .orderIndex(1)

        when: "POST request is sent to create attribute definition"
        mockMvc.perform(post("/component-types/1/attributes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())

        then: "attribute definition is not created"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return not found when creating attribute definition for non-existent component type"() {
        given: "a valid create attribute definition request"
        def createRequest = new CreateAttributeDefinitionRequest()
                .name("test_attribute")
                .label("Test Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(true)
                .orderIndex(1)

        when: "POST request is sent to create attribute definition for non-existent component type"
        mockMvc.perform(post("/component-types/{id}/attributes", 999999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isNotFound())

        then: "attribute definition is not created"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql"])
    def "should return conflict when creating attribute definition with duplicate name in same component type"() {
        given: "an attribute definition is created"
        def createRequest1 = new CreateAttributeDefinitionRequest()
                .name("duplicate_attribute")
                .label("Duplicate Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(true)
                .orderIndex(1)

        mockMvc.perform(post("/component-types/1/attributes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest1)))
                .andExpect(status().isCreated())

        and: "another attribute definition with the same name is attempted to be created"
        def createRequest2 = new CreateAttributeDefinitionRequest()
                .name("duplicate_attribute")
                .label("Another Duplicate Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.NUMBER)
                .isRequired(false)
                .orderIndex(2)

        when: "POST request is sent to create attribute definition with duplicate name"
        mockMvc.perform(post("/component-types/1/attributes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest2)))
                .andExpect(status().isConflict())

        then: "attribute definition is not created"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql", "/sql/insert-test-attribute-definition.sql"])
    def "should get attribute definitions by component type id successfully"() {
        when: "GET request is sent to retrieve attribute definitions by component type id"
        def result = mockMvc.perform(get("/component-types/1/attributes"))
                .andExpect(status().isOk())
                .andReturn()

        then: "attribute definitions are retrieved successfully"
        def responseBody = objectMapper.readValue(result.response.contentAsString, AttributeDefinition[].class)
        responseBody.length == 3
        responseBody.each { attrDef ->
            attrDef.componentTypeId() == 1L
        }
        responseBody*.name().sort() == ["attribute_one", "attribute_two", "original_attribute"].sort()
    }

    @Sql(scripts = ["/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql"])
    def "should return empty array when getting attribute definitions for component type with no attributes"() {
        when: "GET request is sent to retrieve attribute definitions by component type id"
        def result = mockMvc.perform(get("/component-types/1/attributes"))
                .andExpect(status().isOk())
                .andReturn()

        then: "empty array is returned"
        def responseBody = objectMapper.readValue(result.response.contentAsString, AttributeDefinition[].class)
        responseBody.length == 0
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return not found when getting attribute definitions for non-existent component type"() {
        when: "GET request is sent for attribute definitions of non-existent component type"
        mockMvc.perform(get("/component-types/{id}/attributes", 999999L))
                .andExpect(status().isNotFound())

        then: "not found error is returned"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql", "/sql/insert-test-attribute-definition.sql"])
    def "should update attribute definition successfully"() {
        given: "an update request"
        def updateRequest = new CreateAttributeDefinitionRequest()
                .name("updated_attribute")
                .label("Updated Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.NUMBER)
                .isRequired(false)
                .orderIndex(2)

        when: "PUT request is sent to update attribute definition"
        def updateResult = mockMvc.perform(put("/attributes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andReturn()

        then: "attribute definition is updated successfully"
        def responseBody = objectMapper.readValue(updateResult.response.contentAsString, AttributeDefinition)
        responseBody.id() == 1L
        responseBody.componentTypeId() == 1L
        responseBody.name() == "updated_attribute"
        responseBody.label() == "Updated Attribute"
        responseBody.dataType() == DataType.NUMBER
        !responseBody.isRequired()
        responseBody.orderIndex() == 2
    }

    @Sql(scripts = ["/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql"])
    def "should update attribute definition with enum values successfully"() {
        given: "an attribute definition is created"
        def createRequest = new CreateAttributeDefinitionRequest()
                .name("original_enum_attribute")
                .label("Original Enum Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(true)
                .orderIndex(1)

        def createResult = mockMvc.perform(post("/component-types/1/attributes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def createdAttribute = objectMapper.readValue(createResult.response.contentAsString, AttributeDefinition)
        def attributeId = createdAttribute.id()

        and: "an update request with enum values"
        def updateRequest = new CreateAttributeDefinitionRequest()
                .name("updated_enum_attribute")
                .label("Updated Enum Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.ENUM)
                .enumValues(["OPTION_A", "OPTION_B", "OPTION_C"])
                .isRequired(false)
                .orderIndex(3)

        when: "PUT request is sent to update attribute definition"
        def updateResult = mockMvc.perform(put("/attributes/${attributeId}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andReturn()

        then: "attribute definition is updated successfully with enum values"
        def responseBody = objectMapper.readValue(updateResult.response.contentAsString, AttributeDefinition)
        responseBody.id() == attributeId
        responseBody.componentTypeId() == 1L
        responseBody.name() == "updated_enum_attribute"
        responseBody.label() == "Updated Enum Attribute"
        responseBody.dataType() == DataType.ENUM
        !responseBody.isRequired()
        responseBody.orderIndex() == 3
        responseBody.enumValues() != null
        responseBody.enumValues().size() == 3
        responseBody.enumValues().containsAll(["OPTION_A", "OPTION_B", "OPTION_C"])
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return not found when updating non-existent attribute definition"() {
        given: "an update request for non-existent attribute definition"
        def updateRequest = new CreateAttributeDefinitionRequest()
                .name("updated_attribute")
                .label("Updated Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(false)
                .orderIndex(1)

        when: "PUT request is sent to update non-existent attribute definition"
        mockMvc.perform(put("/attributes/{id}", 999999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())

        then: "not found error is returned"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql", "/sql/insert-test-attribute-definition.sql"])
    def "should return bad request when updating attribute definition with empty name"() {
        given: "an invalid update request with empty name"
        def updateRequest = new CreateAttributeDefinitionRequest()
                .name("")
                .label("Updated Attribute")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.STRING)
                .isRequired(false)
                .orderIndex(2)

        when: "PUT request is sent to update attribute definition"
        mockMvc.perform(put("/attributes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())

        then: "bad request error is returned"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql", "/sql/insert-test-domain.sql", "/sql/insert-test-component-type.sql", "/sql/insert-test-attribute-definition.sql"])
    def "should return conflict when updating attribute definition with duplicate name in same component type"() {
        given: "an update request that would create a duplicate name"
        def updateRequest = new CreateAttributeDefinitionRequest()
                .name("attribute_one") // This name already exists in the same component type
                .label("Updated Attribute Two")
                .dataType(CreateAttributeDefinitionRequest.DataTypeEnum.NUMBER)
                .isRequired(false)
                .orderIndex(3)

        when: "PUT request is sent to update attribute definition with duplicate name"
        mockMvc.perform(put("/attributes/3")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isConflict())

        then: "conflict error is returned"
        noExceptionThrown()
    }

    @Shared
    ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}
