package com.homenetics.eagleeye.manager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.homenetics.eagleeye.entity.AlarmEntity;
import com.homenetics.eagleeye.entity.DeviceEntity;

@Component
public class AlarmManager {
    public static final int SEVERITY_ERROR = 4;
    public static final int SEVERITY_OK = 2;
    public static final int SEVERITY_WARN = 3;
    public static final int SEVERITY_INFO = 1;

    // Thread-safe map to manage alarms with a composite key: entityType:entityId:key
    private final ConcurrentHashMap<String, AlarmEntity> activeAlarms = new ConcurrentHashMap<>();
    private final List<AlarmEntity> historicalAlarms = new ArrayList<>();

    @Autowired
    private DevicesManager devicesManager;

    private Logger logger = LoggerFactory.getLogger(AlarmManager.class);

    public AlarmManager() {}


    @Scheduled(fixedRate = 60000)
    public void refreshAlarms() {
        // take the device details from device collector
        // take the device details from database
        // generate alarms
        List<DeviceEntity> devices = devicesManager.getAllDevices();
        logger.info("Starting refreshAlarms of devices. Total devices: {}", devices.size());
        devices.parallelStream().forEach(device -> {
            this.processMessageUrlResponseCode(device);
            this.processBootTimeResponseCode(device);
            this.processMessageDeliveryStatus(device);
            this.processDeviceActiveState(device);
            this.processSignalStrength(device);
        });
        logger.info("Successfully refreshed alarms of all devices.");
    }

    public void processDeviceActiveState(DeviceEntity deviceEntity) {
        AlarmEntity alarm = new AlarmEntity();
        alarm.setKey("dev.active.status");
        alarm.setEntityId(deviceEntity.getDeviceId());
        alarm.setEntityType("device");

        AlarmEntity existingAlarm = activeAlarms.get(generateKey(alarm));

        if (deviceEntity.getActiveState() == 1) {
            // device is active
            alarm.setStatus("up");
            alarm.setDetail("Device is active");
            alarm.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.getActiveState() == 0) {
            // device is not active
            alarm.setStatus("down");
            alarm.setDetail("Device is not active");
            alarm.setSeverity(SEVERITY_ERROR);
        } else if (deviceEntity.getActiveState() == 2) {
            // device is not active
            alarm.setStatus("delay");
            alarm.setDetail("Device is not active");
            alarm.setSeverity(SEVERITY_WARN);
        } else {
            alarm.setStatus("unknown");
            alarm.setDetail("Device Active State = " + String.valueOf(deviceEntity.getActiveState()));
            alarm.setSeverity(SEVERITY_ERROR);
        }

        if (existingAlarm != null) {
            if (existingAlarm.getSeverity() != alarm.getSeverity() || existingAlarm.getStatus() != alarm.getStatus()) {
                // alarm severity changed, push old alarm to history
                this.moveToHistory(existingAlarm);
                // as old alarm shifted to history
                alarm.setStartTime(LocalDateTime.now());
            } else {
                alarm.setStartTime(existingAlarm.getStartTime());
            }
        }else{
            alarm.setStartTime(LocalDateTime.now());
        }

        alarm.setLastUpdatedTime(LocalDateTime.now());
        alarm.setDuration(this.getDuration(alarm.getStartTime()));
        alarm.setState(true);
        this.addAlarm(alarm);

    }

    public void processSignalStrength(DeviceEntity deviceEntity) {
        AlarmEntity alarm = new AlarmEntity();
        alarm.setKey("dev.signal.strength");
        alarm.setEntityId(deviceEntity.getDeviceId());
        alarm.setEntityType("device");

        AlarmEntity existingAlarm = activeAlarms.get(generateKey(alarm));
        if (deviceEntity.getSignalStrength() == null ) {
            alarm.setStatus("unknown");
            alarm.setDetail("Signal Strength = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarm.setSeverity(SEVERITY_WARN);
        } else if (deviceEntity.getSignalStrength() == 1 ) {
            alarm.setStatus("excellent");
            alarm.setDetail("Signal Strength (Excellent) = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarm.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.getSignalStrength() == 2 ) {
            alarm.setStatus("very good");
            alarm.setDetail("Signal Strength (Very Good) = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarm.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.getSignalStrength() == 3 ) {
            alarm.setStatus("good");
            alarm.setDetail("Signal Strength (Good) = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarm.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.getSignalStrength() == 4 ) {
            alarm.setStatus("fair");
            alarm.setDetail("Signal Strength (Fair) = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarm.setSeverity(SEVERITY_WARN);
        } else if (deviceEntity.getSignalStrength() == 5 ) {
            alarm.setStatus("bad");
            alarm.setDetail("Signal Strength (Poor) = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarm.setSeverity(SEVERITY_WARN);
        } else if (deviceEntity.getSignalStrength() == 6 ) {
            alarm.setStatus("very bad");
            alarm.setDetail("Signal Strength (Very Poor) = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarm.setSeverity(SEVERITY_ERROR);
        }


        if (existingAlarm != null) {
            if (existingAlarm.getSeverity() != alarm.getSeverity() || existingAlarm.getStatus() != alarm.getStatus()) {
                // alarm severity changed, push old alarm to history
                this.moveToHistory(existingAlarm);
                // as old alarm shifted to history
                alarm.setStartTime(LocalDateTime.now());
            } else {
                alarm.setStartTime(existingAlarm.getStartTime());
            }
        }else{
            alarm.setStartTime(LocalDateTime.now());
        }

        alarm.setLastUpdatedTime(LocalDateTime.now());
        alarm.setDuration(this.getDuration(alarm.getStartTime()));
        alarm.setState(true);
        this.addAlarm(alarm);
    }

    public void processMessageDeliveryStatus(DeviceEntity deviceEntity) {
        AlarmEntity alarm = new AlarmEntity();
        alarm.setKey("dev.msgdelivery.status");
        alarm.setEntityId(deviceEntity.getDeviceId());
        alarm.setEntityType("device");

        AlarmEntity existingAlarm = activeAlarms.get(generateKey(alarm));
        if (deviceEntity.getMessage_publish_status() == null ) {
            alarm.setStatus("unknown");
            alarm.setDetail("Message Publish Status = " + String.valueOf(deviceEntity.getMessage_publish_status()));
            alarm.setSeverity(SEVERITY_WARN);
        } else if (deviceEntity.getMessage_publish_status() == true && deviceEntity.isOnline() == true) {
            alarm.setStatus("ok");
            alarm.setDetail("Message Published, Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
            alarm.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.getMessage_publish_status() == false && deviceEntity.isOnline() == true) {
            alarm.setStatus("failed");
            alarm.setDetail("Message Not Published, Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
            alarm.setSeverity(SEVERITY_ERROR);
        } else if (deviceEntity.getMessage_publish_status() == false && deviceEntity.isOnline() == false) {
            alarm.setStatus("ok");
            alarm.setDetail("Message Not Published, Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
            alarm.setSeverity(SEVERITY_OK);
        } else {
            alarm.setStatus("unknown");
            alarm.setDetail("Message Publish Status = " + String.valueOf(deviceEntity.getMessage_publish_status()) + ", Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
            alarm.setSeverity(SEVERITY_ERROR);
        }

        if (existingAlarm != null) {
            if (existingAlarm.getSeverity() != alarm.getSeverity()) {
                // alarm severity changed, push old alarm to history
                this.moveToHistory(existingAlarm);
                // as old alarm shifted to history
                alarm.setStartTime(LocalDateTime.now());
            } else {
                alarm.setStartTime(existingAlarm.getStartTime());
            }
        }else{
            alarm.setStartTime(LocalDateTime.now());
        }

        alarm.setLastUpdatedTime(LocalDateTime.now());
        alarm.setDuration(this.getDuration(alarm.getStartTime()));
        alarm.setState(true);
        this.addAlarm(alarm);
    }

    public void processBootTimeResponseCode(DeviceEntity deviceEntity) {
        AlarmEntity alarm = new AlarmEntity();
        alarm.setKey("dev.boottime.code");
        alarm.setEntityId(deviceEntity.getDeviceId());
        alarm.setEntityType("device");

        AlarmEntity existingAlarm = activeAlarms.get(generateKey(alarm));
        
        Integer responseCode = deviceEntity.getBoot_status_code();

        if (responseCode == null) {
            alarm.setStatus("unknown");
            alarm.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
            alarm.setSeverity(SEVERITY_WARN);
        }
        else if (responseCode == -99 || responseCode == 200) {
            alarm.setStatus("ok");
            alarm.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
            alarm.setSeverity(SEVERITY_OK);
        } else {
            alarm.setStatus("error");
            alarm.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
            alarm.setSeverity(SEVERITY_ERROR);
        }

        if (existingAlarm != null) {
            if (existingAlarm.getSeverity() != alarm.getSeverity()) {
                // alarm severity changed, push old alarm to history
                this.moveToHistory(existingAlarm);
                // as old alarm shifted to history
                alarm.setStartTime(LocalDateTime.now());
            } else {
                alarm.setStartTime(existingAlarm.getStartTime());
            }
        }else{
            alarm.setStartTime(LocalDateTime.now());
        }

        alarm.setLastUpdatedTime(LocalDateTime.now());
        alarm.setDuration(this.getDuration(alarm.getStartTime()));
        alarm.setState(true);
        this.addAlarm(alarm);

    }

    public void processMessageUrlResponseCode(DeviceEntity deviceEntity) {
        AlarmEntity alarm = new AlarmEntity();
        alarm.setKey("dev.msgurl.code");
        alarm.setEntityId(deviceEntity.getDeviceId());
        alarm.setEntityType("device");

        AlarmEntity existingAlarm = activeAlarms.get(generateKey(alarm));
        
        Integer responseCode = deviceEntity.getDownloadMqttUrlResponseCode();

        if (responseCode == null ) {
            alarm.setStatus("unknown");
            alarm.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
            alarm.setSeverity(SEVERITY_WARN);
        }
        else if (responseCode == -99 || responseCode == 200) {
            alarm.setStatus("ok");
            alarm.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
            alarm.setSeverity(SEVERITY_OK);
        } else {
            alarm.setStatus("error");
            alarm.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
            alarm.setSeverity(SEVERITY_ERROR);
        }

        if (existingAlarm != null) {
            if (existingAlarm.getSeverity() != alarm.getSeverity()) {
                // alarm severity changed, push old alarm to history
                this.moveToHistory(existingAlarm);
                // as old alarm shifted to history
                alarm.setStartTime(LocalDateTime.now());
            } else {
                alarm.setStartTime(existingAlarm.getStartTime());
            }
        }else{
            alarm.setStartTime(LocalDateTime.now());
        }

        alarm.setLastUpdatedTime(LocalDateTime.now());
        alarm.setDuration(this.getDuration(alarm.getStartTime()));
        alarm.setState(true);
        this.addAlarm(alarm);
    }

    public Long getDuration(LocalDateTime startTime) {
        return LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - startTime.toEpochSecond(ZoneOffset.UTC);
    }

    private void moveToHistory(AlarmEntity alarm) {
        alarm.setState(false); // Mark as history
        alarm.setResolutionTime(LocalDateTime.now());
        alarm.setDuration(this.getDuration(alarm.getStartTime()));
        activeAlarms.remove(generateKey(alarm));
        historicalAlarms.add(alarm);
    }
    
    // Add or update an alarm
    public void addAlarm(AlarmEntity alarm) {
        String key = generateKey(alarm);
        activeAlarms.put(key, alarm);
    }

    // Remove a specific alarm
    public void deleteAlarm(AlarmEntity alarm) {
        logger.warn("Deleting alarm: " + generateKey(alarm));
        String key = generateKey(alarm);
        activeAlarms.remove(key);
    }

    // Retrieve a specific alarm by composite key
    public AlarmEntity getAlarm(String key) {
        return activeAlarms.get(key);
    }

    // Retrieve all alarms
    public List<AlarmEntity> getAllAlarms() {
        return activeAlarms.values().stream().collect(Collectors.toList());
    }

    // Clear all activeAlarms
    public void clear() {
        logger.warn("Clearing all active alarms");
        activeAlarms.clear();
    }

    // Update an existing alarm's details
    public void updateAlarm(AlarmEntity updatedAlarm) {
        String key = generateKey(updatedAlarm);
        activeAlarms.computeIfPresent(key, (k, existingAlarm) -> {
            existingAlarm.setSeverity(updatedAlarm.getSeverity());
            existingAlarm.setStatus(updatedAlarm.getStatus());
            existingAlarm.setDetail(updatedAlarm.getDetail());
            existingAlarm.setDuration(updatedAlarm.getDuration());
            return existingAlarm;
        });
    }

    // Retrieve alarms filtered by severity
    public List<AlarmEntity> getAlarmsBySeverity(int severity) {
        return activeAlarms.values().stream()
                .filter(alarm -> alarm.getSeverity() == severity)
                .collect(Collectors.toList());
    }

    // Retrieve alarms filtered by status
    public List<AlarmEntity> getAlarmsByStatus(String status) {
        return activeAlarms.values().stream()
                .filter(alarm -> alarm.getStatus().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    // Retrieve alarms by a specific entity type
    public List<AlarmEntity> getAlarmsByEntityType(String entityType) {
        return activeAlarms.values().stream()
                .filter(alarm -> alarm.getEntityType().equalsIgnoreCase(entityType))
                .collect(Collectors.toList());
    }

    // Retrieve alarms by a specific entity ID
    public List<AlarmEntity> getAlarmsByEntityId(Integer entityId) {
        return activeAlarms.values().stream()
                .filter(alarm -> alarm.getEntityId().equals(entityId))
                .collect(Collectors.toList());
    }

    // Generate a composite key for an alarm
    private String generateKey(AlarmEntity alarm) {
        return alarm.getEntityType() + ":" + alarm.getEntityId() + ":" + alarm.getKey(); // active alarm
    }
}
