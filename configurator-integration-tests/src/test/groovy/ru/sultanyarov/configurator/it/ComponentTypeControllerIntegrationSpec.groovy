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
import ru.sultanyarov.configurator.domain.dto.ComponentType
import ru.sultanyarov.configurator.domain.dto.CreateComponentTypeRequest
import ru.sultanyarov.configurator.domain.dto.CreateDomainRequest
import ru.sultanyarov.configurator.domain.dto.Domain
import spock.lang.Shared

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ComponentTypeControllerIntegrationSpec extends BaseIntegrationSpec {

    @Autowired
    WebApplicationContext webApplicationContext

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should create component type successfully"() {
        given: "a domain is created"
        def createDomainRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")

        def domainResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDomainRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def domain = objectMapper.readValue(domainResult.response.contentAsString, Domain)
        def domainId = domain.getId()

        and: "a valid create component type request"
        def createRequest = new CreateComponentTypeRequest()
                .name("Test Component Type")
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)

        when: "POST request is sent to create component type"
        def result = mockMvc.perform(post("/domains/{id}/component-types", domainId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        then: "component type is created successfully"
        def responseBody = objectMapper.readValue(result.response.contentAsString, ComponentType)
        responseBody.getId() != null
        responseBody.getDomainId() == domainId
        responseBody.getName() == "Test Component Type"
        responseBody.getCode() == "TEST_CODE"
        responseBody.getDescription() == "Test Description"
        responseBody.getOrderIndex() == 1
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return bad request when creating component type with empty name"() {
        given: "a domain is created"
        def createDomainRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")

        def domainResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDomainRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def domain = objectMapper.readValue(domainResult.response.contentAsString, Domain)
        def domainId = domain.getId()

        and: "an invalid create component type request with empty name"
        def createRequest = new CreateComponentTypeRequest()
                .name("")
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)

        when: "POST request is sent to create component type"
        mockMvc.perform(post("/domains/{id}/component-types", domainId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())

        then: "component type is not created"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return bad request when creating component type with null name"() {
        given: "a domain is created"
        def createDomainRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")

        def domainResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDomainRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def domain = objectMapper.readValue(domainResult.response.contentAsString, Domain)
        def domainId = domain.getId()

        and: "an invalid create component type request with null name"
        def createRequest = new CreateComponentTypeRequest()
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)

        when: "POST request is sent to create component type"
        mockMvc.perform(post("/domains/{id}/component-types", domainId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())

        then: "component type is not created"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return not found when creating component type for non-existent domain"() {
        given: "a valid create component type request"
        def createRequest = new CreateComponentTypeRequest()
                .name("Test Component Type")
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)

        when: "POST request is sent to create component type for non-existent domain"
        mockMvc.perform(post("/domains/{id}/component-types", 999999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isNotFound())

        then: "component type is not created"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return conflict when creating component type with duplicate name in same domain"() {
        given: "a domain is created"
        def createDomainRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")

        def domainResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDomainRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def domain = objectMapper.readValue(domainResult.response.contentAsString, Domain)
        def domainId = domain.getId()

        and: "a component type is created"
        def createRequest1 = new CreateComponentTypeRequest()
                .name("Test Component Type")
                .code("TEST_CODE_1")
                .description("Test Description 1")
                .orderIndex(1)

        mockMvc.perform(post("/domains/{id}/component-types", domainId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest1)))
                .andExpect(status().isCreated())

        and: "another component type with the same name is attempted to be created"
        def createRequest2 = new CreateComponentTypeRequest()
                .name("Test Component Type")
                .code("TEST_CODE_2")
                .description("Test Description 2")
                .orderIndex(2)

        when: "POST request is sent to create component type with duplicate name"
        mockMvc.perform(post("/domains/{id}/component-types", domainId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest2)))
                .andExpect(status().isConflict())

        then: "component type is not created"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should get component types by domain id successfully"() {
        given: "a domain is created"
        def createDomainRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")

        def domainResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDomainRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def domain = objectMapper.readValue(domainResult.response.contentAsString, Domain)
        def domainId = domain.getId()

        and: "multiple component types are created"
        3.times { i ->
            def createRequest = new CreateComponentTypeRequest()
                    .name("Component Type ${i}")
                    .code("CODE_${i}")
                    .description("Description ${i}")
                    .orderIndex(i)

            mockMvc.perform(post("/domains/{id}/component-types", domainId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
        }

        when: "GET request is sent to retrieve component types by domain id"
        def result = mockMvc.perform(get("/domains/{id}/component-types", domainId))
                .andExpect(status().isOk())
                .andReturn()

        then: "component types are retrieved successfully"
        def responseBody = objectMapper.readValue(result.response.contentAsString, ComponentType[].class)
        responseBody.length == 3
        responseBody.each { componentType ->
            componentType.getDomainId() == domainId
        }
        responseBody*.getName().sort() == ["Component Type 0", "Component Type 1", "Component Type 2"].sort()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return not found when getting component types for non-existent domain"() {
        when: "GET request is sent for component types of non-existent domain"
        mockMvc.perform(get("/domains/{id}/component-types", 999999L))
                .andExpect(status().isNotFound())

        then: "not found error is returned"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should get component type by id successfully"() {
        given: "a domain is created"
        def createDomainRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")

        def domainResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDomainRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def domain = objectMapper.readValue(domainResult.response.contentAsString, Domain)
        def domainId = domain.getId()

        and: "a component type is created"
        def createRequest = new CreateComponentTypeRequest()
                .name("Test Component Type")
                .code("TEST_CODE")
                .description("Test Description")
                .orderIndex(1)

        def createResult = mockMvc.perform(post("/domains/{id}/component-types", domainId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def createdComponentType = objectMapper.readValue(createResult.response.contentAsString, ComponentType)
        def componentTypeId = createdComponentType.getId()

        when: "GET request is sent to retrieve component type by id"
        def getResult = mockMvc.perform(get("/component-types/{id}", componentTypeId))
                .andExpect(status().isOk())
                .andReturn()

        then: "component type is retrieved successfully"
        def responseBody = objectMapper.readValue(getResult.response.contentAsString, ComponentType)
        responseBody.getId() == componentTypeId
        responseBody.getDomainId() == domainId
        responseBody.getName() == "Test Component Type"
        responseBody.getCode() == "TEST_CODE"
        responseBody.getDescription() == "Test Description"
        responseBody.getOrderIndex() == 1
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return not found when getting non-existent component type"() {
        when: "GET request is sent for non-existent component type"
        mockMvc.perform(get("/component-types/{id}", 999999L))
                .andExpect(status().isNotFound())

        then: "not found error is returned"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should update component type successfully"() {
        given: "a domain is created"
        def createDomainRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")

        def domainResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDomainRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def domain = objectMapper.readValue(domainResult.response.contentAsString, Domain)
        def domainId = domain.getId()

        and: "a component type is created"
        def createRequest = new CreateComponentTypeRequest()
                .name("Original Component Type")
                .code("ORIGINAL_CODE")
                .description("Original Description")
                .orderIndex(1)

        def createResult = mockMvc.perform(post("/domains/{id}/component-types", domainId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def createdComponentType = objectMapper.readValue(createResult.response.contentAsString, ComponentType)
        def componentTypeId = createdComponentType.getId()

        and: "an update request"
        def updateRequest = new CreateComponentTypeRequest()
                .name("Updated Component Type")
                .code("UPDATED_CODE")
                .description("Updated Description")
                .orderIndex(2)

        when: "PUT request is sent to update component type"
        def updateResult = mockMvc.perform(put("/component-types/{id}", componentTypeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andReturn()

        then: "component type is updated successfully"
        def responseBody = objectMapper.readValue(updateResult.response.contentAsString, ComponentType)
        responseBody.getId() == componentTypeId
        responseBody.getDomainId() == domainId
        responseBody.getName() == "Updated Component Type"
        responseBody.getCode() == "UPDATED_CODE"
        responseBody.getDescription() == "Updated Description"
        responseBody.getOrderIndex() == 2
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return not found when updating non-existent component type"() {
        given: "an update request for non-existent component type"
        def updateRequest = new CreateComponentTypeRequest()
                .name("Updated Component Type")
                .code("UPDATED_CODE")
                .description("Updated Description")
                .orderIndex(2)

        when: "PUT request is sent to update non-existent component type"
        mockMvc.perform(put("/component-types/{id}", 999999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())

        then: "not found error is returned"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return bad request when updating component type with empty name"() {
        given: "a domain is created"
        def createDomainRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")

        def domainResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDomainRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def domain = objectMapper.readValue(domainResult.response.contentAsString, Domain)
        def domainId = domain.getId()

        and: "a component type is created"
        def createRequest = new CreateComponentTypeRequest()
                .name("Original Component Type")
                .code("ORIGINAL_CODE")
                .description("Original Description")
                .orderIndex(1)

        def createResult = mockMvc.perform(post("/domains/{id}/component-types", domainId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def createdComponentType = objectMapper.readValue(createResult.response.contentAsString, ComponentType)
        def componentTypeId = createdComponentType.getId()

        and: "an invalid update request with empty name"
        def updateRequest = new CreateComponentTypeRequest()
                .name("")
                .code("UPDATED_CODE")
                .description("Updated Description")
                .orderIndex(2)

        when: "PUT request is sent to update component type"
        mockMvc.perform(put("/component-types/{id}", componentTypeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())

        then: "bad request error is returned"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should delete component type successfully"() {
        given: "a domain is created"
        def createDomainRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")

        def domainResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDomainRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def domain = objectMapper.readValue(domainResult.response.contentAsString, Domain)
        def domainId = domain.getId()

        and: "a component type is created"
        def createRequest = new CreateComponentTypeRequest()
                .name("Component Type to Delete")
                .code("DELETE_CODE")
                .description("Description")
                .orderIndex(1)

        def createResult = mockMvc.perform(post("/domains/{id}/component-types", domainId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def createdComponentType = objectMapper.readValue(createResult.response.contentAsString, ComponentType)
        def componentTypeId = createdComponentType.getId()

        when: "DELETE request is sent to delete component type"
        mockMvc.perform(delete("/component-types/{id}", componentTypeId))
                .andExpect(status().isNoContent())

        then: "component type is deleted"
        mockMvc.perform(get("/component-types/{id}", componentTypeId))
                .andExpect(status().isNotFound())
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return not found when deleting non-existent component type"() {
        when: "DELETE request is sent for non-existent component type"
        mockMvc.perform(delete("/component-types/{id}", 999999L))
                .andExpect(status().isNotFound())

        then: "not found error is returned"
        noExceptionThrown()
    }

    @Shared
    ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}
