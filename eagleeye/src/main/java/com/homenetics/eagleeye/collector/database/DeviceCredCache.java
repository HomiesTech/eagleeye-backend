package com.homenetics.eagleeye.collector.database;

import com.homenetics.eagleeye.entity.DeviceCredEntity;
import com.homenetics.eagleeye.service.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DeviceCredCache {
    private static final ConcurrentHashMap<Integer, String> deviceSsidById =  new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(DeviceCredCache.class);
    private static long refreshCycleCount = 0;

    @Autowired
    private DatabaseService databaseService;

    @Autowired
    private DevicesCache devicesCache;

    @Scheduled(fixedRate = 60000)
    public void refreshCache() {
        long startTime = System.currentTimeMillis();
        refreshCycleCount++;
        logger.info("Starting devices cred cache refresh. Cycle count: {}", refreshCycleCount);
        try {
            List<Integer> devIds = devicesCache.getAllDeviceIds();
            for (Integer devId : devIds) {
                DeviceCredEntity deviceCred = databaseService.getDeviceCredById(devId);
                deviceSsidById.put(devId,deviceCred.getSsid());
            }
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Device Cred cache refreshed successfully. Total Devices: {} loaded in {} ms.", devIds.size(), duration);
        } catch (Exception e) {
            logger.error("Error while refreshing devices cred cache: {}", e.getMessage(), e);
        }
    }

    public String getDeviceSsid(Integer id) {
        return deviceSsidById.get(id);
    }
}
