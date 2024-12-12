package com.homenetics.eagleeye.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class DeviceModel {
    private Integer devId;
    private String ssid;
    private String password;
    private String macAddress;
    private Integer userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress.replace(":", "_");
    }

    public String getMacAddress(String macAddress) {
        return macAddress.replace("_", ":");
    }
}
