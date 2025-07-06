package ru.savin.devicecollectorservice.util;


import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import static ru.savin.devicecollectorservice.util.TestContainersConfig.*;


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

        registry.add("spring.kafka.bootstrap-servers", TestContainersConfig.kafkaTest::getBootstrapServers);
        registry.add("kafka.consumer.enabled", () -> "true");
        registry.add("spring.kafka.producer.properties.schema.registry.url", () ->
                "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081)));
        registry.add("spring.kafka.consumer.properties.schema.registry.url", () ->
                "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081)));
        registry.add("spring.kafka.properties.schema.registry.url", () ->
                "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081)));
        registry.add("spring.kafka.properties.schema.registry.ssl.endpoint.identification.algorithm", () -> "");
        registry.add("spring.kafka.properties.schema.registry.basic.auth.user.info", () -> "");
    }
}
