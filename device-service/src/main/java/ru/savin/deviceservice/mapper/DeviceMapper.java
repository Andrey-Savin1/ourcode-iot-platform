package ru.savin.deviceservice.mapper;

import org.mapstruct.Mapper;
import ru.savin.deviceservice.dto.DeviceDto;
import ru.savin.deviceservice.model.Device;


@Mapper(componentModel = "spring")
public interface DeviceMapper {


    DeviceDto toDto(Device device);

    Device map(DeviceDto device);

}
