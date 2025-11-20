package ru.sultanyarov.configurator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class ConfiguratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfiguratorApplication.class, args);
    }
}
