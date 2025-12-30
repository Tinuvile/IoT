package com.tinuvile.service;

import com.tinuvile.model.AnalysisData;
import com.tinuvile.model.SensorDataEntity;
import com.tinuvile.model.SensorType;
import com.tinuvile.model.StatisticsData;
import com.tinuvile.repository.SensorDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据分析服务
 * 提供各种数据分析功能
 * 
 * @author tinuvile
 */
@Service
public class DataAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataAnalysisService.class);
    
    @Autowired
    private SensorDataRepository sensorDataRepository;
    
    /**
     * 获取指定传感器类型的历史数据用于图表展示
     */
    public List<AnalysisData> getHistoryData(SensorType sensorType, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<SensorDataEntity> entities = sensorDataRepository
                    .findBySensorTypeAndTimestampBetween(sensorType, startTime, endTime);
            
            return entities.stream()
                    .map(this::convertToAnalysisData)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("获取历史数据失败: sensorType={}, startTime={}, endTime={}, error={}", 
                    sensorType, startTime, endTime, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取指定传感器类型的最新数据
     */
    public List<AnalysisData> getLatestData(SensorType sensorType, int limit) {
        try {
            List<SensorDataEntity> entities = sensorDataRepository
                    .findBySensorTypeOrderByTimestampDesc(sensorType, PageRequest.of(0, limit));
            
            return entities.stream()
                    .map(this::convertToAnalysisData)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("获取最新数据失败: sensorType={}, limit={}, error={}", 
                    sensorType, limit, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取所有传感器类型的最新数据
     */
    public Map<SensorType, AnalysisData> getAllLatestData() {
        try {
            List<SensorDataEntity> entities = sensorDataRepository.findLatestDataForAllSensorTypes();
            
            return entities.stream()
                    .collect(Collectors.toMap(
                            SensorDataEntity::getSensorType,
                            this::convertToAnalysisData
                    ));
                    
        } catch (Exception e) {
            logger.error("获取所有最新数据失败: error={}", e.getMessage(), e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * 获取统计数据
     */
    public StatisticsData getStatistics(SensorType sensorType, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            logger.info("执行统计查询: sensorType={}, startTime={}, endTime={}", 
                    sensorType, startTime, endTime);
            
            // 先进行计数验证
            Long count = sensorDataRepository.countBySensorTypeAndTimestampBetweenCustom(sensorType, startTime, endTime);
            logger.info("数据计数验证: count={}", count);
            
            // 执行统计查询
            Object[] result = sensorDataRepository
                    .getStatisticsBySensorTypeAndTimestampBetween(sensorType, startTime, endTime);
            
            logger.info("统计查询结果: result={}, length={}", 
                    result, result != null ? result.length : "null");
            
            if (result != null) {
                for (int i = 0; i < result.length; i++) {
                    logger.info("result[{}] = {} (type: {})", i, result[i], 
                            result[i] != null ? result[i].getClass().getSimpleName() : "null");
                }
            }
            
            StatisticsData statistics = new StatisticsData(sensorType, startTime, endTime);
            
            // JPA查询返回Object[]，需要处理result[0]
            if (result != null && result.length > 0 && result[0] != null) {
                Object[] statValues = (Object[]) result[0];
                if (statValues.length >= 5) {
                    statistics.setAvgValue(statValues[0] != null ? new BigDecimal(statValues[0].toString()) : BigDecimal.ZERO);
                    statistics.setMinValue(statValues[1] != null ? new BigDecimal(statValues[1].toString()) : BigDecimal.ZERO);
                    statistics.setMaxValue(statValues[2] != null ? new BigDecimal(statValues[2].toString()) : BigDecimal.ZERO);
                    statistics.setTotalCount(statValues[3] != null ? ((Number) statValues[3]).longValue() : 0L);
                    statistics.setValidCount(statValues[4] != null ? ((Number) statValues[4]).longValue() : 0L);
                    
                    logger.info("成功设置统计数据: avg={}, min={}, max={}, total={}, valid={}", 
                            statistics.getAvgValue(), statistics.getMinValue(), statistics.getMaxValue(),
                            statistics.getTotalCount(), statistics.getValidCount());
                } else {
                    logger.warn("统计值数组长度不足: statValues.length={}", statValues.length);
                }
            } else {
                logger.warn("统计查询结果为空: result={}", result);
            }
            
            return statistics;
            
        } catch (Exception e) {
            logger.error("获取统计数据失败: sensorType={}, startTime={}, endTime={}, error={}", 
                    sensorType, startTime, endTime, e.getMessage(), e);
            return new StatisticsData(sensorType, startTime, endTime);
        }
    }
    
    /**
     * 获取按小时分组的统计数据
     */
    public List<StatisticsData> getHourlyStatistics(SensorType sensorType, LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<Object[]> results = sensorDataRepository
                    .getHourlyStatistics(sensorType, startTime, endTime);
            
            return results.stream()
                    .map(result -> {
                        StatisticsData statistics = new StatisticsData();
                        statistics.setSensorType(sensorType);
                        statistics.setUnit(sensorType.getUnit());
                        
                        if (result.length >= 5) {
                            // 解析小时组时间
                            String hourGroupStr = (String) result[0];
                            statistics.setStartTime(LocalDateTime.parse(hourGroupStr.replace(" ", "T")));
                            statistics.setEndTime(statistics.getStartTime().plusHours(1));
                            
                            statistics.setAvgValue(result[1] != null ? (BigDecimal) result[1] : BigDecimal.ZERO);
                            statistics.setMinValue(result[2] != null ? (BigDecimal) result[2] : BigDecimal.ZERO);
                            statistics.setMaxValue(result[3] != null ? (BigDecimal) result[3] : BigDecimal.ZERO);
                            statistics.setTotalCount(result[4] != null ? ((Number) result[4]).longValue() : 0L);
                            statistics.setValidCount(statistics.getTotalCount()); // 查询已过滤无效数据
                        }
                        
                        return statistics;
                    })
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            logger.error("获取小时统计数据失败: sensorType={}, startTime={}, endTime={}, error={}", 
                    sensorType, startTime, endTime, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取数据总量统计
     */
    public Map<SensorType, Long> getDataCounts(LocalDateTime since) {
        try {
            Map<SensorType, Long> counts = new HashMap<>();
            
            for (SensorType sensorType : SensorType.values()) {
                Long count = sensorDataRepository.countBySensorTypeAndTimestampAfter(sensorType, since);
                counts.put(sensorType, count != null ? count : 0L);
            }
            
            return counts;
            
        } catch (Exception e) {
            logger.error("获取数据总量统计失败: since={}, error={}", since, e.getMessage(), e);
            return new HashMap<>();
        }
    }
    
    /**
     * 转换实体为分析数据
     */
    private AnalysisData convertToAnalysisData(SensorDataEntity entity) {
        AnalysisData analysisData = new AnalysisData();
        analysisData.setTimestamp(entity.getTimestamp());
        analysisData.setValue(entity.getValue());
        analysisData.setSensorType(entity.getSensorType());
        analysisData.setUnit(entity.getUnit());
        return analysisData;
    }
}
