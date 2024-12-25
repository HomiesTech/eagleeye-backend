package com.homenetics.eagleeye.entity.APIEntity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BootTimeDeviceEntity {

    private String macAddress;
    private LocalDateTime bootTime;

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress.replace(":", "_");
    }

    public String getMacAddress(String macAddress) {
        return macAddress.replace("_", ":");
    }
}
