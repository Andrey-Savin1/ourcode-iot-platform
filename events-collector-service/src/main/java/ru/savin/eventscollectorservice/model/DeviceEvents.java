package ru.savin.eventscollectorservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table("device_events_by_device")
public class DeviceEvents {

    @PrimaryKey
    private DeviceEventKey key;
    @Column("event_id")
    private String eventId;
    @Column("type")
    private String type;
    @Column("payload")
    private String payload;

}
