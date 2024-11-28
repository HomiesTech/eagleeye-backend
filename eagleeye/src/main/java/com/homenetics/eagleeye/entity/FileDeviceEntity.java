package com.homenetics.eagleeye.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private List<DeviceUserEntity> deviceUsers;

    public FileDeviceEntity() {
        this.deviceUsers = new ArrayList<>();
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress.replace(":", "_");
    }

    public String getMacAddress(String macAddress) {
        return macAddress.replace("_", ":");
    }

    public void setDeviceUser(DeviceUserEntity deviceUser) {
        this.deviceUsers.add(deviceUser);
    }
}
