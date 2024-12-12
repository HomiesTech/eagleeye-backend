package com.homenetics.eagleeye.collector.database;

import com.homenetics.eagleeye.models.CustomerModel;
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
    private static final ConcurrentHashMap<String, CustomerModel> customersCache = new ConcurrentHashMap<>();
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
                ConcurrentHashMap<String, CustomerModel> newCache = new ConcurrentHashMap<>();
                for (CustomerModel customer : customers) {
                    customer.setCode();
                    // Use one instance for multiple keys
                    String customerIdKey = buildKey("id", String.valueOf(customer.getId()));
                    String customerCodeKey = buildKey("code", customer.getCode());
                    String customerEmailKey = buildKey("email", customer.getEmail());

                    // Add keys pointing to the same customer object
                    newCache.put(customerIdKey, customer);
                    if (customer.getCode() != null && !customer.getCode().isEmpty()) {
                        newCache.put(customerCodeKey, customer);
                    }
                    if (customer.getEmail() != null && !customer.getEmail().isEmpty()) {
                        newCache.put(customerEmailKey, customer);
                    }
                }
                synchronized (customersCache) {
                    customersCache.clear();
                    customersCache.putAll(newCache);
                }
                long duration = System.currentTimeMillis() - startTime;
                logger.info("Customer cache refreshed successfully. Total Customers: {} loaded in {} ms.", customers.size(), duration);
            } else {
                logger.warn("No data returned from the database during cache refresh.");
            }
        } catch (Exception e) {
            logger.error("Error while refreshing customers cache: {}", e.getMessage(), e);
        }
    }

    public CustomerModel getCustomerById(int id) {
        return customersCache.get(buildKey("id", String.valueOf(id)));
    }

    public CustomerModel getCustomerByCode(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        return customersCache.get(buildKey("code", code));
    }

    public CustomerModel getCustomerByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        return customersCache.get(buildKey("email", email));
    }

    public List<CustomerModel> getAllCustomers() {
        return customersCache.values()
                .stream()
                .distinct()
                .toList();
    }

    private String buildKey(String type, String value) {
        return type + ":" + value; // Generate unique keys for each type
    }
}
