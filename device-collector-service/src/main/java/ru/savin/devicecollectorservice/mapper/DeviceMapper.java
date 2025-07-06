package ru.savin.devicecollectorservice.mapper;

import org.mapstruct.Mapper;
import ru.savin.devicecollectorservice.model.Device;
import ru.savin.eventscollectorservice.KafkaDevice;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    Device map(KafkaDevice kafkaDevice);

}
