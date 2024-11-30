package com.homenetics.eagleeye.schedule;

import com.homenetics.eagleeye.entity.DeviceEntity;
import com.homenetics.eagleeye.merger.Devices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScheduleTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduleTasks.class);

    @Autowired
    private Devices devices;


}
