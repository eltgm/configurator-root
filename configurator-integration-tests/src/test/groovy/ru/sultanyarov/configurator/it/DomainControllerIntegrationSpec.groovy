package ru.sultanyarov.configurator.it

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import ru.sultanyarov.configurator.domain.dto.CreateDomainRequest
import ru.sultanyarov.configurator.domain.dto.Domain
import ru.sultanyarov.configurator.domain.dto.UpdateDomainRequest
import spock.lang.Shared

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class DomainControllerIntegrationSpec extends BaseIntegrationSpec {

    @Autowired
    WebApplicationContext webApplicationContext

    MockMvc mockMvc

    def setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should create domain successfully"() {
        given: "a valid create domain request"
        def createRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")

        when: "POST request is sent to create domain"
        def result = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        then: "domain is created successfully"
        def responseBody = objectMapper.readValue(result.response.contentAsString, Domain)
        responseBody.getId() != null
        responseBody.getName() == "Test Domain"
        responseBody.getDescription() == "Test Description"
        responseBody.getCreatedAt() != null
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return bad request when creating domain with empty name"() {
        given: "an invalid create domain request with empty name"
        def createRequest = new CreateDomainRequest()
                .name("")
                .description("Test Description")

        when: "POST request is sent to create domain"
        mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())

        then: "domain is not created"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return bad request when creating domain with null name"() {
        given: "an invalid create domain request with null name"
        def createRequest = new CreateDomainRequest()
                .description("Test Description")

        when: "POST request is sent to create domain"
        mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())

        then: "domain is not created"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should get domain by id successfully"() {
        given: "a domain is created"
        def createRequest = new CreateDomainRequest()
                .name("Test Domain")
                .description("Test Description")

        def createResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def createdDomain = objectMapper.readValue(createResult.response.contentAsString, Domain)
        def domainId = createdDomain.getId()

        when: "GET request is sent to retrieve domain by id"
        def getResult = mockMvc.perform(get("/domains/{id}", domainId))
                .andExpect(status().isOk())
                .andReturn()

        then: "domain is retrieved successfully"
        def responseBody = objectMapper.readValue(getResult.response.contentAsString, Domain)
        responseBody.getId() == domainId
        responseBody.getName() == "Test Domain"
        responseBody.getDescription() == "Test Description"
        responseBody.getCreatedAt() != null
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return not found when getting non-existent domain"() {
        when: "GET request is sent for non-existent domain"
        mockMvc.perform(get("/domains/{id}", 999999L))
                .andExpect(status().isNotFound())

        then: "not found error is returned"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should update domain successfully"() {
        given: "a domain is created"
        def createRequest = new CreateDomainRequest()
                .name("Original Domain")
                .description("Original Description")

        def createResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def createdDomain = objectMapper.readValue(createResult.response.contentAsString, Domain)
        def domainId = createdDomain.getId()

        and: "an update request"
        def updateRequest = new UpdateDomainRequest()
                .name("Updated Domain")
                .description("Updated Description")

        when: "PUT request is sent to update domain"
        def updateResult = mockMvc.perform(put("/domains/{id}", domainId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andReturn()

        then: "domain is updated successfully"
        def responseBody = objectMapper.readValue(updateResult.response.contentAsString, Domain)
        responseBody.getId() == domainId
        responseBody.getName() == "Updated Domain"
        responseBody.getDescription() == "Updated Description"
        responseBody.getCreatedAt() != null
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return not found when updating non-existent domain"() {
        given: "an update request for non-existent domain"
        def updateRequest = new UpdateDomainRequest()
                .name("Updated Domain")
                .description("Updated Description")

        when: "PUT request is sent to update non-existent domain"
        mockMvc.perform(put("/domains/{id}", 999999L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound())

        then: "not found error is returned"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return bad request when updating domain with empty name"() {
        given: "a domain is created"
        def createRequest = new CreateDomainRequest()
                .name("Original Domain")
                .description("Original Description")

        def createResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def createdDomain = objectMapper.readValue(createResult.response.contentAsString, Domain)
        def domainId = createdDomain.getId()

        and: "an invalid update request with empty name"
        def updateRequest = new UpdateDomainRequest()
                .name("")
                .description("Updated Description")

        when: "PUT request is sent to update domain"
        mockMvc.perform(put("/domains/{id}", domainId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isBadRequest())

        then: "bad request error is returned"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should delete domain successfully"() {
        given: "a domain is created"
        def createRequest = new CreateDomainRequest()
                .name("Domain to Delete")
                .description("Description")

        def createResult = mockMvc.perform(post("/domains")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn()

        def createdDomain = objectMapper.readValue(createResult.response.contentAsString, Domain)
        def domainId = createdDomain.getId()

        when: "DELETE request is sent to delete domain"
        mockMvc.perform(delete("/domains/{id}", domainId))
                .andExpect(status().isNoContent())

        then: "domain is deleted"
        mockMvc.perform(get("/domains/{id}", domainId))
                .andExpect(status().isNotFound())
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should return not found when deleting non-existent domain"() {
        when: "DELETE request is sent for non-existent domain"
        mockMvc.perform(delete("/domains/{id}", 999999L))
                .andExpect(status().isNotFound())

        then: "not found error is returned"
        noExceptionThrown()
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should get domains with pagination"() {
        given: "multiple domains are created"
        5.times { i ->
            def createRequest = new CreateDomainRequest()
                    .name("Domain ${i}")
                    .description("Description ${i}")

            mockMvc.perform(post("/domains")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
        }

        when: "GET request is sent to retrieve domains with pagination"
        def result = mockMvc.perform(get("/domains")
                .param("page", "0")
                .param("size", "3"))
                .andExpect(status().isOk())
                .andReturn()

        then: "domains are retrieved with correct pagination"
        def responseBody = objectMapper.readValue(result.response.contentAsString, ru.sultanyarov.configurator.domain.dto.DomainPage)
        responseBody.getItems().size() == 3
        responseBody.getPage() == 0
        responseBody.getSize() == 3
        responseBody.getTotalItems() >= 5
    }

    @Sql(scripts = ["/sql/clear-db.sql"])
    def "should handle default pagination when parameters are not provided"() {
        given: "multiple domains are created"
        3.times { i ->
            def createRequest = new CreateDomainRequest()
                    .name("Domain ${i}")
                    .description("Description ${i}")

            mockMvc.perform(post("/domains")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
                    .andExpect(status().isCreated())
        }

        when: "GET request is sent without pagination parameters"
        def result = mockMvc.perform(get("/domains"))
                .andExpect(status().isOk())
                .andReturn()

        then: "domains are retrieved with default pagination"
        def responseBody = objectMapper.readValue(result.response.contentAsString, ru.sultanyarov.configurator.domain.dto.DomainPage)
        responseBody.getItems() != null
        responseBody.getPage() != null
        responseBody.getSize() != null
        responseBody.getTotalItems() != null
    }

    @Shared
    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            .disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
}
