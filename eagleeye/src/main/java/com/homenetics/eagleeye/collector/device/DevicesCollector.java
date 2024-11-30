package com.homenetics.eagleeye.collector.device;

import com.homenetics.eagleeye.collector.database.CustomersCache;
import com.homenetics.eagleeye.collector.database.DevicesCache;
import com.homenetics.eagleeye.entity.DeviceUserEntity;
import com.homenetics.eagleeye.entity.FileDeviceEntity;
import com.homenetics.eagleeye.models.CustomerModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DevicesCollector {
    private final List<FileDeviceEntity> fileDevices = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(DevicesCollector.class);

    @Autowired
    private CustomersCache customers;

//    private static final String DeviceDataPath = "D:\\DATA\\data";
    private static final String DeviceDataPath = "/var/homenetics/devices/data";
    public synchronized List<FileDeviceEntity> getAllFileDevices() {
        return new ArrayList<>(fileDevices);
    }

    private synchronized void updateFileDevices(List<FileDeviceEntity> newDevices) {
        fileDevices.clear();
        fileDevices.addAll(newDevices);
    }

    @Scheduled(fixedRate = 65000) // Every 65 seconds
    public void collector() {
        fileDevices.clear(); // Clear old buffer before collecting new file devices.
        try {
            String deviceDataPath = DeviceDataPath;
            File baseFolder = new File(deviceDataPath);

            if (!baseFolder.exists() || !baseFolder.isDirectory()) {
                logger.warn("Base Folder does not exist or is not a directory: {}", deviceDataPath);
                return;
            }

            LocalDateTime now = LocalDateTime.now();
            String todayDate = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String todayDateP2 = now.format(DateTimeFormatter.ofPattern("ddMMyyyy"));
            int currentHour = now.getHour();
            int targetMinute = now.getMinute() - 1;

            if (targetMinute < 0) {
                // Handle the case where minute goes negative (e.g., 00 -> 59 and adjust the hour)
                targetMinute = 59;
                currentHour = (currentHour - 1 + 24) % 24; // Adjust the hour if necessary
            }

            String timePattern = String.format("_%02d%02d", currentHour, targetMinute);
            List<FileDeviceEntity> newDevices = Collections.synchronizedList(new ArrayList<>());

            // Process each folder in parallel
            Arrays.stream(Objects.requireNonNull(baseFolder.listFiles(File::isDirectory)))
                    .parallel()
                    .forEach(macFolder -> {
                        logger.info("Processing folder: {}", macFolder.getAbsolutePath());
                        File currentDateFolder = new File(macFolder, todayDate);

                        if (!currentDateFolder.exists() || !currentDateFolder.isDirectory()) {
                            logger.warn("No folder found for today's date: {}", todayDate);
                            logger.warn("Current Folder does not exist or is not a directory: {}", currentDateFolder.getAbsolutePath());
                            return;
                        }

                        String generatedFilename = todayDateP2 + timePattern + ".txt";
                        File deviceFile = new File(currentDateFolder, generatedFilename);

                        if (deviceFile.exists() && deviceFile.isFile()) {
                            FileDeviceEntity deviceEntity = createFileDeviceEntity(deviceFile, macFolder);
                            if (deviceEntity != null) {
                                newDevices.add(deviceEntity); // Thread-safe addition to the synchronized list
                                logger.info("Processed device file: {}", deviceFile.getName());
                            }
                        } else {
                            logger.warn("No matching file found for the generated filename: {}", generatedFilename);
                        }
                    });

            if (!newDevices.isEmpty()) {
                updateFileDevices(newDevices);
                logger.info("Updated devices with {} new entries.", newDevices.size());
            }

        } catch (Exception e) {
            logger.error("Error during device collection: {}", e.getMessage(), e);
        }
    }

    private FileDeviceEntity createFileDeviceEntity(File deviceFile, File macFolder) {
        try {
            Map<String, String> deviceInfo = parseDeviceFile(deviceFile);
            if (deviceInfo.isEmpty()) {
                logger.warn("No valid data found in file: {}", deviceFile.getName());
                return null;
            }

            logger.info("DeviceInfo: {}", deviceInfo);

            // Process the data (create a FileDeviceEntity)
            FileDeviceEntity deviceEntity = new FileDeviceEntity();
            deviceEntity.setMacAddress(deviceInfo.get("mac"));
            deviceEntity.setDeviceName(deviceInfo.get("devname"));
            deviceEntity.setIpAddress(deviceInfo.get("ip"));
            deviceEntity.setApplianceState(deviceInfo.get("rstate"));
            deviceEntity.setWifiSignalStrength(Integer.valueOf(deviceInfo.get("wss")));
            deviceEntity.setOnline(deviceInfo.get("online").equals("1"));
            deviceEntity.setCodeVersion(deviceInfo.get("version"));
            // Parse sync_time to LocalDateTime
            String syncTimeString = deviceInfo.get("sync_time");
            if (syncTimeString != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime syncTime = LocalDateTime.parse(syncTimeString, formatter);
                deviceEntity.setSyncTime(syncTime);
            }
            deviceEntity.setUsers(deviceInfo.get("users"));

            String[] userRecords = deviceInfo.get("users").split("\\|");
            for (String userRecord : userRecords) {
                if (!userRecord.trim().isEmpty()) {
                    // Split each record by "," to extract user details
                    String[] userDetails = userRecord.split(",");
                    if (userDetails.length == 3) { // Ensure all details are present
                        String userCode = userDetails[0];
                        String userIpAddress = userDetails[1];
                        String userFailureCount = userDetails[2];
                        DeviceUserEntity deviceUserEntity = new DeviceUserEntity();
                        deviceUserEntity.setUserCode(userCode);
                        deviceUserEntity.setUserIpAddress(userIpAddress);
                        deviceUserEntity.setUserFailureCount(userFailureCount);

                        CustomerModel customer = customers.getCustomerByCode(userCode);
                        if (customer != null) {
                            deviceUserEntity.setCustomerId(customer.getId());
                            deviceUserEntity.setName(customer.getName());
                        }
                        deviceEntity.setDeviceUser(deviceUserEntity);
                    }
                }
            }
            return deviceEntity;
        } catch (Exception e) {
            logger.error("Error creating FileDeviceEntity for file: {}", deviceFile.getName(), e);
            return null;
        }
    }

    private Map<String, String> parseDeviceFile(File deviceFile) throws IOException {
        Map<String, String> deviceInfo = new ConcurrentHashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(deviceFile))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("=", 2); // Split only on the first '=' character
                if (parts.length == 2) {
                    deviceInfo.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        return deviceInfo;
    }
}
