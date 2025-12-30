package com.tinuvile.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 统计数据模型
 * 包含各种统计指标
 * 
 * @author tinuvile
 */
public class StatisticsData {
    
    private SensorType sensorType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal avgValue;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private Long totalCount;
    private Long validCount;
    private String unit;
    
    public StatisticsData() {}
    
    public StatisticsData(SensorType sensorType, LocalDateTime startTime, LocalDateTime endTime) {
        this.sensorType = sensorType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.unit = sensorType.getUnit();
    }
    
    // Getters and Setters
    public SensorType getSensorType() { return sensorType; }
    public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }
    
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    
    @JsonProperty("avgValue")
    public BigDecimal getAvgValue() { return avgValue; }
    public void setAvgValue(BigDecimal avgValue) { this.avgValue = avgValue; }
    
    @JsonProperty("minValue")
    public BigDecimal getMinValue() { return minValue; }
    public void setMinValue(BigDecimal minValue) { this.minValue = minValue; }
    
    @JsonProperty("maxValue")
    public BigDecimal getMaxValue() { return maxValue; }
    public void setMaxValue(BigDecimal maxValue) { this.maxValue = maxValue; }
    
    @JsonProperty("totalCount")
    public Long getTotalCount() { return totalCount; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    
    @JsonProperty("validCount")
    public Long getValidCount() { return validCount; }
    public void setValidCount(Long validCount) { this.validCount = validCount; }
    
    @JsonProperty("unit")
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    
    /**
     * 计算数据质量百分比
     */
    public Double getDataQualityPercent() {
        if (totalCount == null || totalCount == 0) {
            return 0.0;
        }
        return (validCount != null ? validCount : 0) * 100.0 / totalCount;
    }
    
    @Override
    public String toString() {
        return "StatisticsData{" +
                "sensorType=" + sensorType +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", avgValue=" + avgValue +
                ", minValue=" + minValue +
                ", maxValue=" + maxValue +
                ", totalCount=" + totalCount +
                ", validCount=" + validCount +
                ", unit='" + unit + '\'' +
                '}';
    }
}
