package com.tinuvile.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分析数据模型
 * 用于数据分析和图表展示
 * 
 * @author tinuvile
 */
public class AnalysisData {
    
    private LocalDateTime timestamp;
    private BigDecimal value;
    private SensorType sensorType;
    private String unit;
    
    public AnalysisData() {}
    
    public AnalysisData(LocalDateTime timestamp, BigDecimal value, SensorType sensorType, String unit) {
        this.timestamp = timestamp;
        this.value = value;
        this.sensorType = sensorType;
        this.unit = unit;
    }
    
    // Getters and Setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    
    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    @Override
    public String toString() {
        return "AnalysisData{" +
                "timestamp=" + timestamp +
                ", value=" + value +
                ", sensorType=" + sensorType +
                ", unit='" + unit + '\'' +
                '}';
    }
}
