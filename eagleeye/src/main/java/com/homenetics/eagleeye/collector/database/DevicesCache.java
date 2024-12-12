package com.homenetics.eagleeye.collector.database;

import com.homenetics.eagleeye.models.DeviceModel;
import com.homenetics.eagleeye.service.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DevicesCache {
    private static final ConcurrentHashMap<Integer, DeviceModel> devicesCacheById =  new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, String> IdToMac = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> MacToId = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(DevicesCache.class);
    private static long refreshCycleCount = 0;

    @Autowired
    private DatabaseService databaseService;

    @Scheduled(fixedRate = 60000)
    public void refreshCache() {
        long startTime = System.currentTimeMillis();
        refreshCycleCount++;
        logger.info("Starting devices cache refresh. Cycle count: {}", refreshCycleCount);
        try {
            List<DeviceModel> devices = databaseService.getAllDevices();
            if (devices != null) {
                ConcurrentHashMap<Integer, DeviceModel> newCache = new ConcurrentHashMap<>();
                for (DeviceModel device : devices) {
                    IdToMac.put(device.getDevId(), device.getMacAddress());
                    MacToId.put(device.getMacAddress(), device.getDevId());
                    newCache.put(device.getDevId(), device);
                }
                synchronized (devicesCacheById) {
                    devicesCacheById.clear();
                    devicesCacheById.putAll(newCache);
                }
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Device cache refreshed successfully. Total Devices: {} loaded in {} ms.", devices.size(), duration);
            }else {
                logger.warn("No data returned from the database during cache refresh.");
            }
        } catch (Exception e) {
            logger.error("Error while refreshing devices cache: {}", e.getMessage(), e);
        }
    }

    public DeviceModel getDeviceById(int id) {
        return devicesCacheById.get(Integer.valueOf(id));
    }

    public String getMacAddressById(int id) {
        return IdToMac.get(id);
    }

    public Integer getIdByMacAddress(String macAddress) {
        return MacToId.get(macAddress);
    }

    public List<DeviceModel> getAllDevices() {
        return List.copyOf(devicesCacheById.values());
    }

    public List<Integer> getAllDeviceIds() {
        return List.copyOf(devicesCacheById.keySet());
    }
}
