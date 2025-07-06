package ru.savin.devicecollectorservice.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Table(name = "devices")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Builder
public class Device {

    @Id
    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "device_type")
    private String deviceType;

    private Long createdAt;

    @Column(columnDefinition = "jsonb")
    @Basic(fetch = FetchType.LAZY)
    private String meta;

}
