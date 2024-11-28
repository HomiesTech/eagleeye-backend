package com.homenetics.eagleeye.controller;


import com.homenetics.eagleeye.entity.DeviceEntity;
import com.homenetics.eagleeye.merger.Devices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/eagleeye/devices")
@CrossOrigin("*")
public class DevicesController {

    @Autowired
    private Devices devices;

    private static final Logger logger = LoggerFactory.getLogger(DevicesController.class);

    @GetMapping("")
    public ResponseEntity<List<DeviceEntity>> getAllDevices() {
        try {
            List<DeviceEntity> devices = this.devices.getAllDevices();
            return new ResponseEntity<>(devices, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("An error occurred while fetching devices: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceEntity> getDeviceById(@PathVariable("id") String id) {
        try {
            DeviceEntity device = this.devices.getDeviceById(Integer.valueOf(id));
            if (device == null) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }else {
                return new ResponseEntity<>(device, HttpStatus.OK);
            }
        } catch (Exception e) {
            logger.error("An error occurred while fetching devices: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
