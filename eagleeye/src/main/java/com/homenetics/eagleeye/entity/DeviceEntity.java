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
    private static final Integer MAX_NVS_VALUES = 60;
    private static final Integer MAX_SPIFFS_VALUES = 60;
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
    private LocalDateTime bootTime;
    private Integer activeState = 0; // 0,1,2 | 0: InActive, 1: Active, 2: between MIN_ACTIVE_MINUTE and MAX_ACTIVE_MINUTE
    private boolean isOnline;
    private String applianceState;
    private boolean powersave;
    private String username;
    private DeviceUserEntity otaTry;
    private DeviceUserEntity otaOk;
    private DeviceUserEntity credChangeTry;
    private DeviceUserEntity credChangeOk;
    private List<WiFiStrengthEntity> wifiSignalStrength;
    private List<NVSStorageEntity> nvsStorage;
    private List<SpiffsStorageEntity> spiffsStorage;
    private Integer DownloadMqttUrlResponseCode;
    private Long millis;
    private Integer message_publish_status;
    private Integer boot_status_code;
    private boolean isOnlineInDb;
    private LocalDateTime onlineTimeInDb;
    /**
     * 1 - Excellent
     * 2 - Very good
     * 3 - Good
     * 4 - Fair
     * 5 - Poor
     * 6 - Very Poor
     **/
    private Integer signalStrength;
    private String users;
    private List<DeviceUserEntity> deviceUsers;

    public DeviceEntity() {
        this.wifiSignalStrength = new ArrayList<>();
        this.nvsStorage = new ArrayList<>();
        this.spiffsStorage = new ArrayList<>();
    }

    public void calculateIsActive() {
        LocalDateTime now = LocalDateTime.now();
        logger.info("Calculating active state for device ID: {} at time: {}", this.deviceId, now);
        if (syncTime != null ) {
            logger.info("SyncTime for device ID {}: {}", this.deviceId, syncTime);
            long minuteDifference = Duration.between(syncTime, now).toMinutes();
            logger.info("Time difference in minutes for device ID {}: {}", this.deviceId, minuteDifference);
            if (minuteDifference >= 0 && minuteDifference < MIN_ACTIVE_MINUTE) {
                this.activeState = 1;
                logger.info("Device ID {} is set to ACTIVE (state: 1)", this.deviceId);
            }else if (minuteDifference >= MIN_ACTIVE_MINUTE && minuteDifference < MAX_ACTIVE_MINUTE) {
                this.activeState = 2;
                logger.info("Device ID {} is set to WARN (state: 2)", this.deviceId);
            }else if (minuteDifference >= MAX_ACTIVE_MINUTE) {
                this.activeState = 0;
                logger.info("Device ID {} is set to OFFLINE (state: 0)", this.deviceId);
            }else {
                this.activeState = 0;
                logger.warn("Unexpected condition for device ID {}: minuteDifference is {}", this.deviceId, minuteDifference);
            }
        }else {
            this.activeState = 0;
            logger.warn("SyncTime is null for devId: {}", this.deviceId);
        }
    }

    private void caluclateSignalStrength(Integer wss) {
        Integer signalState = 6;
        if (wss != null) {
            if (wss >= -30) { signalState = 1; }
            else if (wss >= -50) { signalState = 2; }
            else if (wss >= -60) { signalState = 3; }
            else if (wss >= -70) { signalState = 4; }
            else if (wss >= -80) { signalState = 5; }
            else { signalState = 6;  }
        }
        this.setSignalStrength(signalState);
    }

    public void setWifiStrength(Integer wifiSignalStrength) {
        this.caluclateSignalStrength(wifiSignalStrength);
        this.wifiSignalStrength.add(new WiFiStrengthEntity(wifiSignalStrength,this.syncTime));
        if (this.wifiSignalStrength.size() > MAX_SIG_VALUES) {
            this.wifiSignalStrength.remove(0);
        }
    }

    public void setNvsStorage(Integer nvs_used, Integer nvs_free, Integer nvs_total) {
        NVSStorageEntity nvsStorage = new NVSStorageEntity(nvs_used, nvs_free, nvs_total, this.syncTime);
        this.nvsStorage.add(nvsStorage);
        if (this.nvsStorage.size() > MAX_NVS_VALUES) {
            this.nvsStorage.remove(0);
        }
    }

    public void setSpiffsStorage(Integer spiffs_used, Integer spiffs_total) {
        logger.info("Setting Spiffs Storage: {} {}", spiffs_used, spiffs_total);
        SpiffsStorageEntity spiffsStorage = new SpiffsStorageEntity(spiffs_used, spiffs_total, this.syncTime);
        this.spiffsStorage.add(spiffsStorage);
        if (this.spiffsStorage.size() > MAX_SPIFFS_VALUES) {
            this.spiffsStorage.remove(0);
        }
    }

    public void update(DeviceModel deviceModel) {
        this.deviceId = deviceModel.getDevId();
        this.macAddress = deviceModel.getMacAddress();
        this.createdAt = deviceModel.getCreatedAt();
        this.updatedAt = deviceModel.getUpdatedAt();
        this.userId = deviceModel.getUserId();
        this.password = "********";
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
        this.username = fileDevice.getUsername();
        this.credChangeOk = fileDevice.getCredChangeOk();
        this.credChangeTry = fileDevice.getCredChangeTry();
        this.otaOk = fileDevice.getOtaOk();
        this.otaTry = fileDevice.getOtaTry();
        this.millis = fileDevice.getMillis();
        this.DownloadMqttUrlResponseCode = fileDevice.getDownloadMqttUrlResponseCode();
        this.powersave = fileDevice.isPowersave();
        this.boot_status_code = fileDevice.getBoot_time_status_code();
        this.message_publish_status = fileDevice.getMessage_publish_status();


        this.setNvsStorage(fileDevice.getNvs_used(), fileDevice.getNvs_free(), fileDevice.getNvs_total());
        this.setSpiffsStorage(fileDevice.getSpiffs_used(), fileDevice.getSpiffs_total());
        this.setWifiStrength(fileDevice.getWifiSignalStrength());
        this.calculateIsActive();
    }

    public void update(BootTimeDeviceEntity bootTimeDevice) {
        this.bootTime = bootTimeDevice.getBootTime();
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress.replace(":", "_");
    }

    public String getMacAddress(String macAddress) {
        return macAddress.replace("_", ":");
    }
}
