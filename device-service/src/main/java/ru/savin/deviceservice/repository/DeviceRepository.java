package ru.savin.deviceservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.savin.deviceservice.model.Device;


public interface DeviceRepository extends JpaRepository<Device, String> {

}
