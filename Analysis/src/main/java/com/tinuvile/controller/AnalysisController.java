package com.tinuvile.controller;

import com.tinuvile.model.AnalysisData;
import com.tinuvile.model.SensorType;
import com.tinuvile.model.StatisticsData;
import com.tinuvile.service.DataAnalysisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据分析控制器
 * 提供数据分析的Web接口
 * 
 * @author tinuvile
 */
@Controller
@RequestMapping("/analysis")
public class AnalysisController {
    
    private static final Logger logger = LoggerFactory.getLogger(AnalysisController.class);
    
    @Autowired
    private DataAnalysisService dataAnalysisService;
    
    /**
     * 分析主页
     */
    @GetMapping({"", "/"})
    public String index(Model model) {
        try {
            // 获取所有传感器类型的最新数据
            Map<SensorType, AnalysisData> latestData = dataAnalysisService.getAllLatestData();
            model.addAttribute("latestData", latestData);
            
            // 获取传感器类型列表
            model.addAttribute("sensorTypes", SensorType.values());
            
            // 设置默认时间范围（最近24小时）
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(24);
            model.addAttribute("defaultStartTime", startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            model.addAttribute("defaultEndTime", endTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return "analysis/index";
            
        } catch (Exception e) {
            logger.error("加载分析主页失败: {}", e.getMessage(), e);
            model.addAttribute("error", "加载数据失败: " + e.getMessage());
            return "analysis/error";
        }
    }
    
    /**
     * 获取历史数据API（用于图表）
     */
    @GetMapping("/api/history")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHistoryData(
            @RequestParam("sensorType") String sensorTypeStr,
            @RequestParam("startTime") String startTimeStr,
            @RequestParam("endTime") String endTimeStr) {
        
        try {
            SensorType sensorType = SensorType.valueOf(sensorTypeStr);
            
            // 解析时间字符串
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
            
            List<AnalysisData> data = dataAnalysisService.getHistoryData(sensorType, startTime, endTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("sensorType", sensorType);
            response.put("startTime", startTime);
            response.put("endTime", endTime);
            response.put("count", data.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取历史数据失败: sensorType={}, startTime={}, endTime={}, error={}", 
                    sensorTypeStr, startTimeStr, endTimeStr, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取最新数据API
     */
    @GetMapping("/api/latest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getLatestData(
            @RequestParam("sensorType") String sensorTypeStr,
            @RequestParam(value = "limit", defaultValue = "100") int limit) {
        
        try {
            SensorType sensorType = SensorType.valueOf(sensorTypeStr);
            List<AnalysisData> data = dataAnalysisService.getLatestData(sensorType, limit);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", data);
            response.put("sensorType", sensorType);
            response.put("count", data.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取最新数据失败: sensorType={}, limit={}, error={}", 
                    sensorTypeStr, limit, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取统计数据API
     */
    @GetMapping("/api/statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getStatistics(
            @RequestParam("sensorType") String sensorTypeStr,
            @RequestParam("startTime") String startTimeStr,
            @RequestParam("endTime") String endTimeStr) {
        
        logger.info("获取统计数据请求: sensorType={}, startTime={}, endTime={}", 
                sensorTypeStr, startTimeStr, endTimeStr);
        
        try {
            SensorType sensorType = SensorType.valueOf(sensorTypeStr);
            
            // 解析时间字符串 (支持 ISO 格式: YYYY-MM-DDTHH:MM:SS)
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
            
            logger.info("解析后的时间: startTime={}, endTime={}", startTime, endTime);
            
            StatisticsData statistics = dataAnalysisService.getStatistics(sensorType, startTime, endTime);
            
            logger.info("统计结果: {}", statistics);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取统计数据失败: sensorType={}, startTime={}, endTime={}, error={}", 
                    sensorTypeStr, startTimeStr, endTimeStr, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取小时统计数据API
     */
    @GetMapping("/api/hourly-statistics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getHourlyStatistics(
            @RequestParam("sensorType") String sensorTypeStr,
            @RequestParam("startTime") String startTimeStr,
            @RequestParam("endTime") String endTimeStr) {
        
        try {
            SensorType sensorType = SensorType.valueOf(sensorTypeStr);
            
            // 解析时间字符串
            LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
            
            List<StatisticsData> statistics = dataAnalysisService.getHourlyStatistics(sensorType, startTime, endTime);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("statistics", statistics);
            response.put("count", statistics.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取小时统计数据失败: sensorType={}, startTime={}, endTime={}, error={}", 
                    sensorTypeStr, startTimeStr, endTimeStr, e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 获取所有传感器类型的最新数据API
     */
    @GetMapping("/api/all-latest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getAllLatestData() {
        try {
            Map<SensorType, AnalysisData> latestData = dataAnalysisService.getAllLatestData();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", latestData);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取所有最新数据失败: error={}", e.getMessage(), e);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            
            return ResponseEntity.badRequest().body(response);
        }
    }
}
