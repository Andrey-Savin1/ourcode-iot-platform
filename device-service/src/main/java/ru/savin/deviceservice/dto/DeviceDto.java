package ru.savin.deviceservice.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DeviceDto implements Serializable {

    private String deviceId;
    private String deviceType;
    private Long createdAt;
    private String meta;

}
