package com.tinuvile.model;

import com.tinuvile.converter.SensorTypeConverter;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 传感器数据实体类
 * 
 * @author tinuvile
 */
@Entity
@Table(name = "sensor_data", indexes = {
    @Index(name = "idx_sensor_type", columnList = "sensor_type"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_received_time", columnList = "received_time"),
    @Index(name = "idx_node_sensor", columnList = "node_id,sensor_type"),
    @Index(name = "idx_timestamp_type", columnList = "timestamp,sensor_type")
})
public class SensorDataEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "node_id", nullable = false)
    private Integer nodeId;
    
    @Convert(converter = SensorTypeConverter.class)
    @Column(name = "sensor_type", nullable = false)
    private SensorType sensorType;
    
    @Column(name = "sensor_location", length = 100)
    private String sensorLocation;
    
    @Column(name = "value", nullable = false, precision = 10, scale = 3)
    private BigDecimal value;
    
    @Column(name = "unit", length = 10, nullable = false)
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
    private Integer qualityScore = 100;
    
    @Column(name = "anomaly_detected")
    private Boolean anomalyDetected = false;
    
    @Column(name = "is_valid")
    private Boolean isValid = true;
    
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (receivedTime == null) {
            receivedTime = LocalDateTime.now();
        }
    }
    
    // 从ReceivedData转换的便利构造函数
    public static SensorDataEntity fromReceivedData(ReceivedData receivedData) {
        SensorDataEntity entity = new SensorDataEntity();
        entity.setNodeId(1); // 默认节点ID
        entity.setSensorType(receivedData.getSensorType());
        entity.setSensorLocation(receivedData.getLocation());
        entity.setValue(new BigDecimal(String.valueOf(receivedData.getValue())));
        entity.setUnit(receivedData.getUnit());
        entity.setTimestamp(receivedData.getOriginalTimestamp());
        entity.setMqttTopic(receivedData.getMqttTopic());
        entity.setReceivedTime(receivedData.getReceivedTime());
        entity.setQualityScore(receivedData.getQualityScore() != null ? 
                receivedData.getQualityScore().intValue() : 100);
        entity.setAnomalyDetected(receivedData.isAnomalyDetected());
        entity.setIsValid(true);
        
        // 将原始数据转为JSON字符串
        try {
            // 使用ObjectMapper正确序列化JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.findAndRegisterModules(); // 处理日期时间格式
            
            String originalDataJson = mapper.writeValueAsString(receivedData.getOriginalData());
            
            entity.setRawData(String.format(
                "{\"sensor_data\":%s,\"mqtt_topic\":\"%s\",\"received_time\":\"%s\"}", 
                originalDataJson,
                receivedData.getMqttTopic(),
                receivedData.getReceivedTime()
            ));
        } catch (Exception e) {
            // 如果序列化失败，设置最小化的JSON
            entity.setRawData(String.format(
                "{\"sensor_data\":{\"error\":\"serialization_failed\"},\"mqtt_topic\":\"%s\",\"received_time\":\"%s\"}", 
                receivedData.getMqttTopic() != null ? receivedData.getMqttTopic() : "unknown",
                receivedData.getReceivedTime() != null ? receivedData.getReceivedTime().toString() : "unknown"
            ));
        }
        
        return entity;
    }
    
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
    
    @Override
    public String toString() {
        return String.format("SensorDataEntity{id=%d, sensorType=%s, value=%s, timestamp=%s}", 
                id, sensorType, value, timestamp);
    }
}
