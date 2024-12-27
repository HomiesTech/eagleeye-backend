package com.homenetics.eagleeye.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.homenetics.eagleeye.entity.DBEntity.DeviceDBEntity;
import com.homenetics.eagleeye.repository.DTO.ActiveStateDTO;

public interface DeviceRepository extends JpaRepository<DeviceDBEntity, Integer>, JpaSpecificationExecutor<DeviceDBEntity> {

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM devices WHERE device_id = :deviceId", nativeQuery = true)
    void deleteDeviceById(@Param("deviceId") Integer deviceId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO devices (device_id, mac_address, ssid, user_id, created_at, updated_at) " +
        "VALUES (:deviceId, :macAddress, :ssid, :userId, :createdAt, :updatedAt)" + 
        "ON DUPLICATE KEY UPDATE " +
        "mac_address = VALUES(mac_address), " +
        "ssid = VALUES(ssid), " +
        "user_id = VALUES(user_id), " +
        "created_at = VALUES(created_at), " +
        "updated_at = VALUES(updated_at);",
        nativeQuery = true
    )
    void upsertDBDevice(@Param("deviceId") Integer deviceId, @Param("macAddress") String macAddress, @Param("ssid") String ssid, @Param("userId") Integer userId, @Param("createdAt") LocalDateTime createdAt, @Param("updatedAt") LocalDateTime updatedAt);


    @Modifying
    @Transactional
    @Query(value = "INSERT INTO devices (device_id, mac_address, ssid, user_id, created_at, updated_at) " +
                   "VALUES (:deviceId, :macAddress, :ssid, :userId, :createdAt, :updatedAt) " +
                   "ON DUPLICATE KEY UPDATE " +
                   "mac_address = VALUES(mac_address), " +
                   "ssid = VALUES(ssid), " +
                   "user_id = VALUES(user_id), " +
                   "created_at = VALUES(created_at), " +
                   "updated_at = VALUES(updated_at);",
           nativeQuery = true)
    void batchUpsertDBDevices(@Param("deviceId") List<Integer> deviceIds, 
                            @Param("macAddress") List<String> macAddresses,
                            @Param("ssid") List<String> ssids,
                            @Param("userId") List<Integer> userIds,
                            @Param("createdAt") List<LocalDateTime> createdAts,
                            @Param("updatedAt") List<LocalDateTime> updatedAts);
    
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO devices (device_id, is_online_in_db) VALUES (:deviceId, :is_online_in_db) ON DUPLICATE KEY UPDATE is_online_in_db = VALUES(is_online_in_db)", nativeQuery = true)
    void upsertDBMqttConnection(@Param("deviceId") Integer deviceId, @Param("is_online_in_db") Boolean status);

    @Modifying
    @Transactional
    @Query(value = "UPDATE devices SET boot_time = :bootTime WHERE mac_address = :macAddress", nativeQuery = true)
    void updateBootTimeDevice(@Param("macAddress") String macAddress, @Param("bootTime") LocalDateTime bootTime);

    @Modifying
    @Transactional
    @Query(value = "UPDATE devices SET " +
                   "device_name = :deviceName, " +
                   "ip_address = :ipAddress, " +
                   "code_version = :codeVersion, " +
                   "sync_time = :syncTime, " +
                   "is_online = :isOnline, " +
                   "appliance_state = :applianceState, " +
                   "powersave = :powersave, " +
                   "username = :username, " +
                   "ota_try_code = :otaTryCode, " +
                   "ota_ok_code = :otaOkCode, " +
                   "cred_change_try_code = :credChangeTryCode, " +
                   "cred_change_ok_code = :credChangeOkCode, " +
                   "wifi_signal_strength = :wifiSignalStrength, " +
                   "nvs_used = :nvsUsed, " +
                   "nvs_free = :nvsFree, " +
                   "nvs_total = :nvsTotal, " +
                   "spiffs_used = :spiffsUsed, " +
                   "spiffs_total = :spiffsTotal, " +
                   "download_mqtt_url_response_code = :downloadMqttUrlResponseCode, " +
                   "millis = :millis, " +
                   "message_publish_status = :message_publish_status, " +
                   "users = :users, " +
                   "data_updated_at = :dataUpdatedAt " +
                   "WHERE mac_address = :macAddress", nativeQuery = true)   
    void updateFileDevice(@Param("macAddress") String macAddress,
                            @Param("deviceName") String deviceName,
                            @Param("ipAddress") String ipAddress,
                            @Param("codeVersion") String codeVersion,
                            @Param("syncTime") LocalDateTime syncTime,
                            @Param("isOnline") Boolean isOnline,
                            @Param("applianceState") String applianceState,
                            @Param("powersave") Boolean powersave,
                            @Param("username") String username,
                            @Param("otaTryCode") String otaTryCode,
                            @Param("otaOkCode") String otaOkCode,
                            @Param("credChangeTryCode") String credChangeTryCode,
                            @Param("credChangeOkCode") String credChangeOkCode,
                            @Param("wifiSignalStrength") Integer wifiSignalStrength,
                            @Param("nvsUsed") Integer nvsUsed,
                            @Param("nvsFree") Integer nvsFree,
                            @Param("nvsTotal") Integer nvsTotal,
                            @Param("spiffsUsed") Integer spiffsUsed,
                            @Param("spiffsTotal") Integer spiffsTotal,
                            @Param("downloadMqttUrlResponseCode") Integer downloadMqttUrlResponseCode,
                            @Param("millis") Long millis,
                            @Param("message_publish_status") Integer message_publish_status,
                            @Param("users") String users,
                            @Param("dataUpdatedAt") LocalDateTime dataUpdatedAt);

    @Modifying
    @Transactional
    @Query(value = "UPDATE devices SET active_state = :activeState WHERE device_id = :deviceId", nativeQuery = true)
    void updateActiveState(@Param("deviceId") Integer deviceId, @Param("activeState") Integer activeState);
    

    @Query("SELECT new com.homenetics.eagleeye.repository.DTO.ActiveStateDTO(d.deviceId, d.syncTime) FROM DeviceDBEntity d")
    Page<ActiveStateDTO> findActiveDevices(Pageable pageable);
    
}