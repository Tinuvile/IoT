package com.tinuvile.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 发布状态模型
 * 
 * @author tinuvile
 */
public class PublishStatus {
    
    @JsonProperty("is_running")
    private boolean running;
    
    @JsonProperty("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonProperty("current_sensor_type")
    private SensorType currentSensorType;
    
    @JsonProperty("total_published")
    private long totalPublished;
    
    @JsonProperty("temperature_count")
    private long temperatureCount;
    
    @JsonProperty("humidity_count")
    private long humidityCount;
    
    @JsonProperty("pressure_count")
    private long pressureCount;
    
    @JsonProperty("last_publish_time")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastPublishTime;
    
    @JsonProperty("error_count")
    private long errorCount;
    
    @JsonProperty("publish_rate")
    private double publishRate; // 每秒发布数量
    
    public PublishStatus() {
        this.running = false;
        this.totalPublished = 0;
        this.temperatureCount = 0;
        this.humidityCount = 0;
        this.pressureCount = 0;
        this.errorCount = 0;
        this.publishRate = 0.0;
    }
    
    public void incrementCounter(SensorType sensorType) {
        this.totalPublished++;
        switch (sensorType) {
            case TEMPERATURE:
                this.temperatureCount++;
                break;
            case HUMIDITY:
                this.humidityCount++;
                break;
            case PRESSURE:
                this.pressureCount++;
                break;
        }
        this.lastPublishTime = LocalDateTime.now();
    }
    
    public void incrementErrorCount() {
        this.errorCount++;
    }
    
    // Getters and Setters
    public boolean isRunning() {
        return running;
    }
    
    public void setRunning(boolean running) {
        this.running = running;
        if (running && startTime == null) {
            this.startTime = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public SensorType getCurrentSensorType() {
        return currentSensorType;
    }
    
    public void setCurrentSensorType(SensorType currentSensorType) {
        this.currentSensorType = currentSensorType;
    }
    
    public long getTotalPublished() {
        return totalPublished;
    }
    
    public void setTotalPublished(long totalPublished) {
        this.totalPublished = totalPublished;
    }
    
    public long getTemperatureCount() {
        return temperatureCount;
    }
    
    public void setTemperatureCount(long temperatureCount) {
        this.temperatureCount = temperatureCount;
    }
    
    public long getHumidityCount() {
        return humidityCount;
    }
    
    public void setHumidityCount(long humidityCount) {
        this.humidityCount = humidityCount;
    }
    
    public long getPressureCount() {
        return pressureCount;
    }
    
    public void setPressureCount(long pressureCount) {
        this.pressureCount = pressureCount;
    }
    
    public LocalDateTime getLastPublishTime() {
        return lastPublishTime;
    }
    
    public void setLastPublishTime(LocalDateTime lastPublishTime) {
        this.lastPublishTime = lastPublishTime;
    }
    
    public long getErrorCount() {
        return errorCount;
    }
    
    public void setErrorCount(long errorCount) {
        this.errorCount = errorCount;
    }
    
    public double getPublishRate() {
        return publishRate;
    }
    
    public void setPublishRate(double publishRate) {
        this.publishRate = publishRate;
    }
}