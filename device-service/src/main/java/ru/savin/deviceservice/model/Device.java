package ru.savin.deviceservice.model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Table(name = "devices")
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Builder
public class Device implements Serializable {

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
