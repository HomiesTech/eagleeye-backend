package com.homenetics.eagleeye.controller;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.homenetics.eagleeye.entity.AlarmEntity;
import com.homenetics.eagleeye.manager.AlarmManager;

@RestController
@RequestMapping("/eagleeye/alarms")
@CrossOrigin("*")
public class AlarmsController {
    
    @Autowired
    private AlarmManager alarmManager;

    private Logger logger = LoggerFactory.getLogger(AlarmsController.class);
    
    @GetMapping("")
    public ResponseEntity<List<AlarmEntity>> getAllAlarms() {
        try {
            List<AlarmEntity> alarms = this.alarmManager.getAllAlarms();
            return new ResponseEntity<>(alarms, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("An error occurred while fetching devices: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{devId}")
    public ResponseEntity<List<AlarmEntity>> getActiveAlarmsByDeviceId(@PathVariable("devId") Integer devId) {
        try {
            List<AlarmEntity> alarms = this.alarmManager.getAlarmsByEntityId(devId);
            return new ResponseEntity<>(alarms, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("An error occurred while fetching devices: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
}
