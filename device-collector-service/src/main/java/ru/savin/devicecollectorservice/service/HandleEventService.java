package ru.savin.devicecollectorservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.savin.devicecollectorservice.mapper.DeviceMapper;
import ru.savin.devicecollectorservice.repository.DeviceRepository;
import ru.savin.eventscollectorservice.KafkaDevice;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
@RequiredArgsConstructor
@Slf4j
public class HandleEventService {

    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private final DeviceMapper mapper;
    private final DeviceRepository deviceRepository;

    /**
     * Обрабатывает входящее событие устройства.
     * <p>
     * Проверяет данные на корректность, определяет шард и добавляет событие в соответствующую очередь.
     */
    @Transactional
    public void handleEvents(KafkaDevice device) {
        virtualThreadExecutor.execute(() -> {
            log.info("Start virtual thread: {}", Thread.currentThread().threadId());

            if (validateDevice(device)) {
                log.info("Saving deviceID: {}", device.getDeviceId());
                deviceRepository.save(mapper.map(device));
            }
        });
    }

    /**
     * Проверяет корректность данных устройства.
     *
     * @param device объект устройства
     * @return true, если данные валидны
     */
    private boolean validateDevice(KafkaDevice device) {
        if (device == null) return false;
        return StringUtils.isNotBlank(device.getDeviceId()) &&
                StringUtils.isNotBlank(device.getDeviceType()) &&
                device.getCreatedAt() >= 0 &&
                StringUtils.isNotBlank(device.getMeta());
    }

}
