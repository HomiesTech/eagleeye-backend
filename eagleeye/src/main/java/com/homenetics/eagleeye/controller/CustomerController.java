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

import com.homenetics.eagleeye.collector.database.CustomersCache;
import com.homenetics.eagleeye.models.CustomerModel;

@RestController
@RequestMapping("/eagleeye/customers")
@CrossOrigin("*")
public class CustomerController {
    
    @Autowired
    private CustomersCache customers;

    private Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @GetMapping("")
    public ResponseEntity<List<CustomerModel>> getAllCustomers() {
        try {
            List<CustomerModel> allcustomers = this.customers.getAllCustomers();
            return new ResponseEntity<>(allcustomers, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("An error occurred while fetching devices: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerModel> getCustomerById(@PathVariable("id") Integer id) {
        try {
            CustomerModel customer = this.customers.getCustomerById(id);
            return new ResponseEntity<>(customer, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("An error occurred while fetching devices: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<CustomerModel> getCustomerByEmail(@PathVariable("email") String email) {
        try {
            CustomerModel customer = this.customers.getCustomerByEmail(email);
            return new ResponseEntity<>(customer, HttpStatus.OK);
        } catch (Exception e) {
            logger.error("An error occurred while fetching devices: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
