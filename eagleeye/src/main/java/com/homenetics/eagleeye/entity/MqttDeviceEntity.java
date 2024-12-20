package com.homenetics.eagleeye.entity;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MqttDeviceEntity {
    private Integer devId;
    private Boolean status;
    private LocalDateTime updatedAt;
}
