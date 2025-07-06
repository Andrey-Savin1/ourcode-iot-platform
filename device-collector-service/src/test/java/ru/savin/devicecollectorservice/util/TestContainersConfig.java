package ru.savin.devicecollectorservice.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;


@Slf4j
@RequiredArgsConstructor
@SpringBootTest
@Testcontainers
public class TestContainersConfig {

    private static final String CONFLUENT_VERSION = "7.7.3";
    private static final Network network = Network.newNetwork();

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


    static {
        log.info("Starting test containers...");
        POSTGRES_0.start();
        POSTGRES_1.start();
        kafkaTest.start();
        schemaRegistry.start();
        log.info("Containers started successfully");
    }

}
