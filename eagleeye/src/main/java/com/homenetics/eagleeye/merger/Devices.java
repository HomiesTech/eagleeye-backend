package com.homenetics.eagleeye.merger;

import com.homenetics.eagleeye.collector.database.DevicesCache;
import com.homenetics.eagleeye.collector.device.DevicesCollector;
import com.homenetics.eagleeye.entity.BootTimeDeviceEntity;
import com.homenetics.eagleeye.entity.DeviceEntity;
import com.homenetics.eagleeye.entity.FileDeviceEntity;
import com.homenetics.eagleeye.models.DeviceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class Devices {
    private final ConcurrentHashMap<Integer, DeviceEntity> mergedDevices = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(Devices.class);

    @Autowired
    private DevicesCache devicesCache;

    @Autowired
    private DevicesCollector devicesCollector;

    @Scheduled(fixedRate = 70000) // Every 60 seconds
    public void merge() {
        long startTime = System.currentTimeMillis(); // Record start time
        try {
            logger.info("Starting combined device cache refresh...");
            // Process dbDevices in parallel
            List<DeviceModel> dbDevices = devicesCache.getAllDevices();
            dbDevices.parallelStream().forEach(dbDevice -> {
                DeviceEntity device = mergedDevices.getOrDefault(dbDevice.getDevId(), new DeviceEntity());
                device.update(dbDevice);
                mergedDevices.put(dbDevice.getDevId(), device);
            });

            // Process fileDevices in parallel
            List<FileDeviceEntity> fileDevices = devicesCollector.getAllFileDevices();
            List<BootTimeDeviceEntity> devicesBootTime = devicesCollector.getAllDeviceBootTime();

            fileDevices.parallelStream().forEach(fileDevice -> {
                Integer deviceId = devicesCache.getIdByMacAddress(fileDevice.getMacAddress());
                if (deviceId != null) {
                    DeviceEntity device = mergedDevices.getOrDefault(deviceId, null);
                    if (device != null) {
                        device.update(fileDevice);
                        mergedDevices.put(deviceId, device);
                    }
                }
            });

            devicesBootTime.parallelStream().forEach(deviceBootTime -> {
                Integer deviceId = devicesCache.getIdByMacAddress(deviceBootTime.getMacAddress());
                if (deviceId != null) {
                    DeviceEntity device = mergedDevices.getOrDefault(deviceId, null);
                    if (device != null) {
                        device.update(deviceBootTime);
                        mergedDevices.put(deviceId, device);
                    }
                }
            });

            long endTime = System.currentTimeMillis(); // Record end time
            long duration = endTime - startTime; // Calculate duration
            logger.info("Combined device cache refresh completed. Total devices: {} in {} ms", mergedDevices.size(), duration);
        } catch (Exception e) {
            logger.error("Error refreshing combined device: {}", e.getMessage(), e);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void refreshActiveState() {
        logger.info("Starting refreshActiveState of devices. Total devices: {}", mergedDevices.size());
        try {
            // Parallelize the processing of devices
            mergedDevices.values().parallelStream().forEach(device -> {
                try {
                    device.calculateIsActive();
                } catch (Exception e) {
                    logger.error("Error calculating active state for device {}: {}", device.getDeviceId(), e.getMessage(), e);
                }
            });
            logger.info("Successfully refreshed active state of all devices.");
        } catch (Exception e) {
            logger.error("Error refreshing calculateActiveState of devices: {}", e.getMessage(), e);
        }
    }

    public DeviceEntity getDeviceById(Integer id) {
        return mergedDevices.get(id);
    }

    public List<DeviceEntity> getAllDevices() {
        return List.copyOf(mergedDevices.values());
    }

}
