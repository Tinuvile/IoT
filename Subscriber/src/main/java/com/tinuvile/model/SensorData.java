package com.tinuvile.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 传感器数据模型
 * 
 * @author tinuvile
 */
public class SensorData {
    
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    @JsonProperty("sensor_type")
    private SensorType sensorType;
    
    @JsonProperty("value")
    private Double value;
    
    @JsonProperty("unit")
    private String unit;
    
    @JsonProperty("node_id")
    private Integer nodeId;
    
    @JsonProperty("location")
    private String location;
    
    public SensorData() {}
    
    public SensorData(LocalDateTime timestamp, SensorType sensorType, Double value, String unit) {
        this.timestamp = timestamp;
        this.sensorType = sensorType;
        this.value = value;
        this.unit = unit;
        this.nodeId = 1; // 默认节点ID
        this.location = getDefaultLocation(sensorType);
    }
    
    private String getDefaultLocation(SensorType sensorType) {
        switch (sensorType) {
            case TEMPERATURE:
            case HUMIDITY:
                return "实验室A区";
            case PRESSURE:
                return "楼顶气象站";
            default:
                return "未知位置";
        }
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public SensorType getSensorType() {
        return sensorType;
    }
    
    public void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
    }
    
    public Double getValue() {
        return value;
    }
    
    public void setValue(Double value) {
        this.value = value;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public Integer getNodeId() {
        return nodeId;
    }
    
    public void setNodeId(Integer nodeId) {
        this.nodeId = nodeId;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SensorData that = (SensorData) o;
        return Objects.equals(timestamp, that.timestamp) &&
                sensorType == that.sensorType &&
                Objects.equals(value, that.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(timestamp, sensorType, value);
    }
    
    @Override
    public String toString() {
        return String.format("SensorData{timestamp=%s, type=%s, value=%s %s, location='%s'}", 
                timestamp, sensorType, value, unit, location);
    }
}