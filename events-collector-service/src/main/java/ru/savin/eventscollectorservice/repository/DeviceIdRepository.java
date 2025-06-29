package ru.savin.eventscollectorservice.repository;

import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.savin.eventscollectorservice.model.DeviceId;

public interface DeviceIdRepository extends CassandraRepository<DeviceId, String> {

    @Query("INSERT INTO device_ids (device_id) VALUES (:deviceId) IF NOT EXISTS")
    ResultSet insertIfNotExists(@Param("deviceId") String deviceId);

}
