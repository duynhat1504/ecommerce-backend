package com.duynhat.ecommerce_backend.integration;

import com.duynhat.ecommerce_backend.modules.auth.email.EmailService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {
    private static final int REDIS_PORT = 6379;

    @MockitoBean
    protected EmailService emailService;

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer("postgres:16-alpine")
                    .withDatabaseName("ecommerce_test")
                    .withUsername("test")
                    .withPassword("test");

    protected static final GenericContainer<?> REDIS =
            new GenericContainer<>(
                    DockerImageName.parse("redis:7.4-alpine")
            )
                    .withExposedPorts(REDIS_PORT);


    static {
        POSTGRES.start();
        REDIS.start();
    }

    @DynamicPropertySource
    static void redisProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add(
                "spring.data.redis.host",
                REDIS::getHost
        );

        registry.add(
                "spring.data.redis.port",
                () -> REDIS.getMappedPort(REDIS_PORT)
        );
    }
}
