package com.homenetics.eagleeye.entity.APIEntity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WiFiStrengthEntity {
    private Integer strength;
    private LocalDateTime time;

    WiFiStrengthEntity(Integer strength, LocalDateTime time) {
        this.strength = strength;
        this.time = time;
    }
}
