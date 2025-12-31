package com.tinuvile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预测数据模型
 * 用于存储和传输预测结果
 * 
 * @author tinuvile
 */
public class PredictionData {
    
    private LocalDateTime timestamp;
    private BigDecimal predictedValue;
    private BigDecimal confidenceLevel;  // 置信度
    private SensorType sensorType;
    private String unit;
    private String predictionMethod;  // 预测方法名称
    
    public PredictionData() {}
    
    public PredictionData(LocalDateTime timestamp, BigDecimal predictedValue, SensorType sensorType, String unit) {
        this.timestamp = timestamp;
        this.predictedValue = predictedValue;
        this.sensorType = sensorType;
        this.unit = unit;
        this.confidenceLevel = new BigDecimal("0.75"); // 默认置信度75%
        this.predictionMethod = "linear_regression"; // 默认预测方法
    }
    
    // Getters and Setters
    @JsonProperty("timestamp")
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    @JsonProperty("predictedValue")
    public BigDecimal getPredictedValue() { return predictedValue; }
    public void setPredictedValue(BigDecimal predictedValue) { this.predictedValue = predictedValue; }
    
    @JsonProperty("confidenceLevel")
    public BigDecimal getConfidenceLevel() { return confidenceLevel; }
    public void setConfidenceLevel(BigDecimal confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    
    @JsonProperty("sensorType")
    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }
    
    @JsonProperty("unit")
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    @JsonProperty("predictionMethod")
    public String getPredictionMethod() { return predictionMethod; }
    public void setPredictionMethod(String predictionMethod) { this.predictionMethod = predictionMethod; }
    
    @Override
    public String toString() {
        return "PredictionData{" +
                "timestamp=" + timestamp +
                ", predictedValue=" + predictedValue +
                ", confidenceLevel=" + confidenceLevel +
                ", sensorType=" + sensorType +
                ", unit='" + unit + '\'' +
                ", predictionMethod='" + predictionMethod + '\'' +
                '}';
    }
}
