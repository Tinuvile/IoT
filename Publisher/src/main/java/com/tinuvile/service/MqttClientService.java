package com.tinuvile.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinuvile.model.SensorData;
import com.tinuvile.model.SensorType;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.CompletableFuture;

/**
 * MQTT客户端服务
 * 负责连接MQTT代理并发布传感器数据
 * 
 * @author tinuvile
 */
@Service
public class MqttClientService implements MqttCallback {
    
    private static final Logger logger = LoggerFactory.getLogger(MqttClientService.class);
    
    @Value("${mqtt.broker-url}")
    private String brokerUrl;
    
    @Value("${mqtt.client-id}")
    private String clientId;
    
    @Value("${mqtt.username:}")
    private String username;
    
    @Value("${mqtt.password:}")
    private String password;
    
    @Value("${mqtt.topics.temperature}")
    private String temperatureTopic;
    
    @Value("${mqtt.topics.humidity}")
    private String humidityTopic;
    
    @Value("${mqtt.topics.pressure}")
    private String pressureTopic;
    
    @Value("${mqtt.qos}")
    private int qos;
    
    @Value("${mqtt.retained}")
    private boolean retained;
    
    private MqttClient mqttClient;
    private final ObjectMapper objectMapper;
    private volatile boolean connected = false;
    
    // 构造函数中初始化ObjectMapper
    public MqttClientService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // 自动注册所有可用模块，包括JSR310
    }
    
    @PostConstruct
    public void init() {
        logger.info("初始化MQTT客户端...");
        logger.info("Broker URL: {}", brokerUrl);
        logger.info("Client ID: {}", clientId);
        connectToBroker();
    }
    
    /**
     * 连接到MQTT代理
     */
    public void connectToBroker() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                logger.info("MQTT客户端已连接");
                return;
            }
            
            logger.info("连接到MQTT代理: {}", brokerUrl);
            
            MemoryPersistence persistence = new MemoryPersistence();
            mqttClient = new MqttClient(brokerUrl, clientId, persistence);
            
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            connectOptions.setKeepAliveInterval(60);
            connectOptions.setConnectionTimeout(30);
            connectOptions.setAutomaticReconnect(true);
            
            // 设置用户名密码（如果有）
            if (username != null && !username.trim().isEmpty()) {
                connectOptions.setUserName(username);
                if (password != null && !password.trim().isEmpty()) {
                    connectOptions.setPassword(password.toCharArray());
                }
            }
            
            // 设置回调
            mqttClient.setCallback(this);
            
            // 连接
            mqttClient.connect(connectOptions);
            connected = true;
            
            logger.info("MQTT客户端连接成功！");
            
        } catch (MqttException e) {
            logger.error("MQTT连接失败: {}", e.getMessage(), e);
            connected = false;
            throw new RuntimeException("MQTT连接失败", e);
        }
    }
    
    /**
     * 发布传感器数据
     */
    public CompletableFuture<Boolean> publishSensorData(SensorData sensorData) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isConnected()) {
                    logger.warn("MQTT客户端未连接，尝试重连...");
                    connectToBroker();
                }
                
                String topic = getTopicForSensorType(sensorData.getSensorType());
                String jsonPayload = objectMapper.writeValueAsString(sensorData);
                
                MqttMessage message = new MqttMessage(jsonPayload.getBytes());
                message.setQos(qos);
                message.setRetained(retained);
                
                mqttClient.publish(topic, message);
                
                logger.debug("发布数据成功: {} -> {}", topic, jsonPayload);
                return true;
                
            } catch (Exception e) {
                logger.error("发布数据失败: {}", e.getMessage(), e);
                return false;
            }
        });
    }
    
    /**
     * 批量发布传感器数据
     */
    public CompletableFuture<Integer> publishSensorDataBatch(java.util.List<SensorData> dataList) {
        return CompletableFuture.supplyAsync(() -> {
            int successCount = 0;
            for (SensorData data : dataList) {
                try {
                    boolean success = publishSensorData(data).get();
                    if (success) {
                        successCount++;
                    }
                } catch (Exception e) {
                    logger.error("批量发布数据项失败: {}", e.getMessage());
                }
            }
            logger.info("批量发布完成: {}/{} 成功", successCount, dataList.size());
            return successCount;
        });
    }
    
    /**
     * 根据传感器类型获取对应的MQTT主题
     */
    private String getTopicForSensorType(SensorType sensorType) {
        switch (sensorType) {
            case TEMPERATURE:
                return temperatureTopic;
            case HUMIDITY:
                return humidityTopic;
            case PRESSURE:
                return pressureTopic;
            default:
                throw new IllegalArgumentException("未知的传感器类型: " + sensorType);
        }
    }
    
    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected() && connected;
    }
    
    /**
     * 获取连接信息
     */
    public ConnectionInfo getConnectionInfo() {
        ConnectionInfo info = new ConnectionInfo();
        info.setBrokerUrl(brokerUrl);
        info.setClientId(clientId);
        info.setConnected(isConnected());
        info.setUsername(username);
        
        if (mqttClient != null) {
            try {
                info.setServerURI(mqttClient.getServerURI());
            } catch (Exception e) {
                info.setServerURI("N/A");
            }
        }
        
        return info;
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                logger.info("MQTT客户端已断开连接");
            }
            connected = false;
        } catch (MqttException e) {
            logger.error("断开MQTT连接失败: {}", e.getMessage(), e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        logger.info("清理MQTT客户端资源...");
        disconnect();
        try {
            if (mqttClient != null) {
                mqttClient.close();
            }
        } catch (MqttException e) {
            logger.error("关闭MQTT客户端失败: {}", e.getMessage(), e);
        }
    }
    
    // MQTT回调方法
    @Override
    public void connectionLost(Throwable cause) {
        logger.warn("MQTT连接丢失: {}", cause.getMessage());
        connected = false;
        
        // 自动重连逻辑
        new Thread(() -> {
            try {
                Thread.sleep(5000); // 等待5秒后重连
                logger.info("尝试重新连接MQTT代理...");
                connectToBroker();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // Publisher通常不需要接收消息，但可以用于调试
        logger.debug("收到消息: {} -> {}", topic, new String(message.getPayload()));
    }
    
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        logger.debug("消息发送完成: {}", token.getMessageId());
    }
    
    /**
     * 连接信息模型
     */
    public static class ConnectionInfo {
        private String brokerUrl;
        private String clientId;
        private String serverURI;
        private boolean connected;
        private String username;
        
        // Getters and Setters
        public String getBrokerUrl() { return brokerUrl; }
        public void setBrokerUrl(String brokerUrl) { this.brokerUrl = brokerUrl; }
        
        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        
        public String getServerURI() { return serverURI; }
        public void setServerURI(String serverURI) { this.serverURI = serverURI; }
        
        public boolean isConnected() { return connected; }
        public void setConnected(boolean connected) { this.connected = connected; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
}