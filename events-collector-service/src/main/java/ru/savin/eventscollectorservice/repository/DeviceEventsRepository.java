package ru.savin.eventscollectorservice.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;

import org.springframework.data.cassandra.repository.Query;
import ru.savin.eventscollectorservice.model.DeviceEvents;

import java.util.List;
import java.util.Optional;

public interface DeviceEventsRepository extends CassandraRepository<DeviceEvents, String> {

    @Query("SELECT * FROM device_events_by_device WHERE device_id = ?0")
    Optional<DeviceEvents> findByDeviceId(String deviceId);


    @Query("SELECT * FROM device_events_by_device WHERE device_id = ?0")
    Optional<List<DeviceEvents>> findAllByDeviceId(String deviceId);

}
