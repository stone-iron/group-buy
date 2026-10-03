package com.livegroupbuy.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class LiveGroupBuyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(LiveGroupBuyBackendApplication.class, args);
    }
}
