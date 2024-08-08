package com.demo.project100.service;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

public abstract class BaseTest {
    @Container
    static PostgreSQLContainer postgresqlContainer = (PostgreSQLContainer) new PostgreSQLContainer("postgres:14")
            .withDatabaseName("test-db")
            .withUsername("user")
            .withPassword("password")
            .withReuse(true);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresqlContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresqlContainer::getUsername);
        registry.add("spring.datasource.password", postgresqlContainer::getPassword);
    }
}
