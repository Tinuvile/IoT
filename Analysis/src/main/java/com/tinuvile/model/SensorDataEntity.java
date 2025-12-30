package com.tinuvile.model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 传感器数据实体类
 * 对应数据库中的sensor_data表
 * 
 * @author tinuvile
 */
@Entity
@Table(name = "sensor_data")
public class SensorDataEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "node_id", nullable = false)
    private Integer nodeId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sensor_type", nullable = false)
    private SensorType sensorType;
    
    @Column(name = "sensor_location")
    private String sensorLocation;
    
    @Column(name = "value", nullable = false, precision = 10, scale = 3)
    private BigDecimal value;
    
    @Column(name = "unit", nullable = false, length = 10)
    private String unit;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "mqtt_topic")
    private String mqttTopic;
    
    @Column(name = "received_time")
    private LocalDateTime receivedTime;
    
    @Column(name = "raw_data", columnDefinition = "JSON")
    private String rawData;
    
    @Column(name = "quality_score")
    private Integer qualityScore;
    
    @Column(name = "anomaly_detected")
    private Boolean anomalyDetected;
    
    @Column(name = "is_valid")
    private Boolean isValid;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    // 默认构造函数
    public SensorDataEntity() {}
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Integer getNodeId() { return nodeId; }
    public void setNodeId(Integer nodeId) { this.nodeId = nodeId; }
    
    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }
    
    public String getSensorLocation() { return sensorLocation; }
    public void setSensorLocation(String sensorLocation) { this.sensorLocation = sensorLocation; }
    
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getMqttTopic() { return mqttTopic; }
    public void setMqttTopic(String mqttTopic) { this.mqttTopic = mqttTopic; }
    
    public LocalDateTime getReceivedTime() { return receivedTime; }
    public void setReceivedTime(LocalDateTime receivedTime) { this.receivedTime = receivedTime; }
    
    public String getRawData() { return rawData; }
    public void setRawData(String rawData) { this.rawData = rawData; }
    
    public Integer getQualityScore() { return qualityScore; }
    public void setQualityScore(Integer qualityScore) { this.qualityScore = qualityScore; }
    
    public Boolean getAnomalyDetected() { return anomalyDetected; }
    public void setAnomalyDetected(Boolean anomalyDetected) { this.anomalyDetected = anomalyDetected; }
    
    public Boolean getIsValid() { return isValid; }
    public void setIsValid(Boolean isValid) { this.isValid = isValid; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (receivedTime == null) {
            receivedTime = LocalDateTime.now();
        }
    }
}
