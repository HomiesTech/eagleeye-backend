package com.homenetics.eagleeye;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import com.homenetics.eagleeye.collector.database.DevicesCache;

@SpringBootApplication
@EnableScheduling
public class EagleeyeApplication {
	
	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(EagleeyeApplication.class, args);
		
		// run exactly once
		DevicesCache devicesCache = context.getBean(DevicesCache.class);
		devicesCache.getDBDevices();
	}

	@Bean
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}
}
