package com.homenetics.eagleeye.controller;

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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.homenetics.eagleeye.entity.DBEntity.AlarmsDBEntity;
import com.homenetics.eagleeye.repository.AlarmsRepository;

@RestController
@RequestMapping("/eagleeye/alarms")
@CrossOrigin("*")
public class AlarmsController {
    
    @Autowired
    private AlarmsRepository alarmsRepository;

    private Logger logger = LoggerFactory.getLogger(AlarmsController.class);
    
    @GetMapping("")
    public ResponseEntity<?> getAllAlarms(
        @RequestParam(value = "page", defaultValue = "0") Integer page,
        @RequestParam(value = "size", defaultValue = "10") Integer size,
        @RequestParam(value = "sortFields", defaultValue = "") List<String> sortFields,
        @RequestParam(value = "sortOrders", defaultValue = "") List<String> sortOrders,
        @RequestParam(value = "state", defaultValue = "1") Boolean state, // Filter by active state
        @RequestParam(value = "severity", required = false) List<Integer> severity, // Filter by multiple severities
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
            Specification<AlarmsDBEntity> spec = Specification.where(null);
            // Add state filter if provided
            if (state != null) {
                spec = spec.and((root, query, criteiraBuilder) -> 
                    criteiraBuilder.equal(root.get("state"), state)
                );
            }

            // Add severity filter if provided
            if (severity != null && !severity.isEmpty()) {
                spec = spec.and((root, query, criteiraBuilder) -> 
                    criteiraBuilder.in(root.get("severity")).value(severity)
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
            Page<AlarmsDBEntity> alarmsPage = alarmsRepository.findAll(spec, pageRequest);
            return new ResponseEntity<>(alarmsPage, HttpStatus.OK);
            
        } catch (Exception e) {
            logger.error("An error occurred while fetching alarms: {}", e.getMessage(), e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<AlarmsDBEntity> getDeviceById(@PathVariable("id") String id) {
        try {
            AlarmsDBEntity device = this.alarmsRepository.findById(Integer.valueOf(id)).orElse(null);
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
