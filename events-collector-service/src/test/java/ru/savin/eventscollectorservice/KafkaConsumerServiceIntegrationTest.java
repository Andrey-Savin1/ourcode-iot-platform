package ru.savin.eventscollectorservice;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.savin.eventscollectorservice.repository.DeviceEventsRepository;
import ru.savin.eventscollectorservice.repository.DeviceIdRepository;
import ru.savin.eventscollectorservice.service.HandleEventService;
import ru.savin.eventscollectorservice.util.BaseConfig;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.savin.eventscollectorservice.util.KafkaTestUtils.createConsumer;
import static ru.savin.eventscollectorservice.util.KafkaTestUtils.createProducer;


@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Slf4j
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class KafkaConsumerServiceIntegrationTest extends BaseConfig {

    private final DeviceEventsRepository deviceEventsRepository;
    private final DeviceIdRepository deviceIdRepository;
    private final HandleEventService handleEventService;

    @Test
    @DisplayName("Проверяем кейс десериализации Avro, \n  проверка записи в касандру \n и уникальности device_id")
    void EventsLogicIntegrationTest() {

        // 1. Подготовка тестового сообщения для кафки
        DeviceEvent testData = DeviceEvent.newBuilder()
                .setEventId("1")
                .setDeviceId("2")
                .setTimestamp(System.currentTimeMillis())
                .setType("type")
                .setPayload("payload")
                .build();

        DeviceEvent testData2 = DeviceEvent.newBuilder()
                .setEventId("11")
                .setDeviceId("2")
                .setTimestamp(System.currentTimeMillis() + 1)
                .setType("type1")
                .setPayload("payload1")
                .build();

        try (KafkaProducer<String, DeviceEvent> producer = createProducer();
             KafkaConsumer<String, DeviceEvent> consumer = createConsumer("test")) {

            // 2. Отправка сообщения в Kafka
            var future = producer.send(new ProducerRecord<>("test", "key", testData));
            producer.send(new ProducerRecord<>("test", "key2", testData2)).get();
            RecordMetadata metadata = future.get();
            log.debug("Message sent to topic: {}, partition: {}, offset: {}", metadata.topic(), metadata.partition(), metadata.offset());

            // 3. Получение сообщений
            ConsumerRecords<String, DeviceEvent> records = consumer.poll(Duration.ofSeconds(5)); // Увеличил таймаут
            if (records.count() < 2) {
                throw new RuntimeException("Expected 2 messages, but received " + records.count());
            }

            // 4. Обработка всех полученных сообщений
            for (ConsumerRecord<String, DeviceEvent> record : records) {
                var data = record.value();
                log.debug("Сообщение из кафки: {}", data);
                handleEventService.handleEvents(data);
            }
            //Сон 2 секунды чтоб отработал аптайм батча
            Thread.sleep(2000);

            // 5. Получили из базы
            var event = deviceEventsRepository.findAll();
            var first = event.getFirst();
            var second = event.getLast();
            var allDeviceId = deviceIdRepository.findAll();

            //Проверяю корректность сохранения событий в касандре
            assertEquals(2, event.size());
            //Проверяем что последние сохраненные события достаются первыми
            assertEquals(testData2.getPayload(), first.getPayload());
            assertEquals(testData.getPayload(), second.getPayload());
            //Проверяю количество сохраненных уникальных device_id
            assertEquals(1, allDeviceId.size());
            assertEquals(allDeviceId.getFirst().getDeviceId(), "2");

        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
