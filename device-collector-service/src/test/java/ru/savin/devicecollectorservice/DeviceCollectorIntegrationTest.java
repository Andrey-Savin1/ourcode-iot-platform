package ru.savin.devicecollectorservice;



import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.savin.devicecollectorservice.model.Device;
import ru.savin.devicecollectorservice.util.BaseConfig;
import ru.savin.devicecollectorservice.util.TestContainersConfig;
import ru.savin.eventscollectorservice.KafkaDevice;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.savin.devicecollectorservice.util.KafkaTestUtils.createConsumer;
import static ru.savin.devicecollectorservice.util.KafkaTestUtils.createProducer;
import static ru.savin.devicecollectorservice.util.TestContainersConfig.POSTGRES_0;
import static ru.savin.devicecollectorservice.util.TestContainersConfig.POSTGRES_1;


@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class DeviceCollectorIntegrationTest extends BaseConfig {

    @BeforeAll
    public static void createDevicesTableInShard() {
        executeSql(POSTGRES_0, """
                CREATE TABLE IF NOT EXISTS device_0.public.devices (
                    device_id text primary key,
                    device_type text,
                    created_at bigint,
                    meta text
                );
                """);

        executeSql(POSTGRES_1, """
                CREATE TABLE IF NOT EXISTS device_1.public.devices (
                    device_id text primary key,
                    device_type text,
                    created_at bigint,
                    meta text
                );""");
    }

    private static void executeSql(PostgreSQLContainer<?> container, String sql) {
        try (Connection conn = container.createConnection("");
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to execute SQL", e);
        }
    }

    @Test
    @DisplayName("Проверяем кейс десериализации Avro, \n  проверка записи в postgres \n и распределение по шардам")
    void EventsLogicIntegrationTest() {

        // 1. Подготовка тестового сообщения для кафки
        KafkaDevice testData = KafkaDevice.newBuilder()
                .setDeviceType("1")
                .setDeviceId("1")
                .setCreatedAt(System.currentTimeMillis())
                .setMeta("1")
                .build();

        KafkaDevice testData2 = KafkaDevice.newBuilder()
                .setDeviceType("2")
                .setDeviceId("2")
                .setCreatedAt(System.currentTimeMillis())
                .setMeta("2")
                .build();

        try (KafkaProducer<String, KafkaDevice> producer = createProducer();
             KafkaConsumer<String, KafkaDevice> consumer = createConsumer("device-id-topic")) {

            // 2. Отправка сообщения в Kafka
            var future = producer.send(new ProducerRecord<>("device-id-topic", "key", testData));
            producer.send(new ProducerRecord<>("device-id-topic", "key2", testData2)).get();
            RecordMetadata metadata = future.get();
            log.debug("Message sent to topic: {}, partition: {}, offset: {}", metadata.topic(), metadata.partition(), metadata.offset());

            // 3. Получение сообщений
            ConsumerRecords<String, KafkaDevice> records = consumer.poll(Duration.ofSeconds(5));
            if (records.count() < 2) {
                throw new RuntimeException("Expected 2 messages, but received " + records.count());
            }

            List<Device> firstShard = new ArrayList<>();
            List<Device> secondShard = new ArrayList<>();

            // RowMapper для маппинга результата в объект Device
            RowMapper<Device> rowMapper = (rs, rowNum) -> {
                Device device = new Device();
                device.setDeviceId(rs.getString("device_id"));
                device.setDeviceType(rs.getString("device_type"));
                device.setCreatedAt(rs.getLong("created_at"));
                device.setMeta(rs.getString("meta"));
                return device;
            };

            Thread.sleep(5000);

            // Запрос к первому шарду
            try (HikariDataSource ds0 = createDataSource(POSTGRES_0)) {
                JdbcTemplate template0 = new JdbcTemplate(ds0);
                List<Device> devices0 = template0.query("SELECT * FROM device_0.public.devices", rowMapper);
                firstShard.addAll(devices0);
            }

            // Запрос ко второму шарду
            try (HikariDataSource ds1 = createDataSource(POSTGRES_1)) {
                JdbcTemplate template1 = new JdbcTemplate(ds1);
                List<Device> devices1 = template1.query("SELECT * FROM device_1.public.devices", rowMapper);
                secondShard.addAll(devices1);
            }

            // 5. Получил первые элементы
            var deviceShard0 = firstShard.stream().findFirst().get();
            var deviceShard1 = secondShard.stream().findFirst().get();

            //Проверяю количество объектов которые достали из базы
            assertEquals(1, firstShard.size());
            assertEquals(1, secondShard.size());
            //Проверяю deviceId объектов из шард
            assertEquals(deviceShard0.getDeviceId(), "2");
            assertEquals(deviceShard1.getDeviceId(), "1");

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // Настройка DataSource для шард
    private HikariDataSource createDataSource(TestContainersConfig.FixedPortPostgreSQLContainer config) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(config.getJdbcUrl());
        ds.setUsername(config.getUsername());
        ds.setPassword(config.getPassword());
        log.debug("ДАТАСОРС {} {}",ds.getPassword(), ds.getJdbcUrl());
        return ds;
    }
}
