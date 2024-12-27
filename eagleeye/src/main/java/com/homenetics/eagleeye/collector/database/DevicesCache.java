package com.homenetics.eagleeye.collector.database;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.homenetics.eagleeye.entity.APIEntity.DeviceCredEntity;
import com.homenetics.eagleeye.models.DeviceModel;
import com.homenetics.eagleeye.models.DeviceModelWrapper;
import com.homenetics.eagleeye.models.MqttDeviceModel;
import com.homenetics.eagleeye.repository.DeviceRepository;
import com.homenetics.eagleeye.service.DatabaseService;

@Component
public class DevicesCache {

    private static final Logger logger = LoggerFactory.getLogger(DevicesCache.class);
    private static long refreshCycleCount = 0;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private DeviceRepository deviceRepository;

    public void getDBDevices() {
        refreshCycleCount++;
        logger.info("Starting devices cache refresh. Cycle count: {}", refreshCycleCount);
        try {
            Integer pageSize = 100;
            Integer totalPages = 0;
            Integer page = 1;
            do {
                long startTime = System.currentTimeMillis();
                DeviceModelWrapper devices = databaseService.getAllDevices(page, pageSize);

                if (devices == null || devices.getDevices().isEmpty()) {
                    logger.warn("No data returned from the database during cache refresh. Page: {}", page);
                    break;
                }
                totalPages = devices.getTotalPages();

                CompletableFuture.runAsync(() -> {
                    devices.getDevices().forEach(device -> {
                        deviceRepository.upsertDBDevice(
                            device.getDevId(),
                            device.getMacAddress(),
                            device.getSsid(),
                            device.getUserId(),
                            device.getCreatedAt(),
                            device.getUpdatedAt()
                        );
                        try {
                            MqttDeviceModel dbOnlineStatus = this.databaseService.getDeviceOnlineDBStatus(device.getDevId());
                            logger.info("Device ID: {}, Online Status in DB: {}", device.getDevId(), dbOnlineStatus.getStatus());
                            deviceRepository.updateOnlineState(device.getDevId(), dbOnlineStatus.getStatus());
                        } catch (Exception e) {
                            logger.error("Error occurred while fetching device online status: {}", e.getMessage(), e);
                        }

                        try {
                            DeviceCredEntity deviceCred = this.databaseService.getDeviceCredById(device.getDevId());
                            logger.info("Device ID: {}, SSID in DB: {}", device.getDevId(), deviceCred.getSsid());
                            deviceRepository.updateSsid(device.getDevId(), deviceCred.getSsid());
                        } catch (Exception e) {
                            logger.error("Error occurred while fetching device SSID: {}", e.getMessage(), e);
                        }
                    });
                });


                page = page + 1;
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Devices cache refreshed successfully. Total Devices: {}. Time taken: {} ms.",devices.getDevices().size(), duration);
            } while (page <= totalPages);
        } catch (Exception e) {
            logger.error("Error while refreshing devices cache: {}", e.getMessage(), e);
        }
    }

    public DeviceModel getDeviceById(int id) {
        return null;
    }

    public String getMacAddressById(int id) {
        return null;
    }

    public Integer getIdByMacAddress(String macAddress) {
        return null;
    }

    public List<DeviceModel> getAllDevices() {
        return null;
    }

    public List<Integer> getAllDeviceIds() {
        return null;
    }
}
