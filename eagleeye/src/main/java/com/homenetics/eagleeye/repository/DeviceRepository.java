package com.homenetics.eagleeye.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import com.homenetics.eagleeye.entity.DeviceDBEntity;

public interface DeviceRepository extends JpaRepository<DeviceDBEntity, Integer> {

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
}