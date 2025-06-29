package ru.savin.eventscollectorservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.cql.Ordering;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@PrimaryKeyClass
public class DeviceEventKey implements Serializable {

    @PrimaryKeyColumn(name = "device_id", ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    private String deviceId;
    @PrimaryKeyColumn(name = "timestamp", ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    private long timestamp;
}