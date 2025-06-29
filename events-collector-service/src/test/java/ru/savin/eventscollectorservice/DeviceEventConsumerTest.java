package ru.savin.eventscollectorservice;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.savin.eventscollectorservice.config.CustomMetricsConfig;
import ru.savin.eventscollectorservice.repository.DeviceEventsRepository;
import ru.savin.eventscollectorservice.repository.DeviceIdRepository;
import ru.savin.eventscollectorservice.service.DeviceIdProducer;
import ru.savin.eventscollectorservice.service.HandleEventService;
import ru.savin.eventscollectorservice.util.BaseConfig;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.savin.eventscollectorservice.util.KafkaTestUtils.createConsumer;

@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
@ActiveProfiles("test")
public class DeviceEventConsumerTest extends BaseConfig {

    @Autowired
    private KafkaTemplate<String, DeviceEvent> kafkaTemplate;
    @Autowired
    private HandleEventService handleEventService;


    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary // Заменяем основной бин
        public HandleEventService handleEventService() {
            HandleEventService spy = spy(new HandleEventService(
                    mock(DeviceEventsRepository.class),
                    mock(DeviceIdProducer.class),
                    mock(DeviceIdRepository.class),
                    mock(CustomMetricsConfig.class)
            ));

            doThrow(new RuntimeException("Simulated error"))
                    .when(spy).handleEvents(any(DeviceEvent.class));

            return spy;
        }
    }

    @Test
    void testWhenProcessingFails_thenMessageGoesToDLT() throws ExecutionException, InterruptedException, TimeoutException {
        // Подготавливаем тестовое событие
        DeviceEvent event = DeviceEvent.newBuilder()
                .setEventId("evt_123")
                .setDeviceId("dev_456")
                .setTimestamp(System.currentTimeMillis())
                .setType("temperature_alert")
                .setPayload("{'temp': 99.9}")
                .build();


        // Отправляем событие в исходный топик
        kafkaTemplate.send("events", "key", event).get(5, TimeUnit.SECONDS);

        // 4. Проверяем вызов метода handleEvents
        verify(handleEventService, timeout(10000).atLeastOnce())
                .handleEvents(any(DeviceEvent.class));

        // Подписываемся на DLT-топик
        try (KafkaConsumer<String, DeviceEvent> dltConsumer = createConsumer("events-dlt")) {

            // Ждём появления сообщения в DLT
            ConsumerRecords<String, DeviceEvent> records = dltConsumer.poll(Duration.ofSeconds(5));

            // Проверяем, что это то же событие
            DeviceEvent received = records.iterator().next().value();
            Assertions.assertNotNull(received);
            Assertions.assertEquals(event.getDeviceId(), received.getDeviceId());
        }
    }

}
