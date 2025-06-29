package ru.savin.eventscollectorservice.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
@RequiredArgsConstructor
@SpringBootTest
public class TestContainersConfig {


    private static final String CONFLUENT_VERSION = "7.7.3";
    private static final Network network = Network.newNetwork();

    @Container
    public static final ConfluentKafkaContainer kafkaTest = new ConfluentKafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka").withTag(CONFLUENT_VERSION))
            .withListener("tc-kafka:19092")
            .withNetwork(network).withNetworkMode(network.getId())
            .withNetworkAliases("tc-kafka")
            .withReuse(true);

    @Container
    public static final GenericContainer<?> schemaRegistry =
            new GenericContainer<>("confluentinc/cp-schema-registry:7.7.3")
                    .withExposedPorts(8081)
                    .withNetwork(network).withNetworkMode(network.getId())
                    .withNetworkAliases("schemaregistry")
                    .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schemaregistry")
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://tc-kafka:19092")
                    .withEnv("SCHEMA_REGISTRY_KAFKASTORE_SECURITY_PROTOCOL", "PLAINTEXT")
                    .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                    .waitingFor(Wait.forHttp("/subjects").forStatusCode(200))
                    .dependsOn(kafkaTest);


    @Container
    public static final CassandraContainer<?> cassandra = new CassandraContainer<>("cassandra:4.1.9")
            .withNetwork(network)
            .withExposedPorts(9042)
            .withNetworkAliases("cassandra");

//
//    @DynamicPropertySource
//    static void properties(DynamicPropertyRegistry registry) {
//
//        registry.add("spring.kafka.bootstrap-servers", kafkaTest::getBootstrapServers);
//        registry.add("kafka.consumer.enabled", () -> "true");
//        registry.add("spring.kafka.producer.properties.schema.registry.url", () ->
//                "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081)));
//        registry.add("spring.kafka.consumer.properties.schema.registry.url", () ->
//                "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081)));
//        registry.add("spring.kafka.properties.schema.registry.url", () ->
//                "http://%s:%d".formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081)));
//        registry.add("spring.kafka.properties.schema.registry.ssl.endpoint.identification.algorithm", () -> "");
//        registry.add("spring.kafka.properties.schema.registry.basic.auth.user.info", () -> "");
//        registry.add("spring.cassandra.contact-points",
//                () -> cassandra.getHost() + ":" + cassandra.getMappedPort(9042));
//    }
//
//    @BeforeAll
//    static void setupCassandra() {
//
//        // Создаем keyspace
//        try (CqlSession session = CqlSession.builder()
//                .addContactPoint(new InetSocketAddress(cassandra.getHost(), cassandra.getMappedPort(9042)))
//                .withLocalDatacenter("datacenter1")
//                .build()) {
//
//            String createKeyspace = "CREATE KEYSPACE IF NOT EXISTS device " +
//                    "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}";
//            String createTableDeviceEvent = "CREATE TABLE IF NOT EXISTS device.device_events_by_device\n" +
//                    "              (\n" +
//                    "               device_id text,\n" +
//                    "               event_id  text,\n" +
//                    "               timestamp bigint,\n" +
//                    "               type      text,\n" +
//                    "               payload   text,\n" +
//                    "               PRIMARY KEY (device_id, timestamp)\n" +
//                    "               ) WITH CLUSTERING ORDER BY (timestamp DESC);";
//
//            String createTableDeviceId = "CREATE TABLE IF NOT EXISTS device.device_ids (device_id TEXT PRIMARY KEY)";
//
//            session.execute("SELECT release_version FROM system.local");
//            log.info("✅ Cassandra доступна");
//            session.execute(createKeyspace);
//            session.execute(createTableDeviceEvent);
//            session.execute(createTableDeviceId);
//
//            log.info("✅ Keyspace и таблицы успешно созданы или уже существуют.");
//        } catch (Exception e) {
//            log.error("Ошибка при создании keyspace 'device': {}", e.getMessage());
//        }
//    }



    static {
        cassandra.start();
        kafkaTest.start();
        schemaRegistry.start();
    }


    /**
     * Метод для регистрации новой схемы в schemaRegistry (при необходимости)
     * (ВАЖНО: в названии схемы должно быть -value (notification-value, myschema-value))
     */
    public static void registerSchema() {
        String schemaRegistryUrl = "http://%s:%d/subjects/notification-value/versions"
                .formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081));

        String schema = "{\n" +
                "  \"schema\": \"{\\\"type\\\":\\\"record\\\",\\\"name\\\":\\\"CreateNotificationData\\\",\\\"namespace\\\":\\\"ru.savin.notificationhub.dto\\\",\\\"fields\\\":[{\\\"name\\\":\\\"userId\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"message\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"message\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"email\\\",\\\"type\\\":\\\"string\\\"},{\\\"name\\\":\\\"actionType\\\",\\\"type\\\":\\\"string\\\"}]}\"\n" +
                "}";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.schemaregistry.v1+json"));
        HttpEntity<String> request = new HttpEntity<>(schema, headers);
        String response = restTemplate.postForObject(schemaRegistryUrl, request, String.class);
        log.debug("Schema registration response: {}", response);

    }

    /**
     * Метод для получения схемы из schemaRegistry
     */
    public static String getSchemaById(int schemaId) {
        String schemaRegistryUrl = "http://%s:%d/notification-value/versions/latest"
                .formatted(schemaRegistry.getHost(), schemaRegistry.getMappedPort(8081));

        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(schemaRegistryUrl, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return response.getBody();
        } else {
            throw new RuntimeException("Failed to fetch schema with ID " + schemaId + ". Status code: " + response.getStatusCode());
        }
    }

}
