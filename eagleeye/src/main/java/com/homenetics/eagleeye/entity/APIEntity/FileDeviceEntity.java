package com.homenetics.eagleeye.entity.APIEntity;

import lombok.Data;

import java.time.LocalDateTime;
// import java.util.ArrayList;
// import java.util.List;

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
    // private List<DeviceUserEntity> deviceUsers;
    private boolean powersave;
    private String username;
    private String otaTry;
    private String otaOk;
    private String credChangeTry;
    private String credChangeOk;
    // private DeviceUserEntity otaTry;
    // private DeviceUserEntity otaOk;
    // private DeviceUserEntity credChangeTry;
    // private DeviceUserEntity credChangeOk;
    private Integer DownloadMqttUrlResponseCode;
    private Long millis;
    private Integer nvs_used;
    private Integer nvs_free;
    private Integer nvs_total;
    private Integer spiffs_total;
    private Integer spiffs_used;
    private Integer message_publish_status;
    private Integer message_publish_status_fail_count;
    private Integer boot_time_status_code;

    public FileDeviceEntity() {
        // this.deviceUsers = new ArrayList<>();
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress.replace(":", "_");
    }

    public String getMacAddress(String macAddress) {
        return macAddress.replace("_", ":");
    }

    // public void setDeviceUser(DeviceUserEntity deviceUser) {
    //     this.deviceUsers.add(deviceUser);
    // }
}
