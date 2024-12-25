package com.homenetics.eagleeye.repository.DTO;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class ActiveStateDTO {
    private Integer deviceId;
    private LocalDateTime syncTime;
    private Integer activeState;

    public ActiveStateDTO(Integer deviceId, LocalDateTime syncTime) {
        this.deviceId = deviceId;
        this.syncTime = syncTime;
    }
}
