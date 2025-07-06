package ru.savin.devicecollectorservice.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.savin.devicecollectorservice.model.Device;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomDeviceRepository {

    private final JdbcTemplate jdbcTemplate;


    public void batchUpsert(List<Device> devices) {
        String sql = """
            INSERT INTO devices (device_id, device_type, created_at, meta)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (device_id)
            DO UPDATE SET
                device_type = EXCLUDED.device_type,
                created_at = EXCLUDED.created_at,
                meta = EXCLUDED.meta
            """;

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Device device = devices.get(i);
                ps.setString(1, device.getDeviceId());
                ps.setString(2, device.getDeviceType());
                ps.setLong(3, device.getCreatedAt());
                ps.setObject(4, device.getMeta(), Types.VARCHAR);
            }

            @Override
            public int getBatchSize() {
                return devices.size();
            }
        });
    }
}
