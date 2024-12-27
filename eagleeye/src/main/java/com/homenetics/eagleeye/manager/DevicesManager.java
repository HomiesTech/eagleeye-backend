package com.homenetics.eagleeye.manager;

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
import java.util.stream.Collectors;

@Component
public class DevicesManager {
    private static final Integer MIN_ACTIVE_MINUTE = 3;
    private static final Integer MAX_ACTIVE_MINUTE = 5;
    private static final Logger logger = LoggerFactory.getLogger(DevicesManager.class);

    @Autowired
    private DeviceRepository deviceRepository;

    @Scheduled(fixedRate = 60000)
    public void refreshActiveState() {
        logger.info("Starting refreshActiveState of devices.");
        try {
            // Parallelize the processing of devices
            Integer page = 0;
            Integer size = 100;
            Page<ActiveStateDTO> devicePage;
            do {
                Pageable pageable = PageRequest.of(page, size);
                devicePage = deviceRepository.findActiveDevices(pageable);
                logger.info("Devices: " + devicePage.toString());
                devicePage.getContent()
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
        LocalDateTime syncTime = device.getSyncTime();
        if (syncTime != null) {
            long minuteDifference = Duration.between(syncTime, now).toMinutes();
            if (minuteDifference >= 0 && minuteDifference < MIN_ACTIVE_MINUTE) {
                device.setActiveState(1); // ACTIVE
                this.deviceRepository.updateActiveState(device.getDeviceId(), 1);
                logger.info("SyncTime for device ID {}: {}, Current Time: {}, Time Difference: {}, state: {}", device.getDeviceId(), syncTime, now, minuteDifference, "active");
            } else if (minuteDifference >= MIN_ACTIVE_MINUTE && minuteDifference < MAX_ACTIVE_MINUTE) {
                device.setActiveState(2); // WARN
                this.deviceRepository.updateActiveState(device.getDeviceId(),2);
                logger.warn("SyncTime for device ID {}: {}, Current Time: {}, Time Difference: {}, state: {}", device.getDeviceId(), syncTime, now, minuteDifference, "delay");
            } else {
                device.setActiveState(0); // OFFLINE
                this.deviceRepository.updateActiveState(device.getDeviceId(),0);
                logger.warn("SyncTime for device ID {}: {}, Current Time: {}, Time Difference: {}, state: {}", device.getDeviceId(), syncTime, now, minuteDifference, "down");
            }
        } else {
            device.setActiveState(0); // Default to OFFLINE
            this.deviceRepository.updateActiveState(device.getDeviceId(),0);
            logger.warn("syncTime is null for device ID {}: setting active state to OFFLINE", device.getDeviceId());
        }
    }
}
