package com.homenetics.eagleeye.merger;

import com.homenetics.eagleeye.collector.database.DevicesCache;
import com.homenetics.eagleeye.collector.device.DevicesCollector;
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
            List<DeviceModel> dbDevices = devicesCache.getAllDevices();

            for (DeviceModel dbdevice : dbDevices) {
                DeviceEntity device = mergedDevices.getOrDefault(dbdevice.getDevId(), new DeviceEntity());
                device.update(dbdevice);
                mergedDevices.put(dbdevice.getDevId(), device);
            }

            List<FileDeviceEntity> fileDevices = devicesCollector.getAllFileDevices();

            for (FileDeviceEntity fileDevice : fileDevices) {
                Integer deviceId = devicesCache.getIdByMacAddress(fileDevice.getMacAddress());
                DeviceEntity device = mergedDevices.getOrDefault(deviceId, null);
                if (device != null) {
                    device.update(fileDevice);
                    mergedDevices.put(deviceId, device);
                }
            }

            long endTime = System.currentTimeMillis(); // Record end time
            long duration = endTime - startTime; // Calculate duration
            logger.info("Combined device cache refresh completed. Total devices: {} in {} ms", mergedDevices.size(), duration);
        } catch (Exception e) {
            logger.error("Error refreshing combined device: {}", e.getMessage(), e);
        }
    }

    public DeviceEntity getDeviceById(Integer id) {
        return mergedDevices.get(id);
    }

    public List<DeviceEntity> getAllDevices() {
        return List.copyOf(mergedDevices.values());
    }

}
