package ru.savin.deviceservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.savin.deviceservice.dto.DeviceDto;
import ru.savin.deviceservice.mapper.DeviceMapper;
import ru.savin.deviceservice.model.Device;
import ru.savin.deviceservice.repository.DeviceRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Сервис для управления устройствами.
 * Предоставляет функционал создания, получения, обновления и удаления устройств.
 */

@Service
@RequiredArgsConstructor
@Slf4j

public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final DeviceMapper deviceMapper;


    /**
     * Найти устройство по его идентификатору.
     *
     * @param deviceId уникальный идентификатор устройства
     * @return {@link Optional} с устройством, если оно найдено, иначе {@link Optional#empty()}
     * @throws IllegalArgumentException если deviceId равен null или пустой строке
     */
    @Cacheable(cacheNames = "devices", key = "#deviceId", unless = "#result == null ")
    public DeviceDto findDeviceById(String deviceId) {

        var result = deviceRepository.findById(deviceId);
        return result.map(deviceMapper::toDto).orElse(null);
    }

    /**
     * Создать новое устройство.
     *
     * @param device данные нового устройства
     * @return сохранённое устройство с присвоенным идентификатором
     * @throws IllegalArgumentException если device равен null
     */
    // @Cacheable(cacheNames = "devices", key = "#device.deviceId")
    public Device createDevice(DeviceDto device) {
        if (device == null) {
            throw new IllegalArgumentException("Device не может быть null");
        }
        var savedDevise = deviceMapper.map(device);
        savedDevise.setDeviceId(UUID.randomUUID().toString());
        savedDevise.setCreatedAt(System.currentTimeMillis());
        return deviceRepository.save(savedDevise);
    }


    /**
     * Обновить существующее устройство.
     *
     * @param deviceDto обновлённые данные устройства
     * @return обновлённое устройство
     * @throws IllegalArgumentException если device равен null
     */
    @CachePut(cacheNames = "devices", key = "#deviceId")
    public DeviceDto updateDevice(DeviceDto deviceDto, String deviceId) {

        var device = Device.builder()
                .deviceId(deviceId)
                .deviceType(deviceDto.getDeviceType())
                .meta(deviceDto.getMeta())
                .createdAt(System.currentTimeMillis())
                .build();
        return deviceMapper.toDto(deviceRepository.save(device));
    }

    /**
     * Удалить устройство по его идентификатору.
     *
     * @param deviceId уникальный идентификатор устройства
     * @throws IllegalArgumentException если deviceId равен null или пустой строке
     */
    @CacheEvict(cacheNames = "devices", key = "#deviceId")
    public void deleteDeviceById(String deviceId) {
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("Device ID не может быть null или пустым");
        }
        deviceRepository.deleteById(deviceId);
    }

}
