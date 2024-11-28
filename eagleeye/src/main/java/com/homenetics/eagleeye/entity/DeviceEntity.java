package com.homenetics.eagleeye.entity;

import com.homenetics.eagleeye.models.DeviceModel;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class DeviceEntity {
    private static final Integer MAX_SIG_VALUES = 60;
    private static final Integer MIN_ACTIVE_MINUTE = 3;
    private static final Integer MAX_ACTIVE_MINUTE = 5;
    private static final Logger logger = LoggerFactory.getLogger(DeviceEntity.class);

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
    private Integer activeState = 0; // 0,1,2 | 0: InActive, 1: Active, 2: between MIN_ACTIVE_MINUTE and MAX_ACTIVE_MINUTE
    private boolean isOnline;
    private String applianceState;
    private List<WiFiStrengthEntity> wifiSignalStrength;
    private String users;
    private List<DeviceUserEntity> deviceUsers;

    public DeviceEntity() {
        this.wifiSignalStrength = new ArrayList<>();
    }

    private void calculateIsActive() {
        LocalDateTime now = LocalDateTime.now();
        if (syncTime != null ) {
            long minuteDifference = Duration.between(syncTime, now).toMinutes();
            if (minuteDifference >= 0 && minuteDifference < MIN_ACTIVE_MINUTE) {
                this.activeState = 1;
            }else if (minuteDifference >= MIN_ACTIVE_MINUTE && minuteDifference < MAX_ACTIVE_MINUTE) {
                this.activeState = 2;
            }else if (minuteDifference >= MAX_ACTIVE_MINUTE) {
                this.activeState = 0;
            }else {
                this.activeState = 0;
            }
        }else {
            this.activeState = 0;
            logger.warn("SyncTime is null for devId: {}", this.deviceId);
        }
    }

    public void setWifiStrength(Integer wifiSignalStrength) {
        this.wifiSignalStrength.add(new WiFiStrengthEntity(wifiSignalStrength,this.syncTime));
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
        this.deviceUsers = fileDevice.getDeviceUsers();
        this.setWifiStrength(fileDevice.getWifiSignalStrength());
        this.calculateIsActive();
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress.replace(":", "_");
    }

    public String getMacAddress(String macAddress) {
        return macAddress.replace("_", ":");
    }
}
