package com.tinuvile.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 数据分析结果模型
 * 
 * @author tinuvile
 */
public class AnalysisResult {
    
    @JsonProperty("sensor_type")
    private SensorType sensorType;
    
    @JsonProperty("analysis_time")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime analysisTime;
    
    @JsonProperty("sample_count")
    private int sampleCount;
    
    @JsonProperty("min_value")
    private Double minValue;
    
    @JsonProperty("max_value")
    private Double maxValue;
    
    @JsonProperty("avg_value")
    private Double avgValue;
    
    @JsonProperty("median_value")
    private Double medianValue;
    
    @JsonProperty("standard_deviation")
    private Double standardDeviation;
    
    @JsonProperty("variance")
    private Double variance;
    
    @JsonProperty("trend_direction")
    private String trendDirection; // "UP", "DOWN", "STABLE"
    
    @JsonProperty("trend_strength")
    private Double trendStrength; // 0-1之间，表示趋势强度
    
    @JsonProperty("anomaly_count")
    private int anomalyCount;
    
    @JsonProperty("data_quality_score")
    private Double dataQualityScore;
    
    @JsonProperty("time_range")
    private Map<String, LocalDateTime> timeRange; // start, end
    
    @JsonProperty("prediction")
    private Double prediction; // 简单预测值
    
    @JsonProperty("confidence_level")
    private Double confidenceLevel; // 预测置信度
    
    public AnalysisResult() {
        this.analysisTime = LocalDateTime.now();
    }
    
    public AnalysisResult(SensorType sensorType) {
        this();
        this.sensorType = sensorType;
    }
    
    // Getters and Setters
    public SensorType getSensorType() {
        return sensorType;
    }
    
    public void setSensorType(SensorType sensorType) {
        this.sensorType = sensorType;
    }
    
    public LocalDateTime getAnalysisTime() {
        return analysisTime;
    }
    
    public void setAnalysisTime(LocalDateTime analysisTime) {
        this.analysisTime = analysisTime;
    }
    
    public int getSampleCount() {
        return sampleCount;
    }
    
    public void setSampleCount(int sampleCount) {
        this.sampleCount = sampleCount;
    }
    
    public Double getMinValue() {
        return minValue;
    }
    
    public void setMinValue(Double minValue) {
        this.minValue = minValue;
    }
    
    public Double getMaxValue() {
        return maxValue;
    }
    
    public void setMaxValue(Double maxValue) {
        this.maxValue = maxValue;
    }
    
    public Double getAvgValue() {
        return avgValue;
    }
    
    public void setAvgValue(Double avgValue) {
        this.avgValue = avgValue;
    }
    
    public Double getMedianValue() {
        return medianValue;
    }
    
    public void setMedianValue(Double medianValue) {
        this.medianValue = medianValue;
    }
    
    public Double getStandardDeviation() {
        return standardDeviation;
    }
    
    public void setStandardDeviation(Double standardDeviation) {
        this.standardDeviation = standardDeviation;
    }
    
    public Double getVariance() {
        return variance;
    }
    
    public void setVariance(Double variance) {
        this.variance = variance;
    }
    
    public String getTrendDirection() {
        return trendDirection;
    }
    
    public void setTrendDirection(String trendDirection) {
        this.trendDirection = trendDirection;
    }
    
    public Double getTrendStrength() {
        return trendStrength;
    }
    
    public void setTrendStrength(Double trendStrength) {
        this.trendStrength = trendStrength;
    }
    
    public int getAnomalyCount() {
        return anomalyCount;
    }
    
    public void setAnomalyCount(int anomalyCount) {
        this.anomalyCount = anomalyCount;
    }
    
    public Double getDataQualityScore() {
        return dataQualityScore;
    }
    
    public void setDataQualityScore(Double dataQualityScore) {
        this.dataQualityScore = dataQualityScore;
    }
    
    public Map<String, LocalDateTime> getTimeRange() {
        return timeRange;
    }
    
    public void setTimeRange(Map<String, LocalDateTime> timeRange) {
        this.timeRange = timeRange;
    }
    
    public Double getPrediction() {
        return prediction;
    }
    
    public void setPrediction(Double prediction) {
        this.prediction = prediction;
    }
    
    public Double getConfidenceLevel() {
        return confidenceLevel;
    }
    
    public void setConfidenceLevel(Double confidenceLevel) {
        this.confidenceLevel = confidenceLevel;
    }
    
    @Override
    public String toString() {
        return String.format("AnalysisResult{sensorType=%s, samples=%d, avg=%.2f, min=%.2f, max=%.2f, trend=%s}", 
                sensorType, sampleCount, avgValue, minValue, maxValue, trendDirection);
    }
}
