package com.homenetics.eagleeye.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.homenetics.eagleeye.entity.DeviceDBEntity;
import com.homenetics.eagleeye.repository.DeviceRepository;

public class DeviceService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    Logger logger = LoggerFactory.getLogger(DeviceService.class);

    public boolean addDevice(DeviceDBEntity device) {
        try {
            deviceRepository.save(device);
            return true;
        } catch (Exception e) {
            logger.error("Error occurred while adding device {}", e.getMessage());
            return false;
        }
    }
}
