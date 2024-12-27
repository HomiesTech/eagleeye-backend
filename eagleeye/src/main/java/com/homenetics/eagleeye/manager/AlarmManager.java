package com.homenetics.eagleeye.manager;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

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
                    this.processMessageUrlResponseCode(device);
                    this.processBootTimeResponseCode(device);
                    this.processMessageDeliveryStatus(device);
                    this.processDeviceActiveState(device);
                    this.processSignalStrength(device);
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
            alarmEntity.setSeverity(SEVERITY_ERROR);
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
            // logger.info("[Existing Alarm] {} [Current Alarm] {}", exisitingAlarm, alarmEntity);
            if (!exisitingAlarm.getSeverity().equals(alarmEntity.getSeverity()) || !exisitingAlarm.getStatus().equals(alarmEntity.getStatus())) {
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
                // logger.info("Old alarm updated: {}", oldAlarmUpdate);
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
            alarmEntity.setStatus("not ok");
            alarmEntity.setDetail("Username is null");
            alarmEntity.setSeverity(SEVERITY_WARN);
        } else if (deviceEntity.getUsername() == deviceEntity.getSsid()) {
            alarmEntity.setStatus("not ok");
            alarmEntity.setDetail("Username is same as SSID" + ", Username = " + deviceEntity.getUsername() + ", SSID = " + deviceEntity.getSsid());
            alarmEntity.setSeverity(SEVERITY_WARN);
        } else {
            alarmEntity.setStatus("ok");
            alarmEntity.setDetail("Username is OK" + ", Username = " + deviceEntity.getUsername());
            alarmEntity.setSeverity(SEVERITY_OK);
        }

        if (exisitingAlarm != null) {
            // logger.info("[Existing Alarm] {} [Current Alarm] {}", exisitingAlarm, alarmEntity);
            if (!exisitingAlarm.getSeverity().equals(alarmEntity.getSeverity())) {
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
                // logger.info("Old alarm updated: {}", oldAlarmUpdate);
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
            // logger.info("[Existing Alarm] {} [Current Alarm] {}", exisitingAlarm, alarmEntity);
            if (!exisitingAlarm.getSeverity().equals(alarmEntity.getSeverity()) || !exisitingAlarm.getStatus().equals(alarmEntity.getStatus())) {
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
                // logger.info("Old alarm updated: {}", oldAlarmUpdate);
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
            // logger.info("[Existing Alarm] {} [Current Alarm] {}", exisitingAlarm, alarmEntity);
            if (!exisitingAlarm.getSeverity().equals(alarmEntity.getSeverity()) || !exisitingAlarm.getStatus().equals(alarmEntity.getStatus())) {
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
                // logger.info("Old alarm updated: {}", oldAlarmUpdate);
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

    private void processBootTimeResponseCode(DeviceDBEntity deviceEntity) {
        AlarmEntity alarmEntity = new AlarmEntity();
        alarmEntity.setEntityType("device");
        alarmEntity.setEntityId(deviceEntity.getDeviceId());
        alarmEntity.setKey("dev.boottime.code");

        AlarmsDBEntity exisitingAlarm = alarmsRepository.getActiveAlarm(alarmEntity.getEntityType(), alarmEntity.getEntityId(), alarmEntity.getKey());

        Integer responseCode = deviceEntity.getBoot_status_code();
        if (responseCode == null) {
            alarmEntity.setStatus("unknown");
            alarmEntity.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
            alarmEntity.setSeverity(SEVERITY_WARN);
        }
        else if (responseCode == -99 || responseCode == 200) {
            alarmEntity.setStatus("ok");
            alarmEntity.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
            alarmEntity.setSeverity(SEVERITY_OK);
        } else {
            alarmEntity.setStatus("error");
            alarmEntity.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
            alarmEntity.setSeverity(SEVERITY_ERROR);
        }

        if (exisitingAlarm != null) {
            // logger.info("[Existing Alarm] {} [Current Alarm] {}", exisitingAlarm, alarmEntity);
            if (!exisitingAlarm.getSeverity().equals(alarmEntity.getSeverity()) || !exisitingAlarm.getStatus().equals(alarmEntity.getStatus())) {
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
                // logger.info("Old alarm updated: {}", oldAlarmUpdate);
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

    private void processMessageUrlResponseCode(DeviceDBEntity deviceEntity) {
        AlarmEntity alarmEntity = new AlarmEntity();
        alarmEntity.setEntityType("device");
        alarmEntity.setEntityId(deviceEntity.getDeviceId());
        alarmEntity.setKey("dev.msgurl.code");

        AlarmsDBEntity exisitingAlarm = alarmsRepository.getActiveAlarm(alarmEntity.getEntityType(), alarmEntity.getEntityId(), alarmEntity.getKey());

        Integer responseCode = deviceEntity.getDownloadMqttUrlResponseCode();
        if (responseCode == null) {
            alarmEntity.setStatus("unknown");
            alarmEntity.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
            alarmEntity.setSeverity(SEVERITY_WARN);
        }
        else if (responseCode == -99 || responseCode == 200) {
            alarmEntity.setStatus("ok");
            alarmEntity.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
            alarmEntity.setSeverity(SEVERITY_OK);
        } else {
            alarmEntity.setStatus("error");
            alarmEntity.setDetail("HTTP Response Code = " + String.valueOf(responseCode));
            alarmEntity.setSeverity(SEVERITY_ERROR);
        }

        if (exisitingAlarm != null) {
            // logger.info("[Existing Alarm] {} [Current Alarm] {}", exisitingAlarm, alarmEntity);
            if (!exisitingAlarm.getSeverity().equals(alarmEntity.getSeverity()) || !exisitingAlarm.getStatus().equals(alarmEntity.getStatus())) {
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
                // logger.info("Old alarm updated: {}", oldAlarmUpdate);
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


    private void processSignalStrength(DeviceDBEntity deviceEntity) {
        AlarmEntity alarmEntity = new AlarmEntity();
        alarmEntity.setEntityType("device");
        alarmEntity.setEntityId(deviceEntity.getDeviceId());
        alarmEntity.setKey("dev.signal.strength");

        AlarmsDBEntity exisitingAlarm = alarmsRepository.getActiveAlarm(alarmEntity.getEntityType(), alarmEntity.getEntityId(), alarmEntity.getKey());
        
        if (deviceEntity.getSignalStrength() == null) {
            alarmEntity.setStatus("unknown");
            alarmEntity.setDetail("Signal Strength = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarmEntity.setSeverity(SEVERITY_WARN);
        } else if (deviceEntity.getSignalStrength() == 1 ) {
            alarmEntity.setStatus("excellent");
            alarmEntity.setDetail("Signal Strength (Excellent) = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarmEntity.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.getSignalStrength() == 2 ) {
            alarmEntity.setStatus("very good");
            alarmEntity.setDetail("Signal Strength (Very Good) = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarmEntity.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.getSignalStrength() == 3 ) {
            alarmEntity.setStatus("good");
            alarmEntity.setDetail("Signal Strength (Good) = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarmEntity.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.getSignalStrength() == 4 ) {
            alarmEntity.setStatus("fair");
            alarmEntity.setDetail("Signal Strength (Fair) = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarmEntity.setSeverity(SEVERITY_OK);
        } else if (deviceEntity.getSignalStrength() == 5 ) {
            alarmEntity.setStatus("bad");
            alarmEntity.setDetail("Signal Strength (Poor) = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarmEntity.setSeverity(SEVERITY_WARN);
        } else if (deviceEntity.getSignalStrength() == 6 ) {
            alarmEntity.setStatus("very bad");
            alarmEntity.setDetail("Signal Strength (Very Poor) = " + String.valueOf(deviceEntity.getSignalStrength()));
            alarmEntity.setSeverity(SEVERITY_ERROR);
        }

        if (exisitingAlarm != null) {
            // logger.info("[Existing Alarm] {} [Current Alarm] {}", exisitingAlarm, alarmEntity);
            if (!exisitingAlarm.getSeverity().equals(alarmEntity.getSeverity()) || !exisitingAlarm.getStatus().equals(alarmEntity.getStatus())) {
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
                // logger.info("Old alarm updated: {}", oldAlarmUpdate);
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