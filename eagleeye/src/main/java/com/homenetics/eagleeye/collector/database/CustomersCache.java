package com.homenetics.eagleeye.collector.database;

import com.homenetics.eagleeye.models.CustomerModel;
import com.homenetics.eagleeye.models.DeviceModel;
import com.homenetics.eagleeye.service.DatabaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CustomersCache {
    private static final ConcurrentHashMap<Integer, CustomerModel> customersCacheById =  new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> customerCodeToId = new ConcurrentHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(CustomersCache.class);
    private static long refreshCycleCount = 0;

    @Autowired
    private DatabaseService databaseService;

    @Scheduled(fixedRate = 60000)
    public void refreshCache() {
        long startTime = System.currentTimeMillis();
        refreshCycleCount++;
        logger.info("Starting customers cache refresh. Cycle count: {}", refreshCycleCount);
        try {
            List<CustomerModel> customers = databaseService.getAllCustomers();
            if (customers != null) {
                ConcurrentHashMap<Integer, CustomerModel> newCache = new ConcurrentHashMap<>();
                ConcurrentHashMap<String, Integer> newCodeToIdCache = new ConcurrentHashMap<>();
                for (CustomerModel customer : customers) {
                    newCache.put(customer.getId(), customer);
                    String customerCode = customer.getCode();
                    if (customerCode == null || customerCode.isEmpty()) {
                        customer.setCode();
                    }
                    newCodeToIdCache.put(customer.getCode(), customer.getId());
                }
                synchronized (customersCacheById) {
                    customersCacheById.clear();
                    customersCacheById.putAll(newCache);
                }
                synchronized (customerCodeToId) {
                    customerCodeToId.clear();
                    customerCodeToId.putAll(newCodeToIdCache);
                }
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Customer cache refreshed successfully. Total Customers: {} loaded in {} ms.", customers.size(), duration);
            }else {
                logger.warn("No data returned from the database during cache refresh.");
            }
        } catch (Exception e) {
            logger.error("Error while refreshing customers cache: {}", e.getMessage(), e);
        }
    }

    public CustomerModel getCustomerById(int id) {
        return customersCacheById.get(Integer.valueOf(id));
    }

    public CustomerModel getCustomerByCode(String code) {
        Integer customerId = customerCodeToId.get(code);
        if (customerId == null) {
            logger.warn("No customer found with code {} in {}", code, customerCodeToId.keySet());
            return null;
        }
        return this.getCustomerById(customerId);
    }

    public List<CustomerModel> getAllCustomers() {
        return List.copyOf(customersCacheById.values());
    }

}

