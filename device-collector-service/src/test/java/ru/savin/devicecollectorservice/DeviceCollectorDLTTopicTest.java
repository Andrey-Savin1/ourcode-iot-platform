package ru.savin.devicecollectorservice;

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
import ru.savin.devicecollectorservice.mapper.DeviceMapper;
import ru.savin.devicecollectorservice.repository.DeviceRepository;
import ru.savin.devicecollectorservice.service.HandleEventService;
import ru.savin.devicecollectorservice.util.BaseConfig;
import ru.savin.eventscollectorservice.KafkaDevice;


import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static ru.savin.devicecollectorservice.util.KafkaTestUtils.createConsumer;


@SpringBootTest
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Slf4j
@ActiveProfiles("test")
public class DeviceCollectorDLTTopicTest extends BaseConfig {

    @Autowired
    private KafkaTemplate<String, KafkaDevice> kafkaTemplate;
    @Autowired
    private HandleEventService handleEventService;


    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary // Заменяем основной бин
        public HandleEventService handleEventService() {
            HandleEventService spy = spy(new HandleEventService(
                    mock(DeviceMapper.class),
                    mock(DeviceRepository.class)
            ));

            doThrow(new RuntimeException("Simulated error"))
                    .when(spy).handleEvents(any(KafkaDevice.class));
            return spy;
        }
    }

    @Test
    void testWhenProcessingFails_thenMessageGoesToDLT() throws ExecutionException, InterruptedException, TimeoutException {
        // Подготавливаем тестовое событие
        KafkaDevice testData = KafkaDevice.newBuilder()
                .setDeviceType("1")
                .setDeviceId("1")
                .setCreatedAt(System.currentTimeMillis())
                .setMeta("1")
                .build();

        // Отправляем событие в исходный топик
        kafkaTemplate.send("device-id-topic", "key", testData).get(5, TimeUnit.SECONDS);

        // 4. Проверяем вызов метода handleEvents
        verify(handleEventService, timeout(10000).atLeastOnce())
                .handleEvents(any(KafkaDevice.class));

        // Подписываемся на DLT-топик
        try (KafkaConsumer<String, KafkaDevice> dltConsumer = createConsumer("device-id-topic-dlt")) {

            // Ждём появления сообщения в DLT
            ConsumerRecords<String, KafkaDevice> records = dltConsumer.poll(Duration.ofSeconds(5));

            // Проверяем, что это то же событие
            KafkaDevice received = records.iterator().next().value();
            Assertions.assertNotNull(received);
            Assertions.assertEquals(testData.getDeviceId(), received.getDeviceId());
        }
    }

}
