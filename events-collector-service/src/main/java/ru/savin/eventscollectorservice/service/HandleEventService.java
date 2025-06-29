package ru.savin.eventscollectorservice.service;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ru.savin.eventscollectorservice.DeviceEvent;
import ru.savin.eventscollectorservice.DeviceID;
import ru.savin.eventscollectorservice.config.CustomMetricsConfig;
import ru.savin.eventscollectorservice.model.DeviceEventKey;
import ru.savin.eventscollectorservice.model.DeviceEvents;
import ru.savin.eventscollectorservice.repository.DeviceEventsRepository;
import ru.savin.eventscollectorservice.repository.DeviceIdRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HandleEventService {

    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final LinkedBlockingQueue<DeviceEvent> eventQueue = new LinkedBlockingQueue<>();

    private final DeviceEventsRepository deviceEventsRepository;
    private final DeviceIdProducer deviceIdProducer;
    private final DeviceIdRepository deviceIdRepository;
    private final CustomMetricsConfig metrics;

    @PostConstruct
    public void init() {
        // Запускаем обработчик батчей в отдельном виртуальном потоке
        virtualThreadExecutor.execute(this::processBatch);
    }

    public void handleEvents(DeviceEvent event) {
        virtualThreadExecutor.execute(() -> {
            log.info("Start virtual thread: {}", Thread.currentThread().threadId());

            if (validateDevice(event)) {
                // 1. Отправляем событие в батч (основная таблица)
                eventQueue.offer(event);
                // 2. Синхронно проверяем/сохраняем device_id
                log.debug("DeviceId: {}", event.getDeviceId());
                ResultSet isNewDevice = deviceIdRepository.insertIfNotExists(event.getDeviceId());

                // 3. Отправляем в Kafka только если device_id новый
                if (!isNewDevice.wasApplied()) {
                    deviceIdProducer.sendDeviceId(DeviceID.newBuilder()
                            .setDeviceId(event.getDeviceId())
                            .build());
                }
            }
        });
    }

    private void processBatch() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                // Размер батча
                int BATCH_SIZE = 20;
                //временный список для накопления событий перед записью в Cassandra.
                List<DeviceEvent> batch = new ArrayList<>(BATCH_SIZE);
                // Таймаут батча в миллисекундах
                long BATCH_TIMEOUT_MS = 1000;

                //Ждём пока в очереди не появится хотя бы одно событие.
                //Это гарантирует, что даже при низкой нагрузке события не будут "застревать" в очереди надолго.
                DeviceEvent firstEvent = eventQueue.poll(BATCH_TIMEOUT_MS, TimeUnit.MILLISECONDS);

                if (firstEvent != null) {
                    batch.add(firstEvent);
                    eventQueue.drainTo(batch, BATCH_SIZE - 1);

                    if (!batch.isEmpty()) {
                        saveBatchToCassandra(batch);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Batch processor is shutting down...");
            } catch (Exception e) {
                log.error("Unexpected error in batch processor", e);
            }
        }
    }

    public void saveBatchToCassandra(List<DeviceEvent> batch) {
        try {
            List<DeviceEvents> entities = batch.stream()
                    .map(device -> DeviceEvents.builder()
                            .key(DeviceEventKey.builder()
                                    .deviceId(device.getDeviceId())
                                    .timestamp(device.getTimestamp())
                                    .build())
                            .eventId(device.getEventId())
                            .type(device.getType())
                            .payload(device.getPayload())
                            .build())
                    .collect(Collectors.toList());
            //метрики успешного сохранения
            metrics.incrementSuccessSaveToCassandra();
            deviceEventsRepository.saveAll(entities);
            log.info("✅ Successfully saved batch of {} events to Cassandra", batch.size());
        } catch (Exception e) {
            //метрики неуспешного сохранения
            metrics.incrementFailedSaveToCassandra();
            log.error("Failed to save batch to Cassandra", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        virtualThreadExecutor.shutdown();
        try {
            if (!virtualThreadExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                virtualThreadExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            virtualThreadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Обработка оставшихся событий перед завершением
        if (!eventQueue.isEmpty()) {
            List<DeviceEvent> remainingEvents = new ArrayList<>();
            eventQueue.drainTo(remainingEvents);
            if (!remainingEvents.isEmpty()) {
                saveBatchToCassandra(remainingEvents);
            }
        }
    }

    private boolean validateDevice(DeviceEvent device) {
        if (device == null) return false;
        return StringUtils.isNotBlank(device.getEventId()) &&
                StringUtils.isNotBlank(device.getDeviceId()) &&
                device.getTimestamp() >= 0 &&
                StringUtils.isNotBlank(device.getType()) &&
                StringUtils.isNotBlank(device.getPayload());

    }

}
