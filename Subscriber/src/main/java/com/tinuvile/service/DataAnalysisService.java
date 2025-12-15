package com.tinuvile.service;

import com.tinuvile.model.AnalysisResult;
import com.tinuvile.model.SensorDataEntity;
import com.tinuvile.model.SensorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据分析服务
 * 负责对传感器数据进行实时统计分析、趋势分析和异常检测
 * 
 * @author tinuvile
 */
@Service
public class DataAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataAnalysisService.class);
    
    @Autowired
    private DataStorageService dataStorageService;
    
    @Value("${subscriber.analysis.window-size:100}")
    private int windowSize;
    
    @Value("${subscriber.analysis.update-interval:10000}")
    private long updateInterval;
    
    // 缓存最新的分析结果
    private final Map<SensorType, AnalysisResult> cachedResults = new ConcurrentHashMap<>();
    
    // 异常检测阈值配置
    private final Map<SensorType, Double[]> anomalyThresholds = new HashMap<>();
    
    public DataAnalysisService() {
        // 初始化异常检测阈值 [minValue, maxValue]
        anomalyThresholds.put(SensorType.TEMPERATURE, new Double[]{-50.0, 100.0});
        anomalyThresholds.put(SensorType.HUMIDITY, new Double[]{0.0, 100.0});
        anomalyThresholds.put(SensorType.PRESSURE, new Double[]{800.0, 1200.0});
    }
    
    /**
     * 获取传感器类型的分析结果
     */
    public AnalysisResult getAnalysisResult(SensorType sensorType) {
        // 先尝试从缓存获取
        AnalysisResult cached = cachedResults.get(sensorType);
        if (cached != null && cached.getAnalysisTime().isAfter(LocalDateTime.now().minusMinutes(5))) {
            return cached;
        }
        
        // 重新计算
        return performAnalysis(sensorType);
    }
    
    /**
     * 获取所有传感器类型的分析结果
     */
    public Map<SensorType, AnalysisResult> getAllAnalysisResults() {
        Map<SensorType, AnalysisResult> results = new HashMap<>();
        
        for (SensorType type : SensorType.values()) {
            results.put(type, getAnalysisResult(type));
        }
        
        return results;
    }
    
    /**
     * 执行指定传感器类型的数据分析
     */
    @Async
    public AnalysisResult performAnalysis(SensorType sensorType) {
        try {
            logger.debug("开始分析 {} 数据", sensorType.getDisplayName());
            
            // 获取最新的分析窗口数据
            List<SensorDataEntity> data = dataStorageService.getDataForTrendAnalysis(sensorType, windowSize);
            
            AnalysisResult result = new AnalysisResult(sensorType);
            
            if (data.isEmpty()) {
                logger.debug("没有可用的 {} 数据进行分析", sensorType.getDisplayName());
                cachedResults.put(sensorType, result);
                return result;
            }
            
            // 基础统计计算
            calculateBasicStatistics(result, data);
            
            // 趋势分析
            calculateTrendAnalysis(result, data);
            
            // 异常检测
            performAnomalyDetection(result, data);
            
            // 数据质量评估
            calculateDataQuality(result, data);
            
            // 简单预测
            performPrediction(result, data);
            
            // 缓存结果
            cachedResults.put(sensorType, result);
            
            logger.debug("完成 {} 数据分析: {}", sensorType.getDisplayName(), result);
            return result;
            
        } catch (Exception e) {
            logger.error("分析 {} 数据失败: {}", sensorType.getDisplayName(), e.getMessage(), e);
            return new AnalysisResult(sensorType);
        }
    }
    
    /**
     * 计算基础统计信息
     */
    private void calculateBasicStatistics(AnalysisResult result, List<SensorDataEntity> data) {
        List<Double> values = data.stream()
                .map(entity -> entity.getValue().doubleValue())
                .collect(Collectors.toList());
        
        if (values.isEmpty()) return;
        
        result.setSampleCount(values.size());
        
        // 最小值、最大值
        result.setMinValue(Collections.min(values));
        result.setMaxValue(Collections.max(values));
        
        // 平均值
        double average = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        result.setAvgValue(Math.round(average * 100.0) / 100.0);
        
        // 中位数
        List<Double> sortedValues = new ArrayList<>(values);
        Collections.sort(sortedValues);
        double median = calculateMedian(sortedValues);
        result.setMedianValue(Math.round(median * 100.0) / 100.0);
        
        // 方差和标准差
        double variance = values.stream()
                .mapToDouble(val -> Math.pow(val - average, 2))
                .average()
                .orElse(0.0);
        result.setVariance(Math.round(variance * 100.0) / 100.0);
        result.setStandardDeviation(Math.round(Math.sqrt(variance) * 100.0) / 100.0);
        
        // 时间范围
        LocalDateTime startTime = data.stream()
                .map(SensorDataEntity::getReceivedTime)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        LocalDateTime endTime = data.stream()
                .map(SensorDataEntity::getReceivedTime)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        
        Map<String, LocalDateTime> timeRange = new HashMap<>();
        timeRange.put("start", startTime);
        timeRange.put("end", endTime);
        result.setTimeRange(timeRange);
    }
    
    /**
     * 计算中位数
     */
    private double calculateMedian(List<Double> sortedValues) {
        int size = sortedValues.size();
        if (size == 0) return 0.0;
        
        if (size % 2 == 0) {
            return (sortedValues.get(size / 2 - 1) + sortedValues.get(size / 2)) / 2.0;
        } else {
            return sortedValues.get(size / 2);
        }
    }
    
    /**
     * 趋势分析
     */
    private void calculateTrendAnalysis(AnalysisResult result, List<SensorDataEntity> data) {
        if (data.size() < 2) {
            result.setTrendDirection("STABLE");
            result.setTrendStrength(0.0);
            return;
        }
        
        // 按时间排序（最新的在前）
        List<SensorDataEntity> sortedData = data.stream()
                .sorted((a, b) -> b.getReceivedTime().compareTo(a.getReceivedTime()))
                .collect(Collectors.toList());
        
        // 使用简单的线性回归计算趋势
        double trendSlope = calculateTrendSlope(sortedData);
        
        // 判断趋势方向
        String trendDirection;
        double trendStrength;
        
        if (Math.abs(trendSlope) < 0.001) {
            trendDirection = "STABLE";
            trendStrength = 0.0;
        } else if (trendSlope > 0) {
            trendDirection = "UP";
            trendStrength = Math.min(Math.abs(trendSlope) * 100, 1.0);
        } else {
            trendDirection = "DOWN";
            trendStrength = Math.min(Math.abs(trendSlope) * 100, 1.0);
        }
        
        result.setTrendDirection(trendDirection);
        result.setTrendStrength(Math.round(trendStrength * 100.0) / 100.0);
    }
    
    /**
     * 计算趋势斜率
     */
    private double calculateTrendSlope(List<SensorDataEntity> sortedData) {
        int n = Math.min(sortedData.size(), 20); // 只使用最新的20个数据点
        
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i; // 时间索引
            double y = sortedData.get(i).getValue().doubleValue();
            
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) return 0.0;
        
        return (n * sumXY - sumX * sumY) / denominator;
    }
    
    /**
     * 异常检测
     */
    private void performAnomalyDetection(AnalysisResult result, List<SensorDataEntity> data) {
        Double[] thresholds = anomalyThresholds.get(result.getSensorType());
        if (thresholds == null) {
            result.setAnomalyCount(0);
            return;
        }
        
        double minThreshold = thresholds[0];
        double maxThreshold = thresholds[1];
        
        long anomalyCount = data.stream()
                .mapToLong(entity -> {
                    double value = entity.getValue().doubleValue();
                    return (value < minThreshold || value > maxThreshold) ? 1 : 0;
                })
                .sum();
        
        result.setAnomalyCount((int) anomalyCount);
    }
    
    /**
     * 数据质量评估
     */
    private void calculateDataQuality(AnalysisResult result, List<SensorDataEntity> data) {
        if (data.isEmpty()) {
            result.setDataQualityScore(0.0);
            return;
        }
        
        // 基于多个因素计算数据质量分数
        double qualityScore = 100.0;
        
        // 异常数据惩罚
        double anomalyRatio = (double) result.getAnomalyCount() / data.size();
        qualityScore -= anomalyRatio * 50; // 异常数据每占1%扣0.5分
        
        // 数据完整性检查（检查空值或无效值）
        long invalidCount = data.stream()
                .mapToLong(entity -> (entity.getValue() == null || entity.getValue().doubleValue() == 0) ? 1 : 0)
                .sum();
        double invalidRatio = (double) invalidCount / data.size();
        qualityScore -= invalidRatio * 30; // 无效数据每占1%扣0.3分
        
        // 时间连续性检查（简单版本，检查数据间隔是否合理）
        if (data.size() > 1) {
            List<SensorDataEntity> sortedData = data.stream()
                    .sorted(Comparator.comparing(SensorDataEntity::getReceivedTime))
                    .collect(Collectors.toList());
            
            long totalMinutes = java.time.Duration.between(
                    sortedData.get(0).getReceivedTime(),
                    sortedData.get(sortedData.size() - 1).getReceivedTime()
            ).toMinutes();
            
            if (totalMinutes > 0) {
                double expectedCount = totalMinutes / 5.0; // 假设每5分钟一个数据点
                double completenessRatio = Math.min(data.size() / expectedCount, 1.0);
                qualityScore = qualityScore * completenessRatio;
            }
        }
        
        result.setDataQualityScore(Math.max(Math.round(qualityScore * 100.0) / 100.0, 0.0));
    }
    
    /**
     * 简单预测
     */
    private void performPrediction(AnalysisResult result, List<SensorDataEntity> data) {
        if (data.size() < 3) {
            result.setPrediction(result.getAvgValue());
            result.setConfidenceLevel(0.0);
            return;
        }
        
        // 使用移动平均进行简单预测
        List<Double> recentValues = data.stream()
                .sorted((a, b) -> b.getReceivedTime().compareTo(a.getReceivedTime()))
                .limit(5) // 最近5个值
                .map(entity -> entity.getValue().doubleValue())
                .collect(Collectors.toList());
        
        double prediction = recentValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        
        // 计算置信度（基于数据稳定性）
        double variance = result.getVariance();
        double confidenceLevel = Math.max(0.0, Math.min(1.0, 1.0 - (variance / 100.0)));
        
        result.setPrediction(Math.round(prediction * 100.0) / 100.0);
        result.setConfidenceLevel(Math.round(confidenceLevel * 100.0) / 100.0);
    }
    
    /**
     * 定期更新分析结果
     */
    @Scheduled(fixedDelayString = "${subscriber.analysis.update-interval}")
    public void scheduledAnalysisUpdate() {
        try {
            logger.debug("开始定期分析更新");
            
            for (SensorType type : SensorType.values()) {
                performAnalysis(type);
            }
            
            logger.debug("定期分析更新完成");
            
        } catch (Exception e) {
            logger.error("定期分析更新失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 清空分析缓存
     */
    public void clearCache() {
        cachedResults.clear();
        logger.info("已清空分析结果缓存");
    }
    
    /**
     * 获取指定时间范围的分析结果
     */
    public AnalysisResult getTimeRangeAnalysis(SensorType sensorType, 
                                              LocalDateTime startTime, 
                                              LocalDateTime endTime) {
        try {
            DataStorageService.SensorStatistics stats = dataStorageService
                    .getSensorStatistics(sensorType, startTime, endTime);
            
            AnalysisResult result = new AnalysisResult(sensorType);
            result.setSampleCount((int) stats.getCount());
            result.setMinValue(stats.getMinValue());
            result.setMaxValue(stats.getMaxValue());
            result.setAvgValue(stats.getAvgValue());
            
            Map<String, LocalDateTime> timeRange = new HashMap<>();
            timeRange.put("start", startTime);
            timeRange.put("end", endTime);
            result.setTimeRange(timeRange);
            
            return result;
            
        } catch (Exception e) {
            logger.error("获取时间范围分析失败: {}", e.getMessage(), e);
            return new AnalysisResult(sensorType);
        }
    }
}
