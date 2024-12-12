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
    private boolean powersave;
    private String username;
    private DeviceUserEntity otaTry;
    private DeviceUserEntity otaOk;
    private DeviceUserEntity credChangeTry;
    private DeviceUserEntity credChangeOk;
    private Integer DownloadMqttUrlResponseCode;
    private Long millis;
    private Integer nvs_used;
    private Integer nvs_free;
    private Integer nvs_total;
    private Integer spiffs_total;
    private Integer spiffs_used;
    private Boolean message_publish_status;
    private Integer boot_time_status_code;

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
