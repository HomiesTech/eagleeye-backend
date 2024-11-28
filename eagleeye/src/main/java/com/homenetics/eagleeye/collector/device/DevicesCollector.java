package com.homenetics.eagleeye.collector.device;

import com.homenetics.eagleeye.entity.FileDeviceEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DevicesCollector {
    private final List<FileDeviceEntity> fileDevices = new ArrayList<>();
    private static final Logger logger = LoggerFactory.getLogger(DevicesCollector.class);
    private static final String DeviceDataPath = "D:\\DATA\\data";

    public synchronized List<FileDeviceEntity> getAllFileDevices() {
        return new ArrayList<>(fileDevices);
    }

    private synchronized void updateFileDevices(List<FileDeviceEntity> newDevices) {
        fileDevices.clear();
        fileDevices.addAll(newDevices);
    }

    @Scheduled(fixedRate = 65000) // Every 65 seconds
    public void collector() {
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
            List<FileDeviceEntity> newDevices = new ArrayList<>();

            for (File macFolder : Objects.requireNonNull(baseFolder.listFiles(File::isDirectory))) {
                logger.info("Processing folder: {}", macFolder.getAbsolutePath());
                // Current Date Folder
                File currentDateFolder = new File(macFolder, todayDate);


                if (!currentDateFolder.exists() || !currentDateFolder.isDirectory()) {
                    logger.warn("No folder found for today's date: {}", todayDate);
                    logger.warn("Current Folder does not exist or is not a directory: {}", currentDateFolder.getAbsolutePath());
                    continue;
                }

                // Process the files inside the date-specific subfolder
                String generatedFilename = todayDateP2 + timePattern + ".txt";
                // Check if the generated filename exists in the current date folder
                File deviceFile = new File(currentDateFolder, generatedFilename);
                if (deviceFile.exists() && deviceFile.isFile()) {
                    // Create a FileDeviceEntity from the file
                    FileDeviceEntity deviceEntity = createFileDeviceEntity(deviceFile, macFolder);
                    if (deviceEntity != null) {
                        newDevices.add(deviceEntity);
                        logger.info("Processed device file: {}", deviceFile.getName());
                    }
                } else {
                    logger.warn("No matching file found for the generated filename: {}", generatedFilename);
                }
            }

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

            // Process the data (create a FileDeviceEntity)
            FileDeviceEntity deviceEntity = new FileDeviceEntity();
            deviceEntity.setMacAddress(deviceInfo.get("mac"));
            deviceEntity.setDeviceName(deviceInfo.get("devname"));
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

            return deviceEntity;

        } catch (IOException e) {
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
