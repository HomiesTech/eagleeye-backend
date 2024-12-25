package com.homenetics.eagleeye.collector.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.homenetics.eagleeye.entity.KafkaEntity.KafkaMessageEntity;
import com.homenetics.eagleeye.models.DeviceModel;
import com.homenetics.eagleeye.models.MqttDeviceModel;
import com.homenetics.eagleeye.repository.DeviceRepository;
import com.homenetics.eagleeye.util.ObjectMapperProvider;

@Service
public class KafkaConsumer {

    Logger logger = LoggerFactory.getLogger(KafkaConsumer.class);

    @Autowired
    private DeviceRepository deviceRepository;

    @KafkaListener(topics = "devices", groupId = "group_id")
    public void consumeDevices(String message) {
        logger.info("[KafkaConsumer] Consumed message: " + message); // TODO - Change to debug
        // Consumed message: {"action":"create","device":{"devId":24,"userId":null,"macAddress":"00:11B:44:11:3A:B7","createdAt":"2024-12-20T19:27:37.443Z","updatedAt":"2024-12-20T19:27:37.443Z","lastConnectionAt":null}}

        ObjectMapper objectMapper = ObjectMapperProvider.getObjectMapper();
        try {
            KafkaMessageEntity<DeviceModel> kafkaMessage = objectMapper.readValue(message, 
                                            objectMapper.getTypeFactory().constructParametricType(KafkaMessageEntity.class, DeviceModel.class));
            
            String action = kafkaMessage.getAction();
            DeviceModel device = kafkaMessage.getData();

            if ("create".equals(action) || "update".equals(action)) {
                deviceRepository.upsertDBDevice(device.getDevId(),device.getMacAddress(),device.getSsid(),device.getUserId(),device.getCreatedAt(),device.getUpdatedAt());
            } else if ("delete".equals(action)) {
                deviceRepository.deleteDeviceById(device.getDevId());
            } else {
                logger.error("[KafkaConsumer] Invalid action: " + action);
            }

        } catch (JsonMappingException e) {
            logger.error("[KafkaConsumer] Error while mapping message to KafkaMessageEntity: " + e.getMessage() + " | " + e.getStackTrace());
        } catch (JsonProcessingException e) {
            logger.error("[KafkaConsumer] Error while processing message: " + e.getMessage() + " | " + e.getStackTrace());
            e.printStackTrace();
        } catch (Exception e) {
            logger.error("[KafkaConsumer] Error: " + e.getMessage() + " | " + e.getStackTrace());
        }
    }

    @KafkaListener(topics = "mqtt-dev-connection", groupId = "group_id")
    public void consumeMqttDeviceConnection(String message) {
        logger.info("[KafkaConsumer] Consumed message: " + message); // TODO - Change to debug

        ObjectMapper objectMapper = ObjectMapperProvider.getObjectMapper();
        try {
            KafkaMessageEntity<MqttDeviceModel> kafkaMessage = objectMapper.readValue(message,
                                                objectMapper.getTypeFactory().constructParametricType(KafkaMessageEntity.class, MqttDeviceModel.class));
            String action = kafkaMessage.getAction();
            MqttDeviceModel mqttDevice = kafkaMessage.getData();

            if ("create".equals(action) || "update".equals(action)) {
                deviceRepository.upsertDBMqttConnection(mqttDevice.getDevId(), mqttDevice.getStatus());
            } else if ("delete".equals(action)) {
                deviceRepository.deleteDeviceById(mqttDevice.getDevId());
                logger.error("[KafkaConsumer] Invalid action: " + action);
            }
        } catch (Exception e) {
            logger.error("[KafkaConsumer] Error: " + e.getMessage() + " | " + e.getStackTrace());
        }
    }
}
