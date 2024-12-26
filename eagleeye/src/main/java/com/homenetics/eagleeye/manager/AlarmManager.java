package com.homenetics.eagleeye.manager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
// import java.util.concurrent.ConcurrentHashMap;
// import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.homenetics.eagleeye.entity.APIEntity.AlarmEntity;
import com.homenetics.eagleeye.entity.DBEntity.AlarmsDBEntity;
import com.homenetics.eagleeye.entity.DBEntity.DeviceDBEntity;
import com.homenetics.eagleeye.repository.AlarmsRepository;
import com.homenetics.eagleeye.repository.DeviceRepository;

// import com.homenetics.eagleeye.entity.APIEntity.AlarmEntity;
// import com.homenetics.eagleeye.entity.APIEntity.DeviceEntity;

@Component
public class AlarmManager {
    public static final int SEVERITY_ERROR = 4;
    public static final int SEVERITY_OK = 2;
    public static final int SEVERITY_WARN = 3;
    public static final int SEVERITY_INFO = 1;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private AlarmsRepository alarmsRepository;

    private Logger logger = LoggerFactory.getLogger(AlarmManager.class);

    public AlarmManager() {}

    @Scheduled(fixedRate = 60000)
    public void refreshAlarms() {
        logger.info("Refreshing alarms");
        // take the device details from deviceRepository as pagination
        Integer page = 0;
        Integer size = 100;
        Page<DeviceDBEntity> devicePage;
        try {
            do {
                Pageable pageable = PageRequest.of(page, size);
                devicePage =  this.deviceRepository.findAll(pageable);
                devicePage.getContent().parallelStream().forEach(device -> {
                    // this.processMessageUrlResponseCode(device);
                    // this.processBootTimeResponseCode(device);
                    this.processMessageDeliveryStatus(device);
                    this.processDeviceActiveState(device);
                    // this.processSignalStrength(device);
                    this.processOnlineComparisonAlarm(device);
                    this.processUsernameAlarm(device);
                });
            } while (devicePage.hasNext());
        } catch (Exception e) {
            logger.error("Error while refreshing alarms: {}", e.getMessage(), e);
        }
        logger.info("Alarms refreshed");
    }

    private void processOnlineComparisonAlarm(DeviceDBEntity deviceEntity) {
        AlarmEntity alarmEntity = new AlarmEntity();
        alarmEntity.setEntityType("device");
        alarmEntity.setEntityId(deviceEntity.getDeviceId());
        alarmEntity.setKey("dev.online.status");

        AlarmsDBEntity exisitingAlarm = alarmsRepository.getActiveAlarm(alarmEntity.getEntityType(), alarmEntity.getEntityId(), alarmEntity.getKey());

        if (deviceEntity.isOnline() == true && deviceEntity.isOnlineInDb() == true) {
            alarmEntity.setStatus("ok");
            alarmEntity.setDetail("Device is online");
            alarmEntity.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.isOnline() == false && deviceEntity.isOnlineInDb() == false) {
            alarmEntity.setStatus("down");
            alarmEntity.setDetail("Device is offline" + ", Device isOnlineInDb ? = " + String.valueOf(deviceEntity.isOnlineInDb()) + ", Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
            alarmEntity.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.isOnline() == true && deviceEntity.isOnlineInDb() == false) {
            alarmEntity.setStatus("error");
            alarmEntity.setDetail("Device Online Connection Error" + ", Device isOnlineInDb ? = " + String.valueOf(deviceEntity.isOnlineInDb()) + ", Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
            alarmEntity.setSeverity(SEVERITY_WARN);
        } else if (deviceEntity.isOnline() == false && deviceEntity.isOnlineInDb() == true) {
            alarmEntity.setStatus("delay");
            alarmEntity.setDetail("Device is offline but delay update in db" + ", Device isOnlineInDb ? = " + String.valueOf(deviceEntity.isOnlineInDb()) + ", Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
            alarmEntity.setSeverity(SEVERITY_WARN);
        } else {
            alarmEntity.setStatus("unknown");
            alarmEntity.setDetail("Device is not online" + ", Device isOnlineInDb ? = " + String.valueOf(deviceEntity.isOnlineInDb()) + ", Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
            alarmEntity.setSeverity(SEVERITY_ERROR);
        }

        if (exisitingAlarm != null) {
            if (exisitingAlarm.getSeverity() != alarmEntity.getSeverity() || exisitingAlarm.getStatus() != alarmEntity.getStatus()) {
                // alarm severity/status changes, move to history
                this.alarmsRepository.updateAlarmState(alarmEntity.getEntityType(),
                                                        alarmEntity.getEntityId(),
                                                        alarmEntity.getKey(),
                                                        LocalDateTime.now());
                // add new alarm as old alarm moved to history
            } else {
                alarmEntity.setStartTime(exisitingAlarm.getStartTime());
                alarmEntity.setLastUpdatedTime(LocalDateTime.now());
                alarmEntity.setState(true);
                alarmEntity.setAid(exisitingAlarm.getAlarmId());
                alarmEntity.setDuration(this.getDuration(alarmEntity.getStartTime()));
                AlarmsDBEntity oldAlarmUpdate = this.alarmEntityToAlarmDBEntity(alarmEntity, true);
                this.alarmsRepository.save(oldAlarmUpdate);
                logger.info("Old alarm updated: {}", oldAlarmUpdate);
                return;
            }
        }
        
        // adding new alarm
        alarmEntity.setStartTime(LocalDateTime.now());
        alarmEntity.setLastUpdatedTime(LocalDateTime.now());
        alarmEntity.setState(true);
        alarmEntity.setDuration(this.getDuration(alarmEntity.getStartTime()));
        AlarmsDBEntity newAlarm = this.alarmEntityToAlarmDBEntity(alarmEntity, false);
        this.alarmsRepository.save(newAlarm);
        logger.info("New alarm added: {}", newAlarm);
    }

    private void processUsernameAlarm(DeviceDBEntity deviceEntity) {
        AlarmEntity alarmEntity = new AlarmEntity();
        alarmEntity.setEntityType("device");
        alarmEntity.setEntityId(deviceEntity.getDeviceId());
        alarmEntity.setKey("dev.cred.username");

        AlarmsDBEntity exisitingAlarm = alarmsRepository.getActiveAlarm(alarmEntity.getEntityType(), alarmEntity.getEntityId(), alarmEntity.getKey());

        if (deviceEntity.getUsername() == null || deviceEntity.getUsername().isEmpty()) {
            alarmEntity.setStatus("NOT OK");
            alarmEntity.setDetail("Username is null");
            alarmEntity.setSeverity(SEVERITY_WARN);
        } else if (deviceEntity.getUsername() == deviceEntity.getSsid()) {
            alarmEntity.setStatus("NOT OK");
            alarmEntity.setDetail("Username is same as SSID" + ", Username = " + deviceEntity.getUsername() + ", SSID = " + deviceEntity.getSsid());
            alarmEntity.setSeverity(SEVERITY_WARN);
        } else {
            alarmEntity.setStatus("OK");
            alarmEntity.setDetail("Username is OK" + ", Username = " + deviceEntity.getUsername());
            alarmEntity.setSeverity(SEVERITY_OK);
        }

        if (exisitingAlarm != null) {
            if (exisitingAlarm.getSeverity() != alarmEntity.getSeverity()) {
                // Move existing alarm to history, as existing alarm changes
                this.alarmsRepository.updateAlarmState(alarmEntity.getEntityType(),
                                                       alarmEntity.getEntityId(),
                                                       alarmEntity.getKey(),
                                                       LocalDateTime.now());
                // as old alarm shift to history, add new alarm
            } else {
                // old alarm not shift to history, updating old alarm only and return
                alarmEntity.setStartTime(exisitingAlarm.getStartTime());
                alarmEntity.setLastUpdatedTime(LocalDateTime.now());
                alarmEntity.setState(true);
                alarmEntity.setAid(exisitingAlarm.getAlarmId());
                alarmEntity.setDuration(this.getDuration(alarmEntity.getStartTime()));
                AlarmsDBEntity oldAlarmUpdate = this.alarmEntityToAlarmDBEntity(alarmEntity, true);
                this.alarmsRepository.save(oldAlarmUpdate);
                logger.info("Old alarm updated: {}", oldAlarmUpdate);
                return;
            }
        }

        // adding new alarm
        alarmEntity.setStartTime(LocalDateTime.now());
        alarmEntity.setLastUpdatedTime(LocalDateTime.now());
        alarmEntity.setState(true);
        alarmEntity.setDuration(this.getDuration(alarmEntity.getStartTime()));
        AlarmsDBEntity newAlarm = this.alarmEntityToAlarmDBEntity(alarmEntity, false);
        this.alarmsRepository.save(newAlarm);
        logger.info("New alarm added: {}", newAlarm);
    }

    private void processDeviceActiveState(DeviceDBEntity deviceEntity) {
        AlarmEntity alarmEntity = new AlarmEntity();
        alarmEntity.setEntityType("device");
        alarmEntity.setEntityId(deviceEntity.getDeviceId());
        alarmEntity.setKey("dev.active.status");

        AlarmsDBEntity exisitingAlarm = alarmsRepository.getActiveAlarm(alarmEntity.getEntityType(), alarmEntity.getEntityId(), alarmEntity.getKey());

        if (deviceEntity.getActiveState() == 1) {
            // device is active
            alarmEntity.setStatus("up");
            alarmEntity.setDetail("Device is active");
            alarmEntity.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.getActiveState() == 0) {
            // device is not active
            alarmEntity.setStatus("down");
            alarmEntity.setDetail("Device is not active");
            alarmEntity.setSeverity(SEVERITY_WARN);
        } else if (deviceEntity.getActiveState() == 2) {
            // device is not active
            alarmEntity.setStatus("delay");
            alarmEntity.setDetail("Device is not active");
            alarmEntity.setSeverity(SEVERITY_WARN);
        } else {
            // device is not active
            alarmEntity.setStatus("unknown");
            alarmEntity.setDetail("Device Active State = " + String.valueOf(deviceEntity.getActiveState()));
            alarmEntity.setSeverity(SEVERITY_ERROR);
        }

        if (exisitingAlarm != null) {
            if (exisitingAlarm.getSeverity() != alarmEntity.getSeverity() || exisitingAlarm.getStatus() != alarmEntity.getStatus()) {
                // alarm severity changed, push old alarm to history
                this.alarmsRepository.updateAlarmState(alarmEntity.getEntityType(),
                                                       alarmEntity.getEntityId(),
                                                       alarmEntity.getKey(),
                                                       LocalDateTime.now());
                // as old alarm shift to history, add new alarm
            } else {
                // as the old alarm not shofted to history, updating old alarm
                alarmEntity.setStartTime(exisitingAlarm.getStartTime());
                alarmEntity.setLastUpdatedTime(LocalDateTime.now());
                alarmEntity.setState(true);
                alarmEntity.setAid(exisitingAlarm.getAlarmId());
                alarmEntity.setDuration(this.getDuration(alarmEntity.getStartTime()));
                AlarmsDBEntity oldAlarmUpdate = this.alarmEntityToAlarmDBEntity(alarmEntity, true);
                this.alarmsRepository.save(oldAlarmUpdate);
                logger.info("Old alarm updated: {}", oldAlarmUpdate);
                return;
            }
        }

        // add new alarm
        alarmEntity.setStartTime(LocalDateTime.now());
        alarmEntity.setLastUpdatedTime(LocalDateTime.now());
        alarmEntity.setState(true);
        alarmEntity.setDuration(this.getDuration(alarmEntity.getStartTime()));
        AlarmsDBEntity newAlarm = this.alarmEntityToAlarmDBEntity(alarmEntity, false);
        this.alarmsRepository.save(newAlarm);
        logger.info("New alarm added: {}", newAlarm);
    }

    private void processMessageDeliveryStatus(DeviceDBEntity deviceEntity) {
        AlarmEntity alarmEntity = new AlarmEntity();
        alarmEntity.setEntityType("device");
        alarmEntity.setEntityId(deviceEntity.getDeviceId());
        alarmEntity.setKey("dev.msgdelivery.status");

        AlarmsDBEntity exisitingAlarm = alarmsRepository.getActiveAlarm(alarmEntity.getEntityType(), alarmEntity.getEntityId(), alarmEntity.getKey());

        if (deviceEntity.getMessage_publish_status() == null ) {
            alarmEntity.setStatus("unknown");
            alarmEntity.setDetail("Message Publish Status = " + String.valueOf(deviceEntity.getMessage_publish_status()));
            alarmEntity.setSeverity(SEVERITY_WARN);
        } else if (deviceEntity.getMessage_publish_status() == 1 && deviceEntity.isOnline() == true) {
            alarmEntity.setStatus("ok");
            alarmEntity.setDetail("Message Publish Status = " + String.valueOf(deviceEntity.getMessage_publish_status()));
            alarmEntity.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.getMessage_publish_status() == 0 && deviceEntity.isOnline() == true) {
            alarmEntity.setStatus("failed");
            alarmEntity.setDetail("Message Not Published, Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
            alarmEntity.setSeverity(SEVERITY_ERROR);
        } else if (deviceEntity.getMessage_publish_status() == -1 && deviceEntity.isOnline() == false) {
            alarmEntity.setStatus("Not Started");
            alarmEntity.setDetail("Message Publish Not Started, Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
            alarmEntity.setSeverity(SEVERITY_OK);
        }else {
            alarmEntity.setStatus("unknown");
            alarmEntity.setDetail("Message Publish Status = " + String.valueOf(deviceEntity.getMessage_publish_status()) + ", Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
            alarmEntity.setSeverity(SEVERITY_ERROR);
        }

        if (exisitingAlarm != null) {
            if (exisitingAlarm.getSeverity() != alarmEntity.getSeverity() || exisitingAlarm.getStatus() != alarmEntity.getStatus()) {
                // alarm severity changed, push old alarm to history
                this.alarmsRepository.updateAlarmState(alarmEntity.getEntityType(),
                                                       alarmEntity.getEntityId(),
                                                       alarmEntity.getKey(),
                                                       LocalDateTime.now());
                // as old alarm shift to history, add new alarm
            } else {
                // as the old alarm not shofted to history, updating old alarm
                alarmEntity.setStartTime(exisitingAlarm.getStartTime());
                alarmEntity.setLastUpdatedTime(LocalDateTime.now());
                alarmEntity.setState(true);
                alarmEntity.setAid(exisitingAlarm.getAlarmId());
                alarmEntity.setDuration(this.getDuration(alarmEntity.getStartTime()));
                AlarmsDBEntity oldAlarmUpdate = this.alarmEntityToAlarmDBEntity(alarmEntity, true);
                this.alarmsRepository.save(oldAlarmUpdate);
                logger.info("Old alarm updated: {}", oldAlarmUpdate);
                return;
            }
        }

        // add new alarm
        alarmEntity.setStartTime(LocalDateTime.now());
        alarmEntity.setLastUpdatedTime(LocalDateTime.now());
        alarmEntity.setState(true);
        alarmEntity.setDuration(this.getDuration(alarmEntity.getStartTime()));
        AlarmsDBEntity newAlarm = this.alarmEntityToAlarmDBEntity(alarmEntity, false);
        this.alarmsRepository.save(newAlarm);
        logger.info("New alarm added: {}", newAlarm);
    }


    private AlarmsDBEntity alarmEntityToAlarmDBEntity(AlarmEntity alarmEntity, boolean old) {
        AlarmsDBEntity alarmsDBEntity = new AlarmsDBEntity();
        if (old) {
            alarmsDBEntity.setAlarmId(alarmEntity.getAid());
        }
        alarmsDBEntity.setEntityType(alarmEntity.getEntityType());
        alarmsDBEntity.setEntityId(alarmEntity.getEntityId());
        alarmsDBEntity.setAlarmKey(alarmEntity.getKey());
        alarmsDBEntity.setStartTime(alarmEntity.getStartTime());
        alarmsDBEntity.setLastUpdatedTime(alarmEntity.getLastUpdatedTime());
        alarmsDBEntity.setDuration(alarmEntity.getDuration());
        alarmsDBEntity.setState(alarmEntity.isState());
        alarmsDBEntity.setStatus(alarmEntity.getStatus());
        alarmsDBEntity.setDetail(alarmEntity.getDetail());
        alarmsDBEntity.setSeverity(alarmEntity.getSeverity());
        return alarmsDBEntity;
    }

    public Long getDuration(LocalDateTime startTime) {
        return LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - startTime.toEpochSecond(ZoneOffset.UTC);
    }

};

// @Component
// public class AlarmManager {
//     public static final int SEVERITY_ERROR = 4;
//     public static final int SEVERITY_OK = 2;
//     public static final int SEVERITY_WARN = 3;
//     public static final int SEVERITY_INFO = 1;

//     // Thread-safe map to manage alarms with a composite key: entityType:entityId:key
//     private final ConcurrentHashMap<String, AlarmEntity> activeAlarms = new ConcurrentHashMap<>();
//     private final List<AlarmEntity> historicalAlarms = new ArrayList<>();

//     @Autowired
//     private DevicesManager devicesManager;

//     private Logger logger = LoggerFactory.getLogger(AlarmManager.class);

//     public AlarmManager() {}


//     @Scheduled(fixedRate = 60000)
//     public void refreshAlarms() {
//         // take the device details from device collector
//         // take the device details from database
//         // generate alarms
//         List<DeviceEntity> devices = devicesManager.getAllDevices();
//         logger.info("Starting refreshAlarms of devices. Total devices: {}", devices.size());
//         devices.parallelStream().forEach(device -> {
//             this.processMessageUrlResponseCode(device);
//             this.processBootTimeResponseCode(device);
//             this.processMessageDeliveryStatus(device);
//             this.processDeviceActiveState(device);
//             this.processSignalStrength(device);
//             this.processOnlineComparisonAlarm(device);
//             this.processUsernameAlarm(device);
//         });
//         logger.info("Successfully refreshed alarms of all devices.");
//     }

//     public void processUsernameAlarm(DeviceEntity deviceEntity) {
//         AlarmEntity alarm = new AlarmEntity();
//         alarm.setKey("dev.cred.username");
//         alarm.setEntityId(deviceEntity.getDeviceId());
//         alarm.setEntityType("device");

//         AlarmEntity existingAlarm = activeAlarms.get(generateKey(alarm));

//         if (deviceEntity.getUsername() == null || deviceEntity.getUsername().isEmpty()) {
//             alarm.setStatus("NOT OK");
//             alarm.setDetail("Username is null");
//             alarm.setSeverity(SEVERITY_WARN);
//         } else if (deviceEntity.getUsername() == deviceEntity.getSsid()) {
//             alarm.setStatus("NOT OK");
//             alarm.setDetail("Username is same as SSID" + ", Username = " + deviceEntity.getUsername() + ", SSID = " + deviceEntity.getSsid());
//             alarm.setSeverity(SEVERITY_WARN);
//         } else {
//             alarm.setStatus("OK");
//             alarm.setDetail("Username is OK" + ", Username = " + deviceEntity.getUsername());
//             alarm.setSeverity(SEVERITY_OK);
//         }

//         if (existingAlarm != null) {
//             if (existingAlarm.getSeverity() != alarm.getSeverity()) {
//                 this.moveToHistory(existingAlarm);
//                 // as old alarm shifted to history
//                 alarm.setStartTime(LocalDateTime.now());
//             }
//         } else {
//             activeAlarms.put(generateKey(alarm), alarm);
//         }

//         alarm.setLastUpdatedTime(LocalDateTime.now());
//         alarm.setDuration(this.getDuration(alarm.getStartTime()));
//         alarm.setState(true);
//         this.addAlarm(alarm);
//     }

//     public void processOnlineComparisonAlarm(DeviceEntity deviceEntity) {
//         AlarmEntity alarm = new AlarmEntity();
//         alarm.setKey("dev.online.status");
//         alarm.setEntityId(deviceEntity.getDeviceId());
//         alarm.setEntityType("device");

//         AlarmEntity existingAlarm = activeAlarms.get(generateKey(alarm));

//         if (deviceEntity.isOnline() == true && deviceEntity.isOnlineInDb() == true) {
//             // device is online
//             alarm.setStatus("up");
//             alarm.setDetail("Device is online" + ", Device isOnlineInDb ? = " + String.valueOf(deviceEntity.isOnlineInDb()) + ", Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
//             alarm.setSeverity(SEVERITY_OK);
//         } else if (deviceEntity.isOnline() == false && deviceEntity.isOnlineInDb() == false) {
//             alarm.setStatus("down");
//             alarm.setDetail("Device is offline" + ", Device isOnlineInDb ? = " + String.valueOf(deviceEntity.isOnlineInDb()) + ", Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
//             alarm.setSeverity(SEVERITY_OK);
//         } else if (deviceEntity.isOnline() == true && deviceEntity.isOnlineInDb() == false) {
//             alarm.setStatus("error");
//             alarm.setDetail("Device Online Connection Error" + ", Device isOnlineInDb ? = " + String.valueOf(deviceEntity.isOnlineInDb()) + ", Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
//             alarm.setSeverity(SEVERITY_WARN);
//         } else if (deviceEntity.isOnline() == false && deviceEntity.isOnlineInDb() == true) {
//             alarm.setStatus("delay");
//             alarm.setDetail("Device is offline but delay update in db" + ", Device isOnlineInDb ? = " + String.valueOf(deviceEntity.isOnlineInDb()) + ", Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
//             alarm.setSeverity(SEVERITY_WARN);
//         } else {
//             alarm.setStatus("unknown");
//             alarm.setDetail("Device is not online" + ", Device isOnlineInDb ? = " + String.valueOf(deviceEntity.isOnlineInDb()) + ", Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
//             alarm.setSeverity(SEVERITY_ERROR);
//         }

//         if (existingAlarm != null) {
//             if (existingAlarm.getSeverity() != alarm.getSeverity() || existingAlarm.getStatus() != alarm.getStatus()) {
//                 // alarm severity changed, push old alarm to history
//                 this.moveToHistory(existingAlarm);
//                 // as old alarm shifted to history
//                 alarm.setStartTime(LocalDateTime.now());
//             } else {
//                 alarm.setStartTime(existingAlarm.getStartTime());
//             }
//         }else{
//             alarm.setStartTime(LocalDateTime.now());
//         }

//         alarm.setLastUpdatedTime(LocalDateTime.now());
//         alarm.setDuration(this.getDuration(alarm.getStartTime()));
//         if (alarm.getDuration() != null && alarm.getDuration() >= 150 && alarm.getStatus() == "error") {
//             // if alarm duration is more then 3 minute, conver to error
//             alarm.setSeverity(SEVERITY_ERROR);
//         }
//         alarm.setState(true);
//         this.addAlarm(alarm);
//     }

//     public void processDeviceActiveState(DeviceEntity deviceEntity) {
//         AlarmEntity alarm = new AlarmEntity();
//         alarm.setKey("dev.active.status");
//         alarm.setEntityId(deviceEntity.getDeviceId());
//         alarm.setEntityType("device");

//         AlarmEntity existingAlarm = activeAlarms.get(generateKey(alarm));

//         if (deviceEntity.getActiveState() == 1) {
//             // device is active
//             alarm.setStatus("up");
//             alarm.setDetail("Device is active");
//             alarm.setSeverity(SEVERITY_OK);
//         } else if (deviceEntity.getActiveState() == 0) {
//             // device is not active
//             alarm.setStatus("down");
//             alarm.setDetail("Device is not active");
//             alarm.setSeverity(SEVERITY_ERROR);
//         } else if (deviceEntity.getActiveState() == 2) {
//             // device is not active
//             alarm.setStatus("delay");
//             alarm.setDetail("Device is not active");
//             alarm.setSeverity(SEVERITY_OK);
//         } else {
//             alarm.setStatus("unknown");
//             alarm.setDetail("Device Active State = " + String.valueOf(deviceEntity.getActiveState()));
//             alarm.setSeverity(SEVERITY_ERROR);
//         }

//         if (existingAlarm != null) {
//             if (existingAlarm.getSeverity() != alarm.getSeverity() || existingAlarm.getStatus() != alarm.getStatus()) {
//                 // alarm severity changed, push old alarm to history
//                 this.moveToHistory(existingAlarm);
//                 // as old alarm shifted to history
//                 alarm.setStartTime(LocalDateTime.now());
//             } else {
//                 alarm.setStartTime(existingAlarm.getStartTime());
//             }
//         }else{
//             alarm.setStartTime(LocalDateTime.now());
//         }

//         alarm.setLastUpdatedTime(LocalDateTime.now());
//         alarm.setDuration(this.getDuration(alarm.getStartTime()));
//         alarm.setState(true);
//         this.addAlarm(alarm);

//     }

//     public void processSignalStrength(DeviceEntity deviceEntity) {
//         AlarmEntity alarm = new AlarmEntity();
//         alarm.setKey("dev.signal.strength");
//         alarm.setEntityId(deviceEntity.getDeviceId());
//         alarm.setEntityType("device");

//         AlarmEntity existingAlarm = activeAlarms.get(generateKey(alarm));
//         if (deviceEntity.getSignalStrength() == null ) {
//             alarm.setStatus("unknown");
//             alarm.setDetail("Signal Strength = " + String.valueOf(deviceEntity.getSignalStrength()));
//             alarm.setSeverity(SEVERITY_WARN);
//         } else if (deviceEntity.getSignalStrength() == 1 ) {
//             alarm.setStatus("excellent");
//             alarm.setDetail("Signal Strength (Excellent) = " + String.valueOf(deviceEntity.getSignalStrength()));
//             alarm.setSeverity(SEVERITY_OK);
//         } else if (deviceEntity.getSignalStrength() == 2 ) {
//             alarm.setStatus("very good");
//             alarm.setDetail("Signal Strength (Very Good) = " + String.valueOf(deviceEntity.getSignalStrength()));
//             alarm.setSeverity(SEVERITY_OK);
//         } else if (deviceEntity.getSignalStrength() == 3 ) {
//             alarm.setStatus("good");
//             alarm.setDetail("Signal Strength (Good) = " + String.valueOf(deviceEntity.getSignalStrength()));
//             alarm.setSeverity(SEVERITY_OK);
//         } else if (deviceEntity.getSignalStrength() == 4 ) {
//             alarm.setStatus("fair");
//             alarm.setDetail("Signal Strength (Fair) = " + String.valueOf(deviceEntity.getSignalStrength()));
//             alarm.setSeverity(SEVERITY_OK);
//         } else if (deviceEntity.getSignalStrength() == 5 ) {
//             alarm.setStatus("bad");
//             alarm.setDetail("Signal Strength (Poor) = " + String.valueOf(deviceEntity.getSignalStrength()));
//             alarm.setSeverity(SEVERITY_WARN);
//         } else if (deviceEntity.getSignalStrength() == 6 ) {
//             alarm.setStatus("very bad");
//             alarm.setDetail("Signal Strength (Very Poor) = " + String.valueOf(deviceEntity.getSignalStrength()));
//             alarm.setSeverity(SEVERITY_ERROR);
//         }


//         if (existingAlarm != null) {
//             if (existingAlarm.getSeverity() != alarm.getSeverity() || existingAlarm.getStatus() != alarm.getStatus()) {
//                 // alarm severity changed, push old alarm to history
//                 this.moveToHistory(existingAlarm);
//                 // as old alarm shifted to history
//                 alarm.setStartTime(LocalDateTime.now());
//             } else {
//                 alarm.setStartTime(existingAlarm.getStartTime());
//             }
//         }else{
//             alarm.setStartTime(LocalDateTime.now());
//         }

//         alarm.setLastUpdatedTime(LocalDateTime.now());
//         alarm.setDuration(this.getDuration(alarm.getStartTime()));
//         alarm.setState(true);
//         this.addAlarm(alarm);
//     }

//     public void processMessageDeliveryStatus(DeviceEntity deviceEntity) {
//         AlarmEntity alarm = new AlarmEntity();
//         alarm.setKey("dev.msgdelivery.status");
//         alarm.setEntityId(deviceEntity.getDeviceId());
//         alarm.setEntityType("device");

//         AlarmEntity existingAlarm = activeAlarms.get(generateKey(alarm));
//         if (deviceEntity.getMessage_publish_status() == null ) {
//             alarm.setStatus("unknown");
//             alarm.setDetail("Message Publish Status = " + String.valueOf(deviceEntity.getMessage_publish_status()));
//             alarm.setSeverity(SEVERITY_WARN);
//         } else if (deviceEntity.getMessage_publish_status() == 1 && deviceEntity.isOnline() == true) {
//             alarm.setStatus("ok");
//             alarm.setDetail("Message Published, Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
//             alarm.setSeverity(SEVERITY_OK);
//         } else if (deviceEntity.getMessage_publish_status() == 0 && deviceEntity.isOnline() == true) {
//             alarm.setStatus("failed");
//             alarm.setDetail("Message Not Published, Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
//             alarm.setSeverity(SEVERITY_ERROR);
//         } else if (deviceEntity.getMessage_publish_status() == -1 && deviceEntity.isOnline() == false) {
//             alarm.setStatus("Not Started");
//             alarm.setDetail("Message Publish Not Started, Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
//             alarm.setSeverity(SEVERITY_OK);
//         } else {
//             alarm.setStatus("unknown");
//             alarm.setDetail("Message Publish Status = " + String.valueOf(deviceEntity.getMessage_publish_status()) + ", Device isOnline ? = " + String.valueOf(deviceEntity.isOnline()));
//             alarm.setSeverity(SEVERITY_ERROR);
//         }

//         if (existingAlarm != null) {
//             if (existingAlarm.getSeverity() != alarm.getSeverity()) {
//                 // alarm severity changed, push old alarm to history
//                 this.moveToHistory(existingAlarm);
//                 // as old alarm shifted to history
//                 alarm.setStartTime(LocalDateTime.now());
//             } else {
//                 alarm.setStartTime(existingAlarm.getStartTime());
//             }
//         }else{
//             alarm.setStartTime(LocalDateTime.now());
//         }

//         alarm.setLastUpdatedTime(LocalDateTime.now());
//         alarm.setDuration(this.getDuration(alarm.getStartTime()));
//         alarm.setState(true);
//         this.addAlarm(alarm);
//     }

//     public void processBootTimeResponseCode(DeviceEntity deviceEntity) {
//         AlarmEntity alarm = new AlarmEntity();
//         alarm.setKey("dev.boottime.code");
//         alarm.setEntityId(deviceEntity.getDeviceId());
//         alarm.setEntityType("device");

//         AlarmEntity existingAlarm = activeAlarms.get(generateKey(alarm));
        
//         Integer responseCode = deviceEntity.getBoot_status_code();

//         if (responseCode == null) {
//             alarm.setStatus("unknown");
//             alarm.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
//             alarm.setSeverity(SEVERITY_WARN);
//         }
//         else if (responseCode == -99 || responseCode == 200) {
//             alarm.setStatus("ok");
//             alarm.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
//             alarm.setSeverity(SEVERITY_OK);
//         } else {
//             alarm.setStatus("error");
//             alarm.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
//             alarm.setSeverity(SEVERITY_ERROR);
//         }

//         if (existingAlarm != null) {
//             if (existingAlarm.getSeverity() != alarm.getSeverity()) {
//                 // alarm severity changed, push old alarm to history
//                 this.moveToHistory(existingAlarm);
//                 // as old alarm shifted to history
//                 alarm.setStartTime(LocalDateTime.now());
//             } else {
//                 alarm.setStartTime(existingAlarm.getStartTime());
//             }
//         }else{
//             alarm.setStartTime(LocalDateTime.now());
//         }

//         alarm.setLastUpdatedTime(LocalDateTime.now());
//         alarm.setDuration(this.getDuration(alarm.getStartTime()));
//         alarm.setState(true);
//         this.addAlarm(alarm);

//     }

//     public void processMessageUrlResponseCode(DeviceEntity deviceEntity) {
//         AlarmEntity alarm = new AlarmEntity();
//         alarm.setKey("dev.msgurl.code");
//         alarm.setEntityId(deviceEntity.getDeviceId());
//         alarm.setEntityType("device");

//         AlarmEntity existingAlarm = activeAlarms.get(generateKey(alarm));
        
//         Integer responseCode = deviceEntity.getDownloadMqttUrlResponseCode();

//         if (responseCode == null ) {
//             alarm.setStatus("unknown");
//             alarm.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
//             alarm.setSeverity(SEVERITY_WARN);
//         }
//         else if (responseCode == -99 || responseCode == 200) {
//             alarm.setStatus("ok");
//             alarm.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
//             alarm.setSeverity(SEVERITY_OK);
//         } else {
//             alarm.setStatus("error");
//             alarm.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
//             alarm.setSeverity(SEVERITY_ERROR);
//         }

//         if (existingAlarm != null) {
//             if (existingAlarm.getSeverity() != alarm.getSeverity()) {
//                 // alarm severity changed, push old alarm to history
//                 this.moveToHistory(existingAlarm);
//                 // as old alarm shifted to history
//                 alarm.setStartTime(LocalDateTime.now());
//             } else {
//                 alarm.setStartTime(existingAlarm.getStartTime());
//             }
//         }else{
//             alarm.setStartTime(LocalDateTime.now());
//         }

//         alarm.setLastUpdatedTime(LocalDateTime.now());
//         alarm.setDuration(this.getDuration(alarm.getStartTime()));
//         alarm.setState(true);
//         this.addAlarm(alarm);
//     }

//     public Long getDuration(LocalDateTime startTime) {
//         return LocalDateTime.now().toEpochSecond(ZoneOffset.UTC) - startTime.toEpochSecond(ZoneOffset.UTC);
//     }

//     private void moveToHistory(AlarmEntity alarm) {
//         alarm.setState(false); // Mark as history
//         alarm.setResolutionTime(LocalDateTime.now());
//         alarm.setDuration(this.getDuration(alarm.getStartTime()));
//         activeAlarms.remove(generateKey(alarm));
//         historicalAlarms.add(alarm);
//     }
    
//     // Add or update an alarm
//     public void addAlarm(AlarmEntity alarm) {
//         String key = generateKey(alarm);
//         activeAlarms.put(key, alarm);
//     }

//     // Remove a specific alarm
//     public void deleteAlarm(AlarmEntity alarm) {
//         logger.warn("Deleting alarm: " + generateKey(alarm));
//         String key = generateKey(alarm);
//         activeAlarms.remove(key);
//     }

//     // Retrieve a specific alarm by composite key
//     public AlarmEntity getAlarm(String key) {
//         return activeAlarms.get(key);
//     }

//     // Retrieve all alarms
//     public List<AlarmEntity> getAllAlarms() {
//         return activeAlarms.values().stream().collect(Collectors.toList());
//     }

//     // Clear all activeAlarms
//     public void clear() {
//         logger.warn("Clearing all active alarms");
//         activeAlarms.clear();
//     }

//     // Update an existing alarm's details
//     public void updateAlarm(AlarmEntity updatedAlarm) {
//         String key = generateKey(updatedAlarm);
//         activeAlarms.computeIfPresent(key, (k, existingAlarm) -> {
//             existingAlarm.setSeverity(updatedAlarm.getSeverity());
//             existingAlarm.setStatus(updatedAlarm.getStatus());
//             existingAlarm.setDetail(updatedAlarm.getDetail());
//             existingAlarm.setDuration(updatedAlarm.getDuration());
//             return existingAlarm;
//         });
//     }

//     // Retrieve alarms filtered by severity
//     public List<AlarmEntity> getAlarmsBySeverity(int severity) {
//         return activeAlarms.values().stream()
//                 .filter(alarm -> alarm.getSeverity() == severity)
//                 .collect(Collectors.toList());
//     }

//     // Retrieve alarms filtered by status
//     public List<AlarmEntity> getAlarmsByStatus(String status) {
//         return activeAlarms.values().stream()
//                 .filter(alarm -> alarm.getStatus().equalsIgnoreCase(status))
//                 .collect(Collectors.toList());
//     }

//     // Retrieve alarms by a specific entity type
//     public List<AlarmEntity> getAlarmsByEntityType(String entityType) {
//         return activeAlarms.values().stream()
//                 .filter(alarm -> alarm.getEntityType().equalsIgnoreCase(entityType))
//                 .collect(Collectors.toList());
//     }

//     // Retrieve alarms by a specific entity ID
//     public List<AlarmEntity> getAlarmsByEntityId(Integer entityId) {
//         return activeAlarms.values().stream()
//                 .filter(alarm -> alarm.getEntityId().equals(entityId))
//                 .collect(Collectors.toList());
//     }

//     // Generate a composite key for an alarm
//     private String generateKey(AlarmEntity alarm) {
//         return alarm.getEntityType() + ":" + alarm.getEntityId() + ":" + alarm.getKey(); // active alarm
//     }
// }
