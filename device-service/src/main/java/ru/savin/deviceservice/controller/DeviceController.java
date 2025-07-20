package ru.savin.deviceservice.controller;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.savin.deviceservice.dto.DeviceDto;
import ru.savin.deviceservice.model.Device;
import ru.savin.deviceservice.repository.DeviceRepository;
import ru.savin.deviceservice.service.DeviceService;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("api/v1/devices")
public class DeviceController {


    private final DeviceService deviceService;
    private final DeviceRepository deviceRepository;


    @GetMapping("/{id}")
    @RolesAllowed({"devise_service_admin", "device_service_read"})
    public ResponseEntity<DeviceDto> getDevice(@PathVariable String id) {
        log.info("Запрос устройства с ID: {}", id);
        return ResponseEntity.ok(deviceService.findDeviceById(id));
    }

    @PostMapping
    @RolesAllowed("devise_service_admin")
    public ResponseEntity<Device> createDevice(@RequestBody DeviceDto device) {
        Device created = deviceService.createDevice(device);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/{id}")
    @RolesAllowed("devise_service_admin")
    public ResponseEntity<Void> deleteDevice(@PathVariable String id) {
        log.info("Удаление устройства с ID: {}", id);
        deviceService.deleteDeviceById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @RolesAllowed("devise_service_admin")
    public ResponseEntity<DeviceDto> updateDevice(@RequestBody DeviceDto device, @PathVariable String id) {

        if (StringUtils.isBlank(id) || !id.equals(device.getDeviceId())) {
            return ResponseEntity.badRequest().build();
        }
        if (!deviceRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        log.info("Обновление устройства с ID: {}", device.getDeviceId());
        return ResponseEntity.ok(deviceService.updateDevice(device, id));
    }

}
