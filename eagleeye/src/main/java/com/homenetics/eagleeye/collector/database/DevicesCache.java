package com.homenetics.eagleeye.collector.database;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.homenetics.eagleeye.models.DeviceModel;
import com.homenetics.eagleeye.models.DeviceModelWrapper;
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
                    });
                });

                // // // // Extracting lists from DeviceModel
                // List<Integer> deviceIds = devices.getDevices().stream().map(DeviceModel::getDevId).collect(Collectors.toList());
                // List<String> macAddresses = devices.getDevices().stream().map(DeviceModel::getMacAddress).collect(Collectors.toList());
                // List<String> ssids = devices.getDevices().stream().map(DeviceModel::getSsid).collect(Collectors.toList());
                // List<Integer> userIds = devices.getDevices().stream().map(DeviceModel::getUserId).collect(Collectors.toList());
                // List<LocalDateTime> createdAts = devices.getDevices().stream().map(DeviceModel::getCreatedAt).collect(Collectors.toList());
                // List<LocalDateTime> updatedAts = devices.getDevices().stream().map(DeviceModel::getUpdatedAt).collect(Collectors.toList());

                // if (deviceIds.size() != macAddresses.size() || 
                //     deviceIds.size() != ssids.size() || 
                //     deviceIds.size() != userIds.size() ||
                //     deviceIds.size() != createdAts.size() || 
                //     deviceIds.size() != updatedAts.size()) {
                //     logger.warn("deviceIds: {}, macAddresses: {}, ssids: {}, userIds: {}, createdAts: {}, updatedAts: {}", 
                //         deviceIds.size(), macAddresses.size(), ssids.size(), userIds.size(), createdAts.size(), updatedAts.size());
                //     logger.error("Mismatch in list sizes for batch upsert. Aborting.");
                //     return;
                // } else {
                //     logger.info("deviceIds: {}, macAddresses: {}, ssids: {}, userIds: {}, createdAts: {}, updatedAts: {}", 
                //         deviceIds.size(), macAddresses.size(), ssids.size(), userIds.size(), createdAts.size(), updatedAts.size());
                // }

                // // // Using CompletableFuture for batch processing
                // // // CompletableFuture<Void> future = 
                // CompletableFuture.runAsync(() -> deviceRepository.batchUpsertDBDevices(deviceIds, macAddresses, ssids, userIds, createdAts, updatedAts));

                // Wait for the batch upsert to complete
                // future.join();
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
