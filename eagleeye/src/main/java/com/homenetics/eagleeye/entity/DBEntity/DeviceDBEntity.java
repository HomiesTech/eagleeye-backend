package com.homenetics.eagleeye.entity.DBEntity;


import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name="devices")
@Data
public class DeviceDBEntity {
    @Id
    @Column
    private Integer deviceId;

    @Column
    private String deviceName;
    
    @Column
    private String ssid;

    @Column
    private String password;

    @Column
    private String macAddress;

    @Column
    private String ipAddress;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private Integer userId;

    @Column
    private String codeVersion;

    @Column
    private LocalDateTime syncTime;

    @Column
    private LocalDateTime bootTime;

    @Column(columnDefinition = "INTEGER DEFAULT 0")
    private Integer activeState = 0; // 0,1,2 | 0: InActive, 1: Active, 2: between MIN_ACTIVE_MINUTE and MAX_ACTIVE_MINUTE
    
    @Column(columnDefinition = "BIT DEFAULT 0")
    private boolean isOnline = false;

    @Column
    private String applianceState;

    @Column(columnDefinition = "BIT DEFAULT 0")
    private boolean powersave;

    @Column
    private String username;

    @Column
    private String otaTryCode;

    @Column
    private String otaOkCode;

    @Column
    private String credChangeTryCode;

    @Column
    private String credChangeOkCode;

    @Column
    private Integer wifiSignalStrength;

    @Column
    private Integer nvs_used;

    @Column
    private Integer nvs_free;

    @Column
    private Integer nvs_total;

    @Column
    private Integer spiffs_used;

    @Column
    private Integer spiffs_total;

    @Column
    private Integer DownloadMqttUrlResponseCode;

    @Column
    private Long millis;

    @Column
    private Integer message_publish_status;

    @Column
    private Integer boot_status_code;

    @Column(columnDefinition = "BIT DEFAULT 0")
    private boolean isOnlineInDb;

    @Column
    private LocalDateTime onlineTimeInDb;

    @Column
    private Integer signalStrength;

    @Column
    private String users;

    @Column
    private LocalDateTime dataCreatedAt;

    @Column
    private LocalDateTime dataUpdatedAt;
}
