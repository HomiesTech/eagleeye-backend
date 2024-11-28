package com.homenetics.eagleeye.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FileDeviceEntity {
    private String deviceName;
    private String macAddress;
    private Integer wifiSignalStrength;
    private String ipAddress;
    private boolean isOnline;
    private String users;
    private LocalDateTime syncTime;
    private String codeVersion;
    private String applianceState;

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress.replace(":", "_");
    }

    public String getMacAddress(String macAddress) {
        return macAddress.replace("_", ":");
    }
}
