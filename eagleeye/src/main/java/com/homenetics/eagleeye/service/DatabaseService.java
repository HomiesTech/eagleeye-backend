package com.homenetics.eagleeye.service;

import com.homenetics.eagleeye.models.DeviceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

@Service
public class DatabaseService {
    private static final String databaseServiceUrl = "http://172.16.0.10/database/api";

    @Autowired
    private RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    public List<DeviceModel> getAllDevices() {
        String finalApi = databaseServiceUrl + "/devices";
        try {
            long startTime = System.currentTimeMillis();
            ResponseEntity<List<DeviceModel>> response = restTemplate.exchange(finalApi, HttpMethod.GET, null, new ParameterizedTypeReference<List<DeviceModel>>() {});
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Devices api response time {} ms.", duration);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                logger.error("getAllDevices, Response Not Successful. {}", response.getBody().toString());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("Error occurred while fetching all devices {}", e.getMessage());
            return Collections.emptyList();
        }
    }

}
