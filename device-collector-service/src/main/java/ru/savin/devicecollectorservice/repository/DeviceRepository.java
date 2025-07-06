package ru.savin.devicecollectorservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.savin.devicecollectorservice.model.Device;

public interface DeviceRepository extends JpaRepository<Device, Long>   {


    @Modifying
    @Query(value = """
        INSERT INTO devices (device_id, device_type, created_at, meta)
        VALUES (:deviceId, :deviceType, :createdAt, :meta)
        ON CONFLICT (device_id)
        DO UPDATE SET
            device_type = EXCLUDED.device_type,
            created_at = EXCLUDED.created_at,
            meta = EXCLUDED.meta
    """, nativeQuery = true)
    void upsertDevice(
            @Param("deviceId") String deviceId,
            @Param("deviceType") String deviceType,
            @Param("createdAt") Long createdAt,
            @Param("meta") String meta
    );
}
