package ru.sultanyarov.configurator.it

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import ru.sultanyarov.configurator.ConfiguratorApplication
import ru.sultanyarov.configurator.contract.AbstractConfiguratorControllerContract
import ru.sultanyarov.configurator.contract.TestResponse

import javax.sql.DataSource

@ActiveProfiles("test")
@SpringBootTest(
        classes = ConfiguratorApplication,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK
)
@AutoConfigureMockMvc
class ConfiguratorControllerIntegrationSpec extends AbstractConfiguratorControllerContract {
    @Autowired
    MockMvc mockMvc

    @Autowired
    DataSource dataSource

    final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    @Override
    void runSqlScripts(String... scripts) {
        def populator = new ResourceDatabasePopulator()
        scripts.each { script ->
            populator.addScript(new ClassPathResource(script.startsWith("/") ? script.substring(1) : script))
        }
        populator.execute(dataSource)
    }

    @Override
    TestResponse get(String path) {
        def result = mockMvc.perform(MockMvcRequestBuilders.get(path)).andReturn()
        return new TestResponse(result.response.status, result.response.contentAsString)
    }

    @Override
    TestResponse get(String path, Map<String, ?> queryParams) {
        def requestBuilder = MockMvcRequestBuilders.get(path)
        queryParams.each { key, value -> requestBuilder.param(key, String.valueOf(value)) }
        def result = mockMvc.perform(requestBuilder).andReturn()
        return new TestResponse(result.response.status, result.response.contentAsString)
    }

    @Override
    TestResponse post(String path, Object body) {
        throw new UnsupportedOperationException()
    }

    @Override
    TestResponse put(String path, Object body) {
        throw new UnsupportedOperationException()
    }

    @Override
    TestResponse delete(String path) {
        throw new UnsupportedOperationException()
    }
}
