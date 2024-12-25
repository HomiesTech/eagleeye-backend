package com.homenetics.eagleeye.models;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MqttDeviceModel {
    private Integer devId;
    private Boolean status;
    private LocalDateTime updatedAt;
}
