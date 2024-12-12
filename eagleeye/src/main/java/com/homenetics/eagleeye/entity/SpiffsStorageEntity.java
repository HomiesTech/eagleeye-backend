package com.homenetics.eagleeye.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class SpiffsStorageEntity {
    private Integer spiffs_used;
    private Integer spiffs_total;
    private LocalDateTime time;
}
