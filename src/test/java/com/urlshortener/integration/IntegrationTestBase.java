package com.urlshortener.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

/**
 * Starts ONE real MySQL and ONE real Redis for the whole test run and points
 * the Spring app at them. Requires Docker to be running.
 */
@SpringBootTest
public abstract class IntegrationTestBase {

    protected static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("app.jwt.secret",
                () -> "integration-test-secret-key-that-is-definitely-longer-than-64-characters-0123456789");
    }
}