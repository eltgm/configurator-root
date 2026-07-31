package ru.sultanyarov.configurator;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.mockito.Mockito.mockStatic;

class ConfiguratorApplicationTest {

    @Test
    void main_shouldDelegateToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApplication = mockStatic(SpringApplication.class)) {
            ConfiguratorApplication.main(new String[]{"--spring.profiles.active=test"});

            springApplication.verify(() -> SpringApplication.run(ConfiguratorApplication.class, new String[]{"--spring.profiles.active=test"}));
        }
    }
}
