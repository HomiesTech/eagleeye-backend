package com.homenetics.eagleeye.service;

import com.homenetics.eagleeye.entity.APIEntity.DeviceCredEntity;
import com.homenetics.eagleeye.models.CustomerModel;
import com.homenetics.eagleeye.models.DeviceModelWrapper;
import com.homenetics.eagleeye.models.MqttDeviceModel;

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
     private static final String databaseServiceUrl = "http://192.168.29.20/database/api";
    // private static final String databaseServiceUrl = "http://172.19.0.2/database/api";
    // private static final String databaseServiceUrl = "http://192.168.29.49/database/api";
    @Autowired
    private RestTemplate restTemplate;

    private static final Logger logger = LoggerFactory.getLogger(DatabaseService.class);

    @SuppressWarnings("null")
    public List<CustomerModel> getAllCustomers() {
        String finalApi = databaseServiceUrl + "/users";
        try {
            long startTime = System.currentTimeMillis();
            ResponseEntity<List<CustomerModel>> response = restTemplate.exchange(finalApi, HttpMethod.GET, null, new ParameterizedTypeReference<List<CustomerModel>>() {});
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Customers api response time {} ms", duration);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                logger.error("getAllCustomers, Response Not Successful. {}", response.getBody().toString());
                return Collections.emptyList();
            }
        } catch (Exception e) {
            logger.error("Error occurred while fetching all customers {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("null")
    public DeviceModelWrapper getAllDevices(Integer page, Integer pageSize) {
        String finalApi = databaseServiceUrl + "/devices?page=" + page + "&pageSize=" + pageSize;
        try {
            long startTime = System.currentTimeMillis();
            ResponseEntity<DeviceModelWrapper> response = restTemplate.exchange(finalApi, HttpMethod.GET, null, new ParameterizedTypeReference<DeviceModelWrapper>() {});
            long duration = System.currentTimeMillis() - startTime;
            logger.info("Devices api response time {} ms.", duration);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                logger.error("getAllDevices, Response Not Successful. {}", response.getBody().toString());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error occurred while fetching all devices {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("null")
    public DeviceCredEntity getDeviceCredById(Integer id) {
        String finalApi = databaseServiceUrl + "/devices/" + id + "/credentials";
        try {
            ResponseEntity<DeviceCredEntity> response = restTemplate.exchange(finalApi, HttpMethod.GET, null, DeviceCredEntity.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                logger.error("getDeviceCredById, Response Not Successful. {}", response.getBody().toString());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error occurred while fetching device {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("null")
    public MqttDeviceModel getDeviceOnlineDBStatus(Integer devId) {
        String finalApi = databaseServiceUrl + "/mqtt-devices/device/" + String.valueOf(devId);
        try {
            ResponseEntity<MqttDeviceModel> response = restTemplate.exchange(finalApi, HttpMethod.GET, null, MqttDeviceModel.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                logger.error("getDeviceOnlineDBStatus, Response Not Successful. {}", response.getBody().toString());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error occurred while fetching device {}", e.getMessage());
            return null;
        }
    }

}
