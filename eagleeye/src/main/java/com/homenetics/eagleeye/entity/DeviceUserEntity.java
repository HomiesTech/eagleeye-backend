package com.homenetics.eagleeye.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceUserEntity {
    private String userCode;
    private String userIpAddress;
    private String userFailureCount;
}
