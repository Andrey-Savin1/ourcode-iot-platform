package ru.savin.eventscollectorservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.savin.eventscollectorservice.DeviceID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceIdProducer {

    @Value("${topic.send-device_id}")
    private String sendClientTopic;

    private final KafkaTemplate<String, DeviceID> kafkaTemplate;

    public void sendDeviceId(DeviceID deviceID) {
        log.info("✅ Sending deviceID: {} to topic {}", deviceID, sendClientTopic);
        kafkaTemplate.send(sendClientTopic, deviceID);
    }

}
