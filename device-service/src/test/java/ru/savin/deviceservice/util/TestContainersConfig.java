package ru.savin.deviceservice.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;


@Slf4j
@RequiredArgsConstructor
@SpringBootTest
@Testcontainers
public class TestContainersConfig {


    public static class FixedPortPostgreSQLContainer extends PostgreSQLContainer<FixedPortPostgreSQLContainer> {
        public FixedPortPostgreSQLContainer(String dockerImageName) {
            super(dockerImageName);
        }

        public FixedPortPostgreSQLContainer withFixedExposedPort(int hostPort, int containerPort) {
            super.addFixedExposedPort(hostPort, containerPort);
            return this;
        }
    }

    @Container
    public static FixedPortPostgreSQLContainer POSTGRES_0 = new FixedPortPostgreSQLContainer("postgres:latest")
            .withFixedExposedPort(53160, 5432)
            .withDatabaseName("device_0")
            .withUsername("admin")
            .withPassword("admin")
            .withReuse(true);

    @Container
    public static FixedPortPostgreSQLContainer POSTGRES_1 = new FixedPortPostgreSQLContainer("postgres:latest")
            .withFixedExposedPort(53158, 5432)
            .withDatabaseName("device_1")
            .withUsername("admin")
            .withPassword("admin")
            .withReuse(true);

    @Container
    public static GenericContainer<?> KEYCLOAK_CONTAINER = new GenericContainer<>("quay.io/keycloak/keycloak:26.0")
            .withExposedPorts(8080)
            .withEnv("KC_BOOTSTRAP_ADMIN_USERNAME", "admin")
            .withEnv("KC_BOOTSTRAP_ADMIN_PASSWORD", "admin")
            .withEnv("KC_HOSTNAME", "localhost")
            .withEnv("KC_HOSTNAME_STRICT", "true")
            .withCommand("start-dev", "--import-realm")
            .withClasspathResourceMapping(
                    "keycloak/realm-export.json",
                    "/opt/keycloak/data/import/realm-export.json",
                    BindMode.READ_ONLY
            )
            .withLogConsumer(new Slf4jLogConsumer(log));


    @Container
    public static GenericContainer<?> REDIS_CONTAINER = new GenericContainer<>(
            DockerImageName.parse("redis:8.0-alpine"))
            .withExposedPorts(6379);


    static {
        log.info("Starting test containers...");
        POSTGRES_0.start();
        POSTGRES_1.start();
        KEYCLOAK_CONTAINER.start();
        REDIS_CONTAINER.start();
        log.info("Containers started successfully");
    }

}
