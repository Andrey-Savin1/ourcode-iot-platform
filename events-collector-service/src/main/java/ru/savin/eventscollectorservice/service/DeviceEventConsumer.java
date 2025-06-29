package ru.savin.eventscollectorservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import ru.savin.eventscollectorservice.DeviceEvent;


@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceEventConsumer {

    private final HandleEventService handleEventService;

    @RetryableTopic(
            attempts = "3",                      // Количество попыток
            backoff = @Backoff(delay = 1000, multiplier = 2.0), // Экспоненциальная задержка: 1s, 2s, 4s
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE
    )
    @KafkaListener(topics = "${topic.event-listener}", groupId = "events-collector-group")
    public void listen(DeviceEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                       @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                       @Header(KafkaHeaders.OFFSET) long offset) {
        try {
            log.info("Received Avro message. Topic: {}, Partition: {}, Offset: {}, Data: {}",
                    topic, partition, offset, event);
            handleEventService.handleEvents(event);
        } catch (Exception e) {
            log.error("Ошибка обработки сообщения: {}", e.getMessage());
            throw e;
        }
    }

    // Обработчик для Dead Letter Topic (DLT)
    @DltHandler
    public void handleDlt(DeviceEvent event,
                          @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                          @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
                          @Header(KafkaHeaders.OFFSET) long offset) {

        log.error("Сообщение ушло в DLT. Топик: {}, Partition: {}, Offset: {}, Данные: {}",
                topic, partition, offset, event);
        // Сохранять в отдельную таблицу?
    }
}
