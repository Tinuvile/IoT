package com.tinuvile.controller;

import com.tinuvile.model.AnalysisResult;
import com.tinuvile.model.ReceivedData;
import com.tinuvile.model.SensorType;
import com.tinuvile.model.SubscriberStatus;
import com.tinuvile.service.DataAnalysisService;
import com.tinuvile.service.DataStorageService;
import com.tinuvile.service.MqttSubscriberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 订阅者REST API控制器
 * 
 * @author tinuvile
 */
@RestController
@RequestMapping("/api/subscriber")
@CrossOrigin(origins = "*")
public class SubscriberController {
    
    private static final Logger logger = LoggerFactory.getLogger(SubscriberController.class);
    
    @Autowired
    private MqttSubscriberService mqttSubscriberService;
    
    @Autowired
    private DataStorageService dataStorageService;
    
    @Autowired
    private DataAnalysisService dataAnalysisService;
    
    /**
     * 开始订阅数据
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startSubscribing() {
        try {
            logger.info("接收到开始订阅请求");
            boolean success = mqttSubscriberService.startSubscribing().get(8, TimeUnit.SECONDS);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "订阅服务已启动" : "订阅服务启动失败");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (TimeoutException e) {
            logger.warn("启动订阅服务超时（可能是MQTT Broker未启动或网络不通）");
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "启动订阅超时：请检查MQTT Broker是否已启动，以及订阅端MQTT连接参数是否正确");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("启动订阅服务失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "启动订阅服务失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 停止订阅数据
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopSubscribing() {
        try {
            logger.info("接收到停止订阅请求");
            boolean success = mqttSubscriberService.stopSubscribing().get(8, TimeUnit.SECONDS);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            response.put("message", success ? "订阅服务已停止" : "订阅服务停止失败");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (TimeoutException e) {
            logger.warn("停止订阅服务超时");
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "停止订阅超时：请稍后重试");
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("停止订阅服务失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "停止订阅服务失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取订阅状态
     */
    @GetMapping("/status")
    public ResponseEntity<SubscriberStatus> getSubscriberStatus() {
        try {
            SubscriberStatus status = mqttSubscriberService.getSubscriberStatus();
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("获取订阅状态失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取系统信息
     */
    @GetMapping("/system-info")
    public ResponseEntity<SystemInfo> getSystemInfo() {
        try {
            SystemInfo info = new SystemInfo();
            
            // MQTT连接信息
            info.setMqttConnected(mqttSubscriberService.isConnected());
            info.setSubscribing(mqttSubscriberService.isSubscribing());
            info.setMqttConnectionInfo(mqttSubscriberService.getConnectionInfo());
            
            // 数据存储信息
            DataStorageService.StorageStatistics storageStats = dataStorageService.getStatistics();
            info.setTotalDataCount(storageStats.getTotalStoredCount());
            info.setTemperatureCount(storageStats.getTemperatureCount());
            info.setHumidityCount(storageStats.getHumidityCount());
            info.setPressureCount(storageStats.getPressureCount());
            
            return ResponseEntity.ok(info);
            
        } catch (Exception e) {
            logger.error("获取系统信息失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取最新数据
     */
    @GetMapping("/data/latest")
    public ResponseEntity<Map<String, List<ReceivedData>>> getLatestData(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {
        try {
            Map<SensorType, List<ReceivedData>> data = dataStorageService.getAllLatestData(limit);
            
            Map<String, List<ReceivedData>> response = new HashMap<>();
            data.forEach((type, list) -> response.put(type.getCode(), list));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取最新数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取指定类型的最新数据
     */
    @GetMapping("/data/latest/{sensorType}")
    public ResponseEntity<List<ReceivedData>> getLatestDataByType(
            @PathVariable String sensorType,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        try {
            SensorType type = SensorType.fromCode(sensorType);
            List<ReceivedData> data = dataStorageService.getLatestData(type, limit);
            
            return ResponseEntity.ok(data);
            
        } catch (IllegalArgumentException e) {
            logger.warn("无效的传感器类型: {}", sensorType);
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("获取最新数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取时间范围内的数据
     */
    @GetMapping("/data/range/{sensorType}")
    public ResponseEntity<List<ReceivedData>> getDataInTimeRange(
            @PathVariable String sensorType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            SensorType type = SensorType.fromCode(sensorType);
            List<ReceivedData> data = dataStorageService.getDataInTimeRange(type, startTime, endTime);
            
            return ResponseEntity.ok(data);
            
        } catch (IllegalArgumentException e) {
            logger.warn("无效的传感器类型: {}", sensorType);
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("获取时间范围数据失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取数据分析结果
     */
    @GetMapping("/analysis")
    public ResponseEntity<Map<String, AnalysisResult>> getAllAnalysisResults() {
        try {
            Map<SensorType, AnalysisResult> results = dataAnalysisService.getAllAnalysisResults();
            
            Map<String, AnalysisResult> response = new HashMap<>();
            results.forEach((type, result) -> response.put(type.getCode(), result));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取分析结果失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取指定类型的分析结果
     */
    @GetMapping("/analysis/{sensorType}")
    public ResponseEntity<AnalysisResult> getAnalysisResult(@PathVariable String sensorType) {
        try {
            SensorType type = SensorType.fromCode(sensorType);
            AnalysisResult result = dataAnalysisService.getAnalysisResult(type);
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("无效的传感器类型: {}", sensorType);
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("获取分析结果失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取时间范围内的分析结果
     */
    @GetMapping("/analysis/{sensorType}/range")
    public ResponseEntity<AnalysisResult> getTimeRangeAnalysis(
            @PathVariable String sensorType,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        try {
            SensorType type = SensorType.fromCode(sensorType);
            AnalysisResult result = dataAnalysisService.getTimeRangeAnalysis(type, startTime, endTime);
            
            return ResponseEntity.ok(result);
            
        } catch (IllegalArgumentException e) {
            logger.warn("无效的传感器类型: {}", sensorType);
            return ResponseEntity.badRequest().build();
            
        } catch (Exception e) {
            logger.error("获取时间范围分析失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 强制刷新分析结果
     */
    @PostMapping("/analysis/refresh")
    public ResponseEntity<Map<String, Object>> refreshAnalysis() {
        try {
            logger.info("接收到刷新分析请求");
            dataAnalysisService.clearCache();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "分析结果已刷新");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("刷新分析结果失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "刷新分析结果失败: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 获取存储统计信息
     */
    @GetMapping("/storage/statistics")
    public ResponseEntity<DataStorageService.StorageStatistics> getStorageStatistics() {
        try {
            DataStorageService.StorageStatistics stats = dataStorageService.getStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取存储统计失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 清空所有数据
     */
    @PostMapping("/storage/clear")
    public ResponseEntity<Map<String, Object>> clearAllData() {
        try {
            logger.info("接收到清空数据请求");
            dataStorageService.clearAllData();
            dataAnalysisService.clearCache();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "所有数据已清空");
            response.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("清空数据失败: {}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "清空数据失败: " + e.getMessage());
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
        health.put("application", "IoT Subscriber");
        health.put("timestamp", System.currentTimeMillis());
        
        try {
            health.put("mqtt_connected", mqttSubscriberService.isConnected());
            health.put("subscribing", mqttSubscriberService.isSubscribing());
            
            DataStorageService.StorageStatistics storageStats = dataStorageService.getStatistics();
            health.put("total_data_count", storageStats.getTotalStoredCount());
            
        } catch (Exception e) {
            health.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * 系统信息模型
     */
    public static class SystemInfo {
        private boolean mqttConnected;
        private boolean subscribing;
        private MqttSubscriberService.ConnectionInfo mqttConnectionInfo;
        private int totalDataCount;
        private int temperatureCount;
        private int humidityCount;
        private int pressureCount;
        
        // Getters and Setters
        public boolean isMqttConnected() { return mqttConnected; }
        public void setMqttConnected(boolean mqttConnected) { this.mqttConnected = mqttConnected; }
        
        public boolean isSubscribing() { return subscribing; }
        public void setSubscribing(boolean subscribing) { this.subscribing = subscribing; }
        
        public MqttSubscriberService.ConnectionInfo getMqttConnectionInfo() { return mqttConnectionInfo; }
        public void setMqttConnectionInfo(MqttSubscriberService.ConnectionInfo mqttConnectionInfo) { 
            this.mqttConnectionInfo = mqttConnectionInfo; 
        }
        
        public int getTotalDataCount() { return totalDataCount; }
        public void setTotalDataCount(int totalDataCount) { this.totalDataCount = totalDataCount; }
        
        public int getTemperatureCount() { return temperatureCount; }
        public void setTemperatureCount(int temperatureCount) { this.temperatureCount = temperatureCount; }
        
        public int getHumidityCount() { return humidityCount; }
        public void setHumidityCount(int humidityCount) { this.humidityCount = humidityCount; }
        
        public int getPressureCount() { return pressureCount; }
        public void setPressureCount(int pressureCount) { this.pressureCount = pressureCount; }
    }
}
