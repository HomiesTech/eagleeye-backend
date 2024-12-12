package com.homenetics.eagleeye.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NVSStorageEntity {
    private Integer nvs_used;
    private Integer nvs_free;
    private Integer nvs_total;
    private LocalDateTime time;
}
