package com.tinuvile.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 接收到的传感器数据模型
 * 
 * @author tinuvile
 */
public class ReceivedData {
    
    @JsonProperty("id")
    private Long id;
    
    @JsonProperty("received_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime receivedTime;
    
    @JsonProperty("original_data")
    private SensorData originalData;
    
    @JsonProperty("processed")
    private boolean processed = false;
    
    @JsonProperty("processing_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime processingTime;
    
    @JsonProperty("mqtt_topic")
    private String mqttTopic;
    
    @JsonProperty("quality_score")
    private Double qualityScore;
    
    @JsonProperty("anomaly_detected")
    private boolean anomalyDetected = false;
    
    public ReceivedData() {
        this.receivedTime = LocalDateTime.now();
    }
    
    public ReceivedData(SensorData originalData, String mqttTopic) {
        this();
        this.originalData = originalData;
        this.mqttTopic = mqttTopic;
        this.id = generateId();
    }
    
    private Long generateId() {
        // 简单的ID生成策略：时间戳 + 传感器类型hashCode
        return System.currentTimeMillis() * 1000 + 
               (originalData != null ? originalData.getSensorType().hashCode() : 0);
    }
    
    // 便利方法获取传感器数据属性
    public SensorType getSensorType() {
        return originalData != null ? originalData.getSensorType() : null;
    }
    
    public Double getValue() {
        return originalData != null ? originalData.getValue() : null;
    }
    
    public String getUnit() {
        return originalData != null ? originalData.getUnit() : null;
    }
    
    public LocalDateTime getOriginalTimestamp() {
        return originalData != null ? originalData.getTimestamp() : null;
    }
    
    public String getLocation() {
        return originalData != null ? originalData.getLocation() : null;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public LocalDateTime getReceivedTime() {
        return receivedTime;
    }
    
    public void setReceivedTime(LocalDateTime receivedTime) {
        this.receivedTime = receivedTime;
    }
    
    public SensorData getOriginalData() {
        return originalData;
    }
    
    public void setOriginalData(SensorData originalData) {
        this.originalData = originalData;
    }
    
    public boolean isProcessed() {
        return processed;
    }
    
    public void setProcessed(boolean processed) {
        this.processed = processed;
        if (processed && processingTime == null) {
            this.processingTime = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getProcessingTime() {
        return processingTime;
    }
    
    public void setProcessingTime(LocalDateTime processingTime) {
        this.processingTime = processingTime;
    }
    
    public String getMqttTopic() {
        return mqttTopic;
    }
    
    public void setMqttTopic(String mqttTopic) {
        this.mqttTopic = mqttTopic;
    }
    
    public Double getQualityScore() {
        return qualityScore;
    }
    
    public void setQualityScore(Double qualityScore) {
        this.qualityScore = qualityScore;
    }
    
    public boolean isAnomalyDetected() {
        return anomalyDetected;
    }
    
    public void setAnomalyDetected(boolean anomalyDetected) {
        this.anomalyDetected = anomalyDetected;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReceivedData that = (ReceivedData) o;
        return Objects.equals(id, that.id);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
    
    @Override
    public String toString() {
        return String.format("ReceivedData{id=%d, receivedTime=%s, sensorType=%s, value=%s, topic='%s'}", 
                id, receivedTime, getSensorType(), getValue(), mqttTopic);
    }
}
