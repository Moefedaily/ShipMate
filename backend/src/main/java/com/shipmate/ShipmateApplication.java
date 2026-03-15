package com.shipmate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ShipmateApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShipmateApplication.class, args);
    }

}