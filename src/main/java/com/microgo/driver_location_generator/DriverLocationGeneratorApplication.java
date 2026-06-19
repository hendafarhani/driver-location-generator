package com.microgo.driver_location_generator;

import com.microgo.driver_location_generator.config.DriverLocationGeneratorProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(DriverLocationGeneratorProperties.class)
public class DriverLocationGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DriverLocationGeneratorApplication.class, args);
    }
}
