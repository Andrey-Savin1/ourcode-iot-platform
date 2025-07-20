package ru.savin.deviceservice.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static ru.savin.deviceservice.util.TestContainersConfig.*;


@Slf4j
@Testcontainers
public abstract class BaseConfig {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {

        registry.add("spring.shardingsphere.datasource.ds_0.url", POSTGRES_0::getJdbcUrl);
        registry.add("spring.shardingsphere.datasource.ds_0.username", POSTGRES_0::getUsername);
        registry.add("spring.shardingsphere.datasource.ds_0.password", POSTGRES_0::getPassword);

        registry.add("spring.shardingsphere.datasource.ds_1.url", POSTGRES_1::getJdbcUrl);
        registry.add("spring.shardingsphere.datasource.ds_1.username", POSTGRES_1::getUsername);
        registry.add("spring.shardingsphere.datasource.ds_1.password", POSTGRES_1::getPassword);
        registry.add("spring.flyway.locations", () -> "classpath:/db/migration-test");
        registry.add("spring.flyway.locations", () -> "classpath:/db/migration-test");

        int keycloakPort = KEYCLOAK_CONTAINER.getMappedPort(8080);
        String keycloakUrl = "http://localhost:" + keycloakPort;
        registry.add("spring.security.oauth2.resource-server.jwt.issuer-uri",
                () -> keycloakUrl + "/realms/devices");

        registry.add("spring.security.oauth2.resource-server.jwt.jwk-set-uri",
                () -> keycloakUrl + "/realms/devices/protocol/openid-connect/certs");

        registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
        registry.add("spring.data.redis.port", () -> REDIS_CONTAINER.getMappedPort(6379));

    }
}
