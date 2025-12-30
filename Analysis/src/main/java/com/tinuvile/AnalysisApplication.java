package com.tinuvile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * IoT数据分析应用
 * 
 * @author tinuvile
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class AnalysisApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalysisApplication.class, args);
        System.out.println("🚀 IoT Analysis Application Started!");
        System.out.println("📡 Web Interface: http://localhost:8083");
        System.out.println("📊 Management: http://localhost:8083/actuator");
    }
}