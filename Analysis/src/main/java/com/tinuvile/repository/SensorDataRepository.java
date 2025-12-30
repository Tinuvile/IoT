package com.tinuvile.repository;

import com.tinuvile.model.SensorDataEntity;
import com.tinuvile.model.SensorType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 传感器数据仓库接口
 * 
 * @author tinuvile
 */
@Repository
public interface SensorDataRepository extends JpaRepository<SensorDataEntity, Long> {
    
    /**
     * 根据传感器类型查找最新数据
     */
    List<SensorDataEntity> findBySensorTypeOrderByTimestampDesc(SensorType sensorType, Pageable pageable);
    
    /**
     * 根据传感器类型和时间范围查找数据
     */
    @Query("SELECT s FROM SensorDataEntity s WHERE s.sensorType = :sensorType " +
           "AND s.timestamp BETWEEN :startTime AND :endTime " +
           "AND s.isValid = true " +
           "ORDER BY s.timestamp ASC")
    List<SensorDataEntity> findBySensorTypeAndTimestampBetween(
            @Param("sensorType") SensorType sensorType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 根据时间范围查找所有类型的数据
     */
    @Query("SELECT s FROM SensorDataEntity s WHERE s.timestamp BETWEEN :startTime AND :endTime " +
           "AND s.isValid = true " +
           "ORDER BY s.timestamp ASC, s.sensorType ASC")
    List<SensorDataEntity> findByTimestampBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 统计指定类型和时间范围内的数据
     */
    @Query("SELECT " +
           "COALESCE(AVG(s.value), 0) as avgValue, " +
           "COALESCE(MIN(s.value), 0) as minValue, " +
           "COALESCE(MAX(s.value), 0) as maxValue, " +
           "COUNT(s) as totalCount, " +
           "SUM(CASE WHEN s.isValid = true THEN 1 ELSE 0 END) as validCount " +
           "FROM SensorDataEntity s WHERE s.sensorType = :sensorType " +
           "AND s.timestamp BETWEEN :startTime AND :endTime")
    Object[] getStatisticsBySensorTypeAndTimestampBetween(
            @Param("sensorType") SensorType sensorType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 简单计数查询，用于验证数据存在
     */
    @Query("SELECT COUNT(s) FROM SensorDataEntity s WHERE s.sensorType = :sensorType " +
           "AND s.timestamp BETWEEN :startTime AND :endTime")
    Long countBySensorTypeAndTimestampBetweenCustom(
            @Param("sensorType") SensorType sensorType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 获取最近的数据数量
     */
    @Query("SELECT COUNT(s) FROM SensorDataEntity s WHERE s.sensorType = :sensorType " +
           "AND s.timestamp >= :since AND s.isValid = true")
    Long countBySensorTypeAndTimestampAfter(
            @Param("sensorType") SensorType sensorType,
            @Param("since") LocalDateTime since);
    
    /**
     * 获取所有传感器类型的最新数据
     */
    @Query("SELECT s FROM SensorDataEntity s WHERE s.id IN (" +
           "SELECT MAX(s2.id) FROM SensorDataEntity s2 WHERE s2.isValid = true " +
           "GROUP BY s2.sensorType) ORDER BY s.sensorType ASC")
    List<SensorDataEntity> findLatestDataForAllSensorTypes();
    
    /**
     * 按小时分组统计数据
     */
    @Query("SELECT " +
           "FUNCTION('DATE_FORMAT', s.timestamp, '%Y-%m-%d %H:00:00') as hourGroup, " +
           "AVG(s.value) as avgValue, " +
           "MIN(s.value) as minValue, " +
           "MAX(s.value) as maxValue, " +
           "COUNT(s) as totalCount " +
           "FROM SensorDataEntity s WHERE s.sensorType = :sensorType " +
           "AND s.timestamp BETWEEN :startTime AND :endTime " +
           "AND s.isValid = true " +
           "GROUP BY FUNCTION('DATE_FORMAT', s.timestamp, '%Y-%m-%d %H:00:00') " +
           "ORDER BY hourGroup ASC")
    List<Object[]> getHourlyStatistics(
            @Param("sensorType") SensorType sensorType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
