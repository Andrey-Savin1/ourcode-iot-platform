package ru.savin.eventscollectorservice.util;

import com.datastax.oss.driver.api.core.CqlSession;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.InetSocketAddress;

import static ru.savin.eventscollectorservice.util.TestContainersConfig.cassandra;
import static ru.savin.eventscollectorservice.util.TestContainersConfig.schemaRegistry;


@Slf4j
@Testcontainers
public abstract class BaseConfig {

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {

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
        registry.add("spring.cassandra.contact-points",
                () -> cassandra.getHost() + ":" + cassandra.getMappedPort(9042));
    }

    @BeforeAll
    static void setupCassandra() {

        // Создаем keyspace
        try (CqlSession session = CqlSession.builder()
                .addContactPoint(new InetSocketAddress(cassandra.getHost(), cassandra.getMappedPort(9042)))
                .withLocalDatacenter("datacenter1")
                .build()) {

            String createKeyspace = "CREATE KEYSPACE IF NOT EXISTS device " +
                    "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}";
            String createTableDeviceEvent = "CREATE TABLE IF NOT EXISTS device.device_events_by_device\n" +
                    "              (\n" +
                    "               device_id text,\n" +
                    "               event_id  text,\n" +
                    "               timestamp bigint,\n" +
                    "               type      text,\n" +
                    "               payload   text,\n" +
                    "               PRIMARY KEY (device_id, timestamp)\n" +
                    "               ) WITH CLUSTERING ORDER BY (timestamp DESC);";

            String createTableDeviceId = "CREATE TABLE IF NOT EXISTS device.device_ids (device_id TEXT PRIMARY KEY)";

            session.execute("SELECT release_version FROM system.local");
            log.info("Cassandra доступна");
            session.execute(createKeyspace);
            session.execute(createTableDeviceEvent);
            session.execute(createTableDeviceId);

            log.info("Keyspace и таблицы успешно созданы или уже существуют.");
        } catch (Exception e) {
            log.error("Ошибка при создании keyspace 'device': {}", e.getMessage());
        }
    }

}
