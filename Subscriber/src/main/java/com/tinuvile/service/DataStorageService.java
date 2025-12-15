package com.tinuvile.service;

import com.tinuvile.model.ReceivedData;
import com.tinuvile.model.SensorDataEntity;
import com.tinuvile.model.SensorType;
import com.tinuvile.repository.SensorDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MySQL数据存储服务
 * 负责将接收到的传感器数据存储到MySQL数据库
 * 
 * @author tinuvile
 */
@Service
public class DataStorageService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataStorageService.class);
    
    @Autowired
    private SensorDataRepository sensorDataRepository;
    
    @Value("${subscriber.storage.max-records:10000}")
    private int maxRecords;
    
    @Value("${subscriber.storage.retention-days:30}")
    private int retentionDays;
    
    /**
     * 存储接收到的数据
     */
    @Async
    @Transactional
    public void storeData(ReceivedData receivedData) {
        try {
            // 转换为数据库实体
            SensorDataEntity entity = SensorDataEntity.fromReceivedData(receivedData);
            
            // 保存到数据库
            sensorDataRepository.save(entity);
            
            // 标记为已处理
            receivedData.setProcessed(true);
            
            logger.debug("数据存储成功: {}", entity);
            
        } catch (Exception e) {
            logger.error("存储数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取指定类型的最新数据
     */
    public List<ReceivedData> getLatestData(SensorType sensorType, int limit) {
        try {
            List<SensorDataEntity> entities = sensorDataRepository
                    .findBySensorTypeOrderByReceivedTimeDesc(sensorType, PageRequest.of(0, limit));
            
            return entities.stream()
                    .map(this::convertToReceivedData)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("查询最新数据失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取所有类型的最新数据
     */
    public Map<SensorType, List<ReceivedData>> getAllLatestData(int limitPerType) {
        Map<SensorType, List<ReceivedData>> result = new HashMap<>();
        
        for (SensorType type : SensorType.values()) {
            result.put(type, getLatestData(type, limitPerType));
        }
        
        return result;
    }
    
    /**
     * 获取指定时间范围内的数据
     */
    public List<ReceivedData> getDataInTimeRange(SensorType sensorType, 
                                                LocalDateTime startTime, 
                                                LocalDateTime endTime) {
        try {
            List<SensorDataEntity> entities = sensorDataRepository
                    .findBySensorTypeAndReceivedTimeBetweenOrderByReceivedTimeAsc(
                            sensorType, startTime, endTime);
            
            return entities.stream()
                    .map(this::convertToReceivedData)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("查询时间范围数据失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取统计信息
     */
    public StorageStatistics getStatistics() {
        StorageStatistics stats = new StorageStatistics();
        
        try {
            // 总数据量
            long totalCount = sensorDataRepository.countValidData();
            stats.setTotalStoredCount((int) totalCount);
            
            // 各类型数据量统计
            List<Object[]> typeCounts = sensorDataRepository.countBySensorType();
            for (Object[] row : typeCounts) {
                SensorType type = (SensorType) row[0];
                Long count = (Long) row[1];
                
                switch (type) {
                    case TEMPERATURE:
                        stats.setTemperatureCount(count.intValue());
                        break;
                    case HUMIDITY:
                        stats.setHumidityCount(count.intValue());
                        break;
                    case PRESSURE:
                        stats.setPressureCount(count.intValue());
                        break;
                }
            }
            
            stats.setLastUpdateTime(LocalDateTime.now());
            
        } catch (Exception e) {
            logger.error("获取存储统计失败: {}", e.getMessage(), e);
        }
        
        return stats;
    }
    
    /**
     * 获取传感器类型的统计数据
     */
    public SensorStatistics getSensorStatistics(SensorType sensorType) {
        try {
            Object[] result = sensorDataRepository.getStatisticsBySensorType(sensorType);
            
            if (result != null && result.length >= 4) {
                SensorStatistics stats = new SensorStatistics();
                stats.setSensorType(sensorType);
                stats.setCount(((Number) result[0]).longValue());
                stats.setMinValue(((Number) result[1]).doubleValue());
                stats.setMaxValue(((Number) result[2]).doubleValue());
                stats.setAvgValue(((Number) result[3]).doubleValue());
                return stats;
            }
            
        } catch (Exception e) {
            logger.error("获取传感器统计失败: {}", e.getMessage(), e);
        }
        
        return new SensorStatistics(sensorType);
    }
    
    /**
     * 获取时间范围内的传感器统计数据
     */
    public SensorStatistics getSensorStatistics(SensorType sensorType, 
                                               LocalDateTime startTime, 
                                               LocalDateTime endTime) {
        try {
            Object[] result = sensorDataRepository.getStatisticsByTypeAndTimeRange(
                    sensorType, startTime, endTime);
            
            if (result != null && result.length >= 6) {
                SensorStatistics stats = new SensorStatistics();
                stats.setSensorType(sensorType);
                stats.setCount(((Number) result[0]).longValue());
                stats.setMinValue(((Number) result[1]).doubleValue());
                stats.setMaxValue(((Number) result[2]).doubleValue());
                stats.setAvgValue(((Number) result[3]).doubleValue());
                stats.setStartTime((LocalDateTime) result[4]);
                stats.setEndTime((LocalDateTime) result[5]);
                return stats;
            }
            
        } catch (Exception e) {
            logger.error("获取时间范围传感器统计失败: {}", e.getMessage(), e);
        }
        
        return new SensorStatistics(sensorType);
    }
    
    /**
     * 获取用于趋势分析的数据
     */
    public List<SensorDataEntity> getDataForTrendAnalysis(SensorType sensorType, int count) {
        try {
            return sensorDataRepository.findLatestBySensorType(
                    sensorType, PageRequest.of(0, count));
        } catch (Exception e) {
            logger.error("获取趋势分析数据失败: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 定期清理过期数据
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    @Transactional
    public void scheduledCleanup() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
            
            long deletedCount = sensorDataRepository.count();
            sensorDataRepository.deleteByReceivedTimeBefore(cutoffTime);
            deletedCount -= sensorDataRepository.count();
            
            if (deletedCount > 0) {
                logger.info("定期清理过期数据: {} 条", deletedCount);
            }
            
        } catch (Exception e) {
            logger.error("定期清理数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 清空所有数据
     */
    @Transactional
    public void clearAllData() {
        try {
            sensorDataRepository.deleteAll();
            logger.info("已清空所有存储数据");
        } catch (Exception e) {
            logger.error("清空数据失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 将数据库实体转换为ReceivedData
     */
    private ReceivedData convertToReceivedData(SensorDataEntity entity) {
        ReceivedData receivedData = new ReceivedData();
        receivedData.setId(entity.getId());
        receivedData.setReceivedTime(entity.getReceivedTime());
        receivedData.setProcessed(true);
        receivedData.setProcessingTime(entity.getCreatedAt());
        receivedData.setMqttTopic(entity.getMqttTopic());
        receivedData.setQualityScore(entity.getQualityScore().doubleValue());
        receivedData.setAnomalyDetected(entity.getAnomalyDetected());
        
        // 重建SensorData对象
        com.tinuvile.model.SensorData sensorData = new com.tinuvile.model.SensorData();
        sensorData.setSensorType(entity.getSensorType());
        sensorData.setValue(entity.getValue().doubleValue());
        sensorData.setUnit(entity.getUnit());
        sensorData.setTimestamp(entity.getTimestamp());
        sensorData.setNodeId(entity.getNodeId());
        sensorData.setLocation(entity.getSensorLocation());
        
        receivedData.setOriginalData(sensorData);
        
        return receivedData;
    }
    
    /**
     * 存储统计信息模型
     */
    public static class StorageStatistics {
        private int totalStoredCount;
        private int temperatureCount;
        private int humidityCount;
        private int pressureCount;
        private LocalDateTime lastUpdateTime;
        
        // Getters and Setters
        public int getTotalStoredCount() { return totalStoredCount; }
        public void setTotalStoredCount(int totalStoredCount) { this.totalStoredCount = totalStoredCount; }
        
        public int getTemperatureCount() { return temperatureCount; }
        public void setTemperatureCount(int temperatureCount) { this.temperatureCount = temperatureCount; }
        
        public int getHumidityCount() { return humidityCount; }
        public void setHumidityCount(int humidityCount) { this.humidityCount = humidityCount; }
        
        public int getPressureCount() { return pressureCount; }
        public void setPressureCount(int pressureCount) { this.pressureCount = pressureCount; }
        
        public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
    }
    
    /**
     * 传感器统计信息模型
     */
    public static class SensorStatistics {
        private SensorType sensorType;
        private long count;
        private double minValue;
        private double maxValue;
        private double avgValue;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        
        public SensorStatistics() {}
        
        public SensorStatistics(SensorType sensorType) {
            this.sensorType = sensorType;
            this.count = 0;
            this.minValue = 0.0;
            this.maxValue = 0.0;
            this.avgValue = 0.0;
        }
        
        // Getters and Setters
        public SensorType getSensorType() { return sensorType; }
        public void setSensorType(SensorType sensorType) { this.sensorType = sensorType; }
        
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        
        public double getMinValue() { return minValue; }
        public void setMinValue(double minValue) { this.minValue = minValue; }
        
        public double getMaxValue() { return maxValue; }
        public void setMaxValue(double maxValue) { this.maxValue = maxValue; }
        
        public double getAvgValue() { return avgValue; }
        public void setAvgValue(double avgValue) { this.avgValue = avgValue; }
        
        public LocalDateTime getStartTime() { return startTime; }
        public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
        
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    }
}
