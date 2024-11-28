package com.homenetics.eagleeye.entity;

import com.homenetics.eagleeye.models.DeviceModel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DeviceEntity {
    private static final Integer MAX_SIG_VALUES = 60;
    private Integer deviceId;
    private String deviceName;
    private String ssid;
    private String password;
    private String macAddress;
    private String ipAddress;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer userId;
    private String codeVersion;
    private LocalDateTime syncTime;
    private boolean isOnline;
    private String applianceState;
    private List<WiFiStrengthEntity> wifiSignalStrength;
    private String users;

    public DeviceEntity() {
        this.wifiSignalStrength = new ArrayList<>();
    }

    public void setWifiStrength(Integer wifiSignalStrength) {
        LocalDateTime now = LocalDateTime.now();
        this.wifiSignalStrength.add(new WiFiStrengthEntity(wifiSignalStrength,now));
        if (this.wifiSignalStrength.size() > MAX_SIG_VALUES) {
            this.wifiSignalStrength.remove(0);
        }
    }

    public void update(DeviceModel deviceModel) {
        this.deviceId = deviceModel.getDevId();
        this.macAddress = deviceModel.getMacAddress();
        this.createdAt = deviceModel.getCreatedAt();
        this.updatedAt = deviceModel.getUpdatedAt();
        this.userId = deviceModel.getUserId();
    }

    public void update(FileDeviceEntity fileDevice) {
        this.ipAddress = fileDevice.getIpAddress();
        this.applianceState = fileDevice.getApplianceState();
        this.codeVersion = fileDevice.getCodeVersion();
        this.deviceName = fileDevice.getDeviceName();
        this.syncTime = fileDevice.getSyncTime();
        this.isOnline = fileDevice.isOnline();
        this.users = fileDevice.getUsers();
        this.setWifiStrength(fileDevice.getWifiSignalStrength());
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress.replace(":", "_");
    }

    public String getMacAddress(String macAddress) {
        return macAddress.replace("_", ":");
    }
}
