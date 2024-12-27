package com.homenetics.eagleeye.controller;

import com.homenetics.eagleeye.entity.DBEntity.DeviceDBEntity;
import com.homenetics.eagleeye.repository.DeviceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/eagleeye/devices")
@CrossOrigin("*")
public class DevicesController {

    @Autowired
    private DeviceRepository deviceRepository;

    private static final Logger logger = LoggerFactory.getLogger(DevicesController.class);

    // @GetMapping("")
    // public ResponseEntity<?> getAllDevices(
    //     @RequestParam(value = "page", defaultValue = "0") Integer page,
    //     @RequestParam(value = "size", defaultValue = "10") Integer size,
    //     @RequestParam(value = "sortFields", defaultValue = "") List<String> sortFields,
    //     @RequestParam(value = "sortOrders", defaultValue = "") List<String> sortOrders
    // ) {
    //     try {

    //         // Validate sort fields and orders
    //         if (sortFields.size() != sortOrders.size()) {
    //             return new ResponseEntity<>("Mismatch between sort fields and orders", HttpStatus.BAD_REQUEST);
    //         }

    //         // Build sorting configuration
    //         List<Sort.Order> orders = new ArrayList<>();
    //         for (int i = 0; i < sortFields.size(); i++) {
    //             String field = sortFields.get(i);
    //             String order = sortOrders.get(i).toLowerCase();
    //             orders.add("desc".equals(order) ? Sort.Order.desc(field) : Sort.Order.asc(field));
    //         }

    //         Sort sort = orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
    //         PageRequest pageRequest = PageRequest.of(page, size, sort);

    //         Page<DeviceDBEntity> devicesPage = this.deviceRepository.findAll(pageRequest);
    //         return new ResponseEntity<>(devicesPage, HttpStatus.OK);
    //     } catch (Exception e) {
    //         logger.error("An error occurred while fetching devices: {}", e.getMessage(), e);
    //         return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
    //     }
    // }

    @GetMapping("")
    public ResponseEntity<?> getAllAlarms(
        @RequestParam(value = "page", defaultValue = "0") Integer page,
        @RequestParam(value = "size", defaultValue = "10") Integer size,
        @RequestParam(value = "sortFields", defaultValue = "") List<String> sortFields,
        @RequestParam(value = "sortOrders", defaultValue = "") List<String> sortOrders,
        @RequestParam(value = "activeState", defaultValue = "1") Boolean activeState, // Filter by active state
        @RequestParam Map<String, String> filters // Catch all additional filter parameters
    ) {
        try {
            // Validate sort fields and orders
            if (sortFields.size() != sortOrders.size()) {
                return new ResponseEntity<>("Mismatch between sort fields and orders", HttpStatus.BAD_REQUEST);
            }

            // Build sorting configuration
            List<Sort.Order> orders = new ArrayList<>();
            for (int i = 0; i < sortFields.size(); i++) {
                String field = sortFields.get(i);
                String order = sortOrders.get(i).toLowerCase();
                orders.add("desc".equals(order) ? Sort.Order.desc(field) : Sort.Order.asc(field));
            }

            Sort sort = orders.isEmpty() ? Sort.unsorted() : Sort.by(orders);
            PageRequest pageRequest = PageRequest.of(page, size, sort);

            // Build dynamic speicifications
            Specification<DeviceDBEntity> spec = Specification.where(null);
            // Add state filter if provided
            if (activeState != null) {
                spec = spec.and((root, query, criteiraBuilder) -> 
                    criteiraBuilder.equal(root.get("activeState"), activeState)
                );
            }

            // Add dynamic filters
            for (Map.Entry<String, String> entry : filters.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                spec = spec.and((root, query, criteiraBuilder) -> 
                    criteiraBuilder.equal(root.get(key), value)
                );
            }
            // Fetch alarms with dynamic filtering
            Page<DeviceDBEntity> alarmsPage = deviceRepository.findAll(spec, pageRequest);
            return new ResponseEntity<>(alarmsPage, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("An error occurred while fetching alarms: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @GetMapping("/{id}")
    public ResponseEntity<DeviceDBEntity> getDeviceById(@PathVariable("id") String id) {
        try {
            DeviceDBEntity device = this.deviceRepository.findById(Integer.valueOf(id)).orElse(null);
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
