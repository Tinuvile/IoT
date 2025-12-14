package com.tinuvile.service;

import com.tinuvile.model.PublishStatus;
import com.tinuvile.model.SensorData;
import com.tinuvile.model.SensorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据发布服务
 * 核心业务逻辑，协调数据读取和MQTT发布
 * 
 * @author tinuvile
 */
@Service
public class PublisherService {
    
    private static final Logger logger = LoggerFactory.getLogger(PublisherService.class);
    
    @Autowired
    private DataReaderService dataReaderService;
    
    @Autowired
    private MqttClientService mqttClientService;
    
    @Value("${publisher.publish.interval}")
    private long publishInterval;
    
    @Value("${publisher.publish.batch-size}")
    private int batchSize;
    
    @Value("${publisher.publish.auto-start}")
    private boolean autoStart;
    
    private final PublishStatus publishStatus = new PublishStatus();
    private final AtomicBoolean isPublishing = new AtomicBoolean(false);
    private ScheduledExecutorService scheduledExecutor;
    private CompletableFuture<Void> publishingTask;
    
    // 统计信息
    private final AtomicLong publishCount = new AtomicLong(0);
    private LocalDateTime lastRateCalculation = LocalDateTime.now();
    
    /**
     * 开始发布数据
     */
    @Async
    public CompletableFuture<Void> startPublishing() {
        if (isPublishing.get()) {
            logger.warn("发布服务已在运行中");
            return CompletableFuture.completedFuture(null);
        }
        
        logger.info("开始发布传感器数据...");
        logger.info("发布间隔: {}ms, 批量大小: {}", publishInterval, batchSize);
        
        isPublishing.set(true);
        publishStatus.setRunning(true);
        publishStatus.setStartTime(LocalDateTime.now());
        
        scheduledExecutor = Executors.newScheduledThreadPool(3);
        
        publishingTask = CompletableFuture.runAsync(() -> {
            try {
                // 启动三个类型的数据发布任务
                ScheduledFuture<?> tempTask = scheduledExecutor.scheduleAtFixedRate(
                    () -> publishSensorTypeData(SensorType.TEMPERATURE),
                    0, publishInterval, TimeUnit.MILLISECONDS);
                
                ScheduledFuture<?> humidTask = scheduledExecutor.scheduleAtFixedRate(
                    () -> publishSensorTypeData(SensorType.HUMIDITY),
                    publishInterval / 3, publishInterval, TimeUnit.MILLISECONDS);
                
                ScheduledFuture<?> pressTask = scheduledExecutor.scheduleAtFixedRate(
                    () -> publishSensorTypeData(SensorType.PRESSURE),
                    publishInterval * 2 / 3, publishInterval, TimeUnit.MILLISECONDS);
                
                // 启动统计更新任务
                ScheduledFuture<?> statsTask = scheduledExecutor.scheduleAtFixedRate(
                    this::updateStatistics,
                    1, 1, TimeUnit.SECONDS);
                
                // 等待停止信号
                while (isPublishing.get()) {
                    Thread.sleep(1000);
                }
                
                // 停止所有任务
                tempTask.cancel(false);
                humidTask.cancel(false);
                pressTask.cancel(false);
                statsTask.cancel(false);
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.info("发布任务被中断");
            } catch (Exception e) {
                logger.error("发布任务异常: {}", e.getMessage(), e);
            }
        });
        
        return publishingTask;
    }
    
    /**
     * 停止发布数据
     */
    public void stopPublishing() {
        if (!isPublishing.get()) {
            logger.warn("发布服务未在运行");
            return;
        }
        
        logger.info("停止发布传感器数据...");
        isPublishing.set(false);
        publishStatus.setRunning(false);
        
        if (scheduledExecutor != null) {
            scheduledExecutor.shutdown();
            try {
                if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduledExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduledExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (publishingTask != null) {
            publishingTask.cancel(true);
        }
        
        logger.info("发布服务已停止");
    }
    
    /**
     * 发布指定类型的传感器数据
     */
    private void publishSensorTypeData(SensorType sensorType) {
        try {
            if (!isPublishing.get()) {
                return;
            }
            
            publishStatus.setCurrentSensorType(sensorType);
            
            if (batchSize <= 1) {
                // 单条发布
                SensorData data = dataReaderService.getNextSensorData(sensorType);
                if (data != null) {
                    CompletableFuture<Boolean> result = mqttClientService.publishSensorData(data);
                    if (result.get()) {
                        publishStatus.incrementCounter(sensorType);
                        publishCount.incrementAndGet();
                    } else {
                        publishStatus.incrementErrorCount();
                    }
                }
            } else {
                // 批量发布
                List<SensorData> dataList = dataReaderService.getNextBatchSensorData(sensorType, batchSize);
                if (!dataList.isEmpty()) {
                    CompletableFuture<Integer> result = mqttClientService.publishSensorDataBatch(dataList);
                    int successCount = result.get();
                    
                    for (int i = 0; i < successCount; i++) {
                        publishStatus.incrementCounter(sensorType);
                        publishCount.incrementAndGet();
                    }
                    
                    int errorCount = dataList.size() - successCount;
                    for (int i = 0; i < errorCount; i++) {
                        publishStatus.incrementErrorCount();
                    }
                }
            }
            
        } catch (Exception e) {
            logger.error("发布 {} 数据失败: {}", sensorType.getDisplayName(), e.getMessage(), e);
            publishStatus.incrementErrorCount();
        }
    }
    
    /**
     * 发布单条随机数据（用于测试）
     */
    public CompletableFuture<Boolean> publishSingleData(SensorType sensorType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                SensorData data = dataReaderService.getRandomSensorData(sensorType);
                if (data != null) {
                    boolean success = mqttClientService.publishSensorData(data).get();
                    if (success) {
                        publishStatus.incrementCounter(sensorType);
                        publishCount.incrementAndGet();
                        logger.info("手动发布成功: {}", data);
                        return true;
                    } else {
                        publishStatus.incrementErrorCount();
                        logger.warn("手动发布失败: {}", data);
                        return false;
                    }
                } else {
                    logger.warn("没有可用的 {} 数据", sensorType.getDisplayName());
                    return false;
                }
            } catch (Exception e) {
                logger.error("手动发布数据失败: {}", e.getMessage(), e);
                publishStatus.incrementErrorCount();
                return false;
            }
        });
    }
    
    /**
     * 更新统计信息
     */
    private void updateStatistics() {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(lastRateCalculation, now);
        
        if (duration.getSeconds() >= 1) {
            long currentCount = publishCount.get();
            long previousTotal = publishStatus.getTotalPublished();
            long newPublished = currentCount - previousTotal;
            
            double rate = newPublished / (double) duration.getSeconds();
            publishStatus.setPublishRate(Math.round(rate * 100.0) / 100.0);
            
            lastRateCalculation = now;
        }
    }
    
    /**
     * 获取发布状态
     */
    public PublishStatus getPublishStatus() {
        return publishStatus;
    }
    
    /**
     * 获取系统信息
     */
    public SystemInfo getSystemInfo() {
        SystemInfo info = new SystemInfo();
        
        // MQTT连接信息
        info.setMqttConnected(mqttClientService.isConnected());
        info.setMqttConnectionInfo(mqttClientService.getConnectionInfo());
        
        // 数据文件信息
        info.setDataFilesExist(dataReaderService.checkDataFilesExist());
        info.setTotalDataCount(dataReaderService.getTotalDataCount());
        info.setTemperatureCount(dataReaderService.getSensorDataCount(SensorType.TEMPERATURE));
        info.setHumidityCount(dataReaderService.getSensorDataCount(SensorType.HUMIDITY));
        info.setPressureCount(dataReaderService.getSensorDataCount(SensorType.PRESSURE));
        
        // 读取进度
        info.setReadProgress(dataReaderService.getReadProgress());
        
        // 发布配置
        info.setPublishInterval(publishInterval);
        info.setBatchSize(batchSize);
        info.setAutoStart(autoStart);
        
        return info;
    }
    
    /**
     * 重置统计数据
     */
    public void resetStatistics() {
        publishStatus.setTotalPublished(0);
        publishStatus.setTemperatureCount(0);
        publishStatus.setHumidityCount(0);
        publishStatus.setPressureCount(0);
        publishStatus.setErrorCount(0);
        publishStatus.setPublishRate(0.0);
        publishCount.set(0);
        
        dataReaderService.resetReadPointers();
        
        logger.info("统计数据已重置");
    }
    
    /**
     * 系统信息模型
     */
    public static class SystemInfo {
        private boolean mqttConnected;
        private MqttClientService.ConnectionInfo mqttConnectionInfo;
        private boolean dataFilesExist;
        private int totalDataCount;
        private int temperatureCount;
        private int humidityCount;
        private int pressureCount;
        private Object readProgress;
        private long publishInterval;
        private int batchSize;
        private boolean autoStart;
        
        // Getters and Setters
        public boolean isMqttConnected() { return mqttConnected; }
        public void setMqttConnected(boolean mqttConnected) { this.mqttConnected = mqttConnected; }
        
        public MqttClientService.ConnectionInfo getMqttConnectionInfo() { return mqttConnectionInfo; }
        public void setMqttConnectionInfo(MqttClientService.ConnectionInfo mqttConnectionInfo) { 
            this.mqttConnectionInfo = mqttConnectionInfo; 
        }
        
        public boolean isDataFilesExist() { return dataFilesExist; }
        public void setDataFilesExist(boolean dataFilesExist) { this.dataFilesExist = dataFilesExist; }
        
        public int getTotalDataCount() { return totalDataCount; }
        public void setTotalDataCount(int totalDataCount) { this.totalDataCount = totalDataCount; }
        
        public int getTemperatureCount() { return temperatureCount; }
        public void setTemperatureCount(int temperatureCount) { this.temperatureCount = temperatureCount; }
        
        public int getHumidityCount() { return humidityCount; }
        public void setHumidityCount(int humidityCount) { this.humidityCount = humidityCount; }
        
        public int getPressureCount() { return pressureCount; }
        public void setPressureCount(int pressureCount) { this.pressureCount = pressureCount; }
        
        public Object getReadProgress() { return readProgress; }
        public void setReadProgress(Object readProgress) { this.readProgress = readProgress; }
        
        public long getPublishInterval() { return publishInterval; }
        public void setPublishInterval(long publishInterval) { this.publishInterval = publishInterval; }
        
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        
        public boolean isAutoStart() { return autoStart; }
        public void setAutoStart(boolean autoStart) { this.autoStart = autoStart; }
    }
}