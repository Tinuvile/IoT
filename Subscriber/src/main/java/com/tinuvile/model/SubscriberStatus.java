package com.tinuvile.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订阅者状态模型
 * 
 * @author tinuvile
 */
public class SubscriberStatus {
    
    @JsonProperty("running")
    private boolean running = false;
    
    @JsonProperty("start_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;
    
    @JsonProperty("total_received")
    private final AtomicLong totalReceived = new AtomicLong(0);
    
    @JsonProperty("temperature_count")
    private final AtomicLong temperatureCount = new AtomicLong(0);
    
    @JsonProperty("humidity_count")
    private final AtomicLong humidityCount = new AtomicLong(0);
    
    @JsonProperty("pressure_count")
    private final AtomicLong pressureCount = new AtomicLong(0);
    
    @JsonProperty("error_count")
    private final AtomicLong errorCount = new AtomicLong(0);
    
    @JsonProperty("receive_rate")
    private double receiveRate = 0.0;
    
    @JsonProperty("last_received_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastReceivedTime;
    
    @JsonProperty("current_sensor_type")
    private SensorType currentSensorType;
    
    public SubscriberStatus() {}
    
    // 增加计数器
    public void incrementCounter(SensorType sensorType) {
        totalReceived.incrementAndGet();
        switch (sensorType) {
            case TEMPERATURE:
                temperatureCount.incrementAndGet();
                break;
            case HUMIDITY:
                humidityCount.incrementAndGet();
                break;
            case PRESSURE:
                pressureCount.incrementAndGet();
                break;
        }
        lastReceivedTime = LocalDateTime.now();
        currentSensorType = sensorType;
    }
    
    public void incrementErrorCount() {
        errorCount.incrementAndGet();
    }
    
    // Getters and Setters
    public boolean isRunning() {
        return running;
    }
    
    public void setRunning(boolean running) {
        this.running = running;
        if (running && startTime == null) {
            startTime = LocalDateTime.now();
        }
    }
    
    public LocalDateTime getStartTime() {
        return startTime;
    }
    
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    
    public long getTotalReceived() {
        return totalReceived.get();
    }
    
    public void setTotalReceived(long totalReceived) {
        this.totalReceived.set(totalReceived);
    }
    
    public long getTemperatureCount() {
        return temperatureCount.get();
    }
    
    public void setTemperatureCount(long temperatureCount) {
        this.temperatureCount.set(temperatureCount);
    }
    
    public long getHumidityCount() {
        return humidityCount.get();
    }
    
    public void setHumidityCount(long humidityCount) {
        this.humidityCount.set(humidityCount);
    }
    
    public long getPressureCount() {
        return pressureCount.get();
    }
    
    public void setPressureCount(long pressureCount) {
        this.pressureCount.set(pressureCount);
    }
    
    public long getErrorCount() {
        return errorCount.get();
    }
    
    public void setErrorCount(long errorCount) {
        this.errorCount.set(errorCount);
    }
    
    public double getReceiveRate() {
        return receiveRate;
    }
    
    public void setReceiveRate(double receiveRate) {
        this.receiveRate = receiveRate;
    }
    
    public LocalDateTime getLastReceivedTime() {
        return lastReceivedTime;
    }
    
    public void setLastReceivedTime(LocalDateTime lastReceivedTime) {
        this.lastReceivedTime = lastReceivedTime;
    }
    
    public SensorType getCurrentSensorType() {
        return currentSensorType;
    }
    
    public void setCurrentSensorType(SensorType currentSensorType) {
        this.currentSensorType = currentSensorType;
    }
}
