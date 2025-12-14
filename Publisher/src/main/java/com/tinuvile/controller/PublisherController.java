package com.tinuvile.controller;

import com.tinuvile.model.PublishStatus;
import com.tinuvile.model.SensorType;
import com.tinuvile.service.PublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 发布者REST API控制器
 * 
 * @author tinuvile
 */
@RestController
@RequestMapping("/api/publisher")
@CrossOrigin(origins = "*")
public class PublisherController {
    
    private static final Logger logger = LoggerFactory.getLogger(PublisherController.class);
    
    @Autowired
    private PublisherService publisherService;
    
    /**
     * 开始发布数据
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startPublishing() {
        try {
            logger.info("接收到开始发布请求");
            publisherService.startPublishing();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "发布服务已启动");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("启动发布服务失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "启动发布服务失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 停止发布数据
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopPublishing() {
        try {
            logger.info("接收到停止发布请求");
            publisherService.stopPublishing();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "发布服务已停止");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("停止发布服务失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "停止发布服务失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取发布状态
     */
    @GetMapping("/status")
    public ResponseEntity<PublishStatus> getPublishStatus() {
        try {
            PublishStatus status = publisherService.getPublishStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("获取发布状态失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取系统信息
     */
    @GetMapping("/system-info")
    public ResponseEntity<PublisherService.SystemInfo> getSystemInfo() {
        try {
            PublisherService.SystemInfo info = publisherService.getSystemInfo();
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            logger.error("获取系统信息失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 手动发布单条数据
     */
    @PostMapping("/publish/{sensorType}")
    public ResponseEntity<Map<String, Object>> publishSingleData(@PathVariable String sensorType) {
        try {
            SensorType type = SensorType.fromCode(sensorType);
            logger.info("接收到手动发布请求: {}", type.getDisplayName());
            
            CompletableFuture<Boolean> result = publisherService.publishSingleData(type);
            boolean success = result.get();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? 
                String.format("成功发布一条%s数据", type.getDisplayName()) : 
                String.format("发布%s数据失败", type.getDisplayName()));
            response.put("sensor_type", type.getCode());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            logger.warn("无效的传感器类型: {}", sensorType);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "无效的传感器类型: " + sensorType);
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.badRequest().body(response);
            
        } catch (Exception e) {
            logger.error("手动发布数据失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "发布数据失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 重置统计数据
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetStatistics() {
        try {
            logger.info("接收到重置统计请求");
            publisherService.resetStatistics();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "统计数据已重置");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("重置统计数据失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "重置统计数据失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("application", "IoT Publisher");
        health.put("timestamp", System.currentTimeMillis());
        
        try {
            PublisherService.SystemInfo systemInfo = publisherService.getSystemInfo();
            health.put("mqtt_connected", systemInfo.isMqttConnected());
            health.put("data_files_exist", systemInfo.isDataFilesExist());
            health.put("total_data_count", systemInfo.getTotalDataCount());
        } catch (Exception e) {
            health.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
}