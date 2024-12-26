package com.homenetics.eagleeye.manager;

import com.homenetics.eagleeye.entity.APIEntity.DeviceEntity;
import com.homenetics.eagleeye.entity.DBEntity.DeviceDBEntity;
import com.homenetics.eagleeye.repository.DeviceRepository;
import com.homenetics.eagleeye.repository.DTO.ActiveStateDTO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class DevicesManager {
    private static final Integer MIN_ACTIVE_MINUTE = 3;
    private static final Integer MAX_ACTIVE_MINUTE = 5;
    private final ConcurrentHashMap<Integer, DeviceEntity> mergedDevices = new ConcurrentHashMap<>();
    private static final Logger logger = LoggerFactory.getLogger(DevicesManager.class);

    @Autowired
    private DeviceRepository deviceRepository;

    @Scheduled(fixedRate = 60000)
    public void refreshActiveState() {
        logger.info("Starting refreshActiveState of devices. Total devices: {}", mergedDevices.size());
        try {
            // Parallelize the processing of devices
            Integer page = 0;
            Integer size = 100;
            Page<ActiveStateDTO> devicePage;
            do {
                Pageable pageable = PageRequest.of(page, size);
                devicePage = deviceRepository.findActiveDevices(pageable);
                logger.info("Devices: " + devicePage.toString());
                List<ActiveStateDTO> updatedDevices = devicePage.getContent()
                        .parallelStream()
                        .peek(device -> {
                            try {
                                calculateIsActive(device);
                            } catch (Exception e) {
                                logger.error("Error calculating active state for device ID {}: {}",
                                        device.getDeviceId(), e.getMessage(), e);
                            }
                        })
                        .collect(Collectors.toList());

                // Save updated devices back to the database
                saveActiveStates(updatedDevices);
                page++;

            } while (devicePage.hasNext());

            logger.info("Successfully refreshed active state of all devices.");
        } catch (Exception e) {
            logger.error("Error refreshing calculateActiveState of devices: {}", e.getMessage(), e);
        }
    }

    /**
     * Calculate active state for a single device based on syncTime.
     */
    private void calculateIsActive(ActiveStateDTO device) {
        LocalDateTime now = LocalDateTime.now();
        logger.info("Calculating active state for device ID: {} at time: {}", device.getDeviceId(), now);
        LocalDateTime syncTime = device.getSyncTime();
        if (syncTime != null) {
            long minuteDifference = Duration.between(syncTime, now).toMinutes();
            if (minuteDifference >= 0 && minuteDifference < MIN_ACTIVE_MINUTE) {
                device.setActiveState(1); // ACTIVE
                this.deviceRepository.updateActiveState(device.getDeviceId(), 1);
            } else if (minuteDifference >= MIN_ACTIVE_MINUTE && minuteDifference < MAX_ACTIVE_MINUTE) {
                device.setActiveState(2); // WARN
                this.deviceRepository.updateActiveState(device.getDeviceId(),2);
            } else {
                device.setActiveState(0); // OFFLINE
                this.deviceRepository.updateActiveState(device.getDeviceId(),0);
            }
        } else {
            device.setActiveState(0); // Default to OFFLINE
            this.deviceRepository.updateActiveState(device.getDeviceId(),0);
        }
    }

    /**
     * Save the updated active states back to the database.
     */
    private void saveActiveStates(List<ActiveStateDTO> devices) {
        // Convert DTOs back to entities or use update queries as required
        List<DeviceDBEntity> entities = devices.stream()
                .map(dto -> {
                    DeviceDBEntity entity = deviceRepository.findById(dto.getDeviceId())
                            .orElseThrow(() -> new RuntimeException("Device not found with ID: " + dto.getDeviceId()));
                    entity.setActiveState(dto.getActiveState());
                    return entity;
                })
                .collect(Collectors.toList());

        deviceRepository.saveAll(entities);
        logger.info("Saved {} devices' active states to the database.", entities.size());
    }

}
