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
 * 传感器数据仓储接口
 * 
 * @author tinuvile
 */
@Repository
public interface SensorDataRepository extends JpaRepository<SensorDataEntity, Long> {
    
    /**
     * 根据传感器类型查询最新数据
     */
    List<SensorDataEntity> findBySensorTypeOrderByReceivedTimeDesc(SensorType sensorType, Pageable pageable);
    
    /**
     * 根据时间范围和传感器类型查询数据
     */
    List<SensorDataEntity> findBySensorTypeAndReceivedTimeBetweenOrderByReceivedTimeAsc(
            SensorType sensorType, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 统计各类型传感器数据数量
     */
    @Query("SELECT s.sensorType, COUNT(s) FROM SensorDataEntity s WHERE s.isValid = true GROUP BY s.sensorType")
    List<Object[]> countBySensorType();
    
    /**
     * 查询指定传感器类型的统计数据
     */
    @Query("SELECT " +
           "COUNT(s) as count, " +
           "MIN(s.value) as minValue, " +
           "MAX(s.value) as maxValue, " +
           "AVG(s.value) as avgValue " +
           "FROM SensorDataEntity s " +
           "WHERE s.sensorType = :sensorType AND s.isValid = true")
    Object[] getStatisticsBySensorType(@Param("sensorType") SensorType sensorType);
    
    /**
     * 查询指定传感器类型在时间范围内的统计数据
     */
    @Query("SELECT " +
           "COUNT(s) as count, " +
           "MIN(s.value) as minValue, " +
           "MAX(s.value) as maxValue, " +
           "AVG(s.value) as avgValue, " +
           "MIN(s.receivedTime) as startTime, " +
           "MAX(s.receivedTime) as endTime " +
           "FROM SensorDataEntity s " +
           "WHERE s.sensorType = :sensorType " +
           "AND s.receivedTime BETWEEN :startTime AND :endTime " +
           "AND s.isValid = true")
    Object[] getStatisticsByTypeAndTimeRange(
            @Param("sensorType") SensorType sensorType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    /**
     * 查询指定传感器类型的最新N条数据用于趋势分析
     */
    @Query("SELECT s FROM SensorDataEntity s " +
           "WHERE s.sensorType = :sensorType AND s.isValid = true " +
           "ORDER BY s.receivedTime DESC")
    List<SensorDataEntity> findLatestBySensorType(@Param("sensorType") SensorType sensorType, Pageable pageable);
    
    /**
     * 查询异常数据
     */
    List<SensorDataEntity> findByAnomalyDetectedTrueOrderByReceivedTimeDesc(Pageable pageable);
    
    /**
     * 统计总数据量
     */
    @Query("SELECT COUNT(s) FROM SensorDataEntity s WHERE s.isValid = true")
    long countValidData();
    
    /**
     * 统计异常数据量
     */
    @Query("SELECT COUNT(s) FROM SensorDataEntity s WHERE s.anomalyDetected = true")
    long countAnomalyData();
    
    /**
     * 删除过期数据
     */
    void deleteByReceivedTimeBefore(LocalDateTime cutoffTime);
}
