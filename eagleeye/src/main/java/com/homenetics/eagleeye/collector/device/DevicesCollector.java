package com.homenetics.eagleeye.collector.device;

import com.homenetics.eagleeye.entity.APIEntity.BootTimeDeviceEntity;
import com.homenetics.eagleeye.entity.APIEntity.FileDeviceEntity;
import com.homenetics.eagleeye.repository.DeviceRepository;

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
    private DeviceRepository deviceRepository;


//    private static final String DeviceDataPath = "D:\\DATA\\data";
    // private static final String DeviceDataPath = "/var/homenetics/devices/data";
    private static final String DeviceDataPath = "/app/devices/data";
    public synchronized List<FileDeviceEntity> getAllFileDevices() {
        return new ArrayList<>(fileDevices);
    }

    @Scheduled(fixedRate = 65000)
    public void bootTimeCollector() {
        // bootTimeDevices.clear();
        try {
            String deviceDataPath = DeviceDataPath;
            File baseFolder = new File(deviceDataPath);

            if (!baseFolder.exists() || !baseFolder.isDirectory()) {
                logger.warn("Base Folder does not exists or is not a directory: {}", deviceDataPath);
                return;
            }
            List<BootTimeDeviceEntity> newDevices = Collections.synchronizedList(new ArrayList<>());

            Arrays.stream(Objects.requireNonNull(baseFolder.listFiles(File::isDirectory)))
                    .parallel()
                    .forEach(macFolder -> {
                        logger.info("Processing folder: {}", macFolder.getAbsolutePath());

                        File deviceBootFile = new File(macFolder, "boot_time.txt");

                        if (deviceBootFile.exists() && deviceBootFile.isFile()) {
                            BootTimeDeviceEntity bootTimeDeviceEntity = createBootTimeDeviceEntity(deviceBootFile, macFolder);
                            if (bootTimeDeviceEntity != null) {
                                deviceRepository.updateBootTimeDevice(bootTimeDeviceEntity.getMacAddress(), bootTimeDeviceEntity.getBootTime());
                                // newDevices.add(bootTimeDeviceEntity); // Thread-safe addition to the synchronized list
                                logger.info("Processed device file: {}", deviceBootFile.getName());
                            }
                        } else {
                            logger.warn("No matching file found for the generated filename: {}", deviceBootFile.getName());
                        }
                    });

            if (!newDevices.isEmpty()) {
                // updateBootTimeDevices(newDevices);
                logger.info("Updated devices boot time with {} new entries.", newDevices.size());
            }
        } catch (Exception e) {
            logger.error("Error during device boot time collection: {}", e.getMessage(), e);
        }
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
            int targetMinute = now.getMinute() - 2;

            if (targetMinute < 0) {
                // Handle the case where minute goes negative (e.g., 00 -> 59 and adjust the hour)
                targetMinute = 59;
                currentHour = (currentHour - 1 + 24) % 24; // Adjust the hour if necessary
            }

            String timePattern = String.format("_%02d%02d", currentHour, targetMinute);

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
                            logger.info("Processed device file: {}, {}", deviceFile.getName(), deviceEntity);
                            if (deviceEntity != null) {
                                deviceRepository.updateFileDevice(
                                        deviceEntity.getMacAddress(),
                                        deviceEntity.getDeviceName(),
                                        deviceEntity.getIpAddress(),
                                        deviceEntity.getCodeVersion(),
                                        deviceEntity.getSyncTime(),
                                        deviceEntity.isOnline(),
                                        deviceEntity.getApplianceState(),
                                        deviceEntity.isPowersave(),
                                        deviceEntity.getUsername(),
                                        deviceEntity.getOtaTry(),
                                        deviceEntity.getOtaOk(),
                                        deviceEntity.getCredChangeTry(),
                                        deviceEntity.getCredChangeOk(),
                                        deviceEntity.getWifiSignalStrength(),
                                        deviceEntity.getNvs_used(),
                                        deviceEntity.getNvs_free(),
                                        deviceEntity.getNvs_total(),
                                        deviceEntity.getSpiffs_used(),
                                        deviceEntity.getSpiffs_total(),
                                        deviceEntity.getDownloadMqttUrlResponseCode(),
                                        deviceEntity.getMillis(),
                                        deviceEntity.getMessage_publish_status(),
                                        deviceEntity.getUsers(),
                                        now
                                );
                                logger.info("Processed device file: {}", deviceFile.getAbsolutePath(), deviceEntity);
                            }
                        } else {

                            logger.warn("No matching file found for the generated filename: {}", deviceFile.getAbsolutePath());
                        }
                    });

        } catch (Exception e) {
            logger.error("Error during device collection: {}", e.getMessage(), e);
        }
    }

    private BootTimeDeviceEntity createBootTimeDeviceEntity(File deviceBootFile, File macFolder) {
        try {
            String bootTime = parseDeviceBootFile(deviceBootFile);
            if (bootTime == null) {
                return null;
            }else {
                BootTimeDeviceEntity bootTimeDeviceEntity = new BootTimeDeviceEntity();
                if (!bootTime.isEmpty()) {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    LocalDateTime bTime = LocalDateTime.parse(bootTime, formatter);
                    bootTimeDeviceEntity.setBootTime(bTime);
                    bootTimeDeviceEntity.setMacAddress(macFolder.getName());
                }
                return bootTimeDeviceEntity;
            }
        } catch ( Exception e ) {
            logger.error("Error creating BootTimeDeviceEntity for file: {}", deviceBootFile.getName(), e);
            return null;
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
            deviceEntity.setPowersave(deviceInfo.get("powersave").equals("1"));
            deviceEntity.setNvs_free(Integer.valueOf(deviceInfo.get("nvs_free")));
            deviceEntity.setNvs_used(Integer.valueOf(deviceInfo.get("nvs_used")));
            deviceEntity.setNvs_total(Integer.valueOf(deviceInfo.get("nvs_total")));
            deviceEntity.setSpiffs_used(Integer.valueOf(deviceInfo.get("spiffs_used")));
            deviceEntity.setSpiffs_total(Integer.valueOf(deviceInfo.get("spiffs_total")));
            deviceEntity.setBoot_time_status_code(Integer.valueOf(deviceInfo.get("boot_status_code")));
            deviceEntity.setMessage_publish_status(Integer.valueOf(deviceInfo.get("message_publish_status")));
            deviceEntity.setOtaOk(deviceInfo.get("ota_ok"));
            deviceEntity.setOtaTry(deviceInfo.get("ota_try"));
            deviceEntity.setCredChangeOk(deviceInfo.get("change_cred_ok"));
            deviceEntity.setCredChangeOk(deviceInfo.get("change_cred_try"));


            deviceEntity.setDownloadMqttUrlResponseCode(deviceInfo.get("download_mqtt_url_res_code") != null ? Integer.valueOf(deviceInfo.get("download_mqtt_url_res_code")) : -99);
            deviceEntity.setMillis(Long.valueOf(deviceInfo.get("millis")));
            deviceEntity.setUsername(deviceInfo.get("username"));
            String syncTimeString = deviceInfo.get("sync_time");
            logger.info("sync_time: {}", syncTimeString);
            if (syncTimeString != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime syncTime = LocalDateTime.parse(syncTimeString, formatter);
                deviceEntity.setSyncTime(syncTime);
            }

            deviceEntity.setUsers(deviceInfo.get("users"));
            logger.warn("[FileDeviceEntity] {}", deviceEntity);
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
                }else{
                    deviceInfo.put(parts[0].trim(),"");
                }
            }
        }
        return deviceInfo;
    }

    private String parseDeviceBootFile(File deviceBootFile) throws IOException {
        if (deviceBootFile.exists() && deviceBootFile.isFile()) {
            try (BufferedReader br = new BufferedReader(new FileReader(deviceBootFile))) {
                // Read the first (and only) line
                return br.readLine();
            }
        } else {
            return null;
        }
    }
}
