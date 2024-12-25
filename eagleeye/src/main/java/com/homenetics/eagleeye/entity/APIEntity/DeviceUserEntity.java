package com.homenetics.eagleeye.entity.APIEntity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceUserEntity {
    private Integer customerId;
    private String name;
    private String userCode;
    private String userIpAddress;
    private String userFailureCount;
}
