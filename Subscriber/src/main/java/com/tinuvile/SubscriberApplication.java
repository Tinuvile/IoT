package com.tinuvile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * IoT数据订阅者应用
 * 
 * @author tinuvile
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class SubscriberApplication {

    public static void main(String[] args) {
        SpringApplication.run(SubscriberApplication.class, args);
        System.out.println("IoT Subscriber Application Started!");
        System.out.println("Web Interface: http://localhost:8082");
        System.out.println("Management: http://localhost:8082/actuator");
    }
}