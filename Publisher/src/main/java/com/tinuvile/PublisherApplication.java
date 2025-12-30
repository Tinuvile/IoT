package com.tinuvile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * IoT数据发布者应用
 * 
 * @author tinuvile
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class PublisherApplication {

    public static void main(String[] args) {
        SpringApplication.run(PublisherApplication.class, args);
        System.out.println("IoT Publisher Application Started!");
        System.out.println("Web Interface: http://localhost:8081");
        System.out.println("Management: http://localhost:8081/actuator");
    }
}