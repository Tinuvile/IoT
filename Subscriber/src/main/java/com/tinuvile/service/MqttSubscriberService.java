package com.tinuvile.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinuvile.model.ReceivedData;
import com.tinuvile.model.SensorData;
import com.tinuvile.model.SensorType;
import com.tinuvile.model.SubscriberStatus;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MQTT订阅服务
 * 负责连接MQTT代理并订阅传感器数据
 * 
 * @author tinuvile
 */
@Service
public class MqttSubscriberService implements MqttCallback {

    private static final Logger logger = LoggerFactory.getLogger(MqttSubscriberService.class);

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

    @Value("${subscriber.analysis.auto-start:true}")
    private boolean autoStart;

    @Autowired
    private DataStorageService dataStorageService;

    private MqttClient mqttClient;
    private final ObjectMapper objectMapper;
    private volatile boolean connected = false;
    private volatile boolean subscribing = false;
    private final SubscriberStatus subscriberStatus = new SubscriberStatus();

    // 用于计算接收速率
    private long lastTotalReceived = 0;
    private LocalDateTime lastRateCalculation = LocalDateTime.now();

    // 防止连接丢失时无限创建重连线程
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);

    // 构造函数中初始化ObjectMapper
    public MqttSubscriberService() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules(); // 自动注册所有可用模块，包括JSR310
    }

    @PostConstruct
    public void init() {
        logger.info("初始化MQTT订阅客户端...");
        logger.info("Broker URL: {}", brokerUrl);
        logger.info("Client ID: {}", clientId);
        try {
            connectToBroker();
        } catch (Exception e) {
            // 不要因为MQTT未启动导致整个Web应用起不来
            logger.warn("初始化阶段连接MQTT失败（将保持Web可用，稍后可手动点击开始订阅重试）: {}", e.getMessage());
        }

        // 如果配置了自动启动，则自动开始订阅
        if (autoStart) {
            logger.info("auto-start 配置为 true，自动开始订阅...");
            // 延迟一下确保连接稳定
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // 等待2秒确保连接稳定
                    startSubscribing();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        } else {
            logger.info("auto-start 配置为 false，等待手动开始订阅...");
        }
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
            connectOptions.setCleanSession(false); // 订阅者使用持久会话
            connectOptions.setKeepAliveInterval(60);
            // 避免Web接口“开始订阅”长时间卡住
            connectOptions.setConnectionTimeout(8);
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

            logger.info("MQTT订阅客户端连接成功！等待用户手动开始订阅...");

        } catch (MqttException e) {
            logger.error("MQTT连接失败: {}", e.getMessage(), e);
            connected = false;
            // 不要抛出异常导致应用上下文失败/重启；保持Web可用
            return;
        }
    }

    /**
     * 开始订阅所有传感器主题
     */
    public CompletableFuture<Boolean> startSubscribing() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!isConnected()) {
                    logger.warn("MQTT客户端未连接，尝试重连...");
                    connectToBroker();
                }
                if (!isConnected()) {
                    logger.warn("MQTT仍未连接，无法开始订阅");
                    subscriberStatus.setSubscribing(false);
                    return false;
                }

                if (subscribing) {
                    logger.warn("已在订阅中");
                    return true;
                }

                logger.info("开始订阅传感器数据主题...");

                // 订阅所有传感器主题
                String[] topics = { temperatureTopic, humidityTopic, pressureTopic };
                int[] qosLevels = { qos, qos, qos };

                mqttClient.subscribe(topics, qosLevels);

                subscribing = true;
                subscriberStatus.setSubscribing(true);

                // 初始化速率计算变量
                lastTotalReceived = subscriberStatus.getTotalReceived();
                lastRateCalculation = LocalDateTime.now();

                logger.info("成功订阅主题: {}", String.join(", ", topics));
                return true;

            } catch (Exception e) {
                logger.error("订阅主题失败: {}", e.getMessage(), e);
                subscribing = false;
                subscriberStatus.setSubscribing(false);
                return false;
            }
        });
    }

    /**
     * 停止订阅
     */
    public CompletableFuture<Boolean> stopSubscribing() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!subscribing) {
                    logger.warn("未在订阅中");
                    return true;
                }

                logger.info("停止订阅传感器数据主题...");

                if (mqttClient != null && mqttClient.isConnected()) {
                    // 取消订阅所有主题
                    String[] topics = { temperatureTopic, humidityTopic, pressureTopic };
                    mqttClient.unsubscribe(topics);
                }

                subscribing = false;
                subscriberStatus.setSubscribing(false);

                logger.info("已停止订阅");
                return true;

            } catch (MqttException e) {
                logger.error("停止订阅失败: {}", e.getMessage(), e);
                return false;
            }
        });
    }

    /**
     * 检查连接状态
     */
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected() && connected;
    }

    /**
     * 检查订阅状态
     */
    public boolean isSubscribing() {
        return subscribing;
    }

    /**
     * 获取订阅状态
     */
    public SubscriberStatus getSubscriberStatus() {
        // 更新接收速率
        updateReceiveRate();
        return subscriberStatus;
    }

    /**
     * 更新接收速率
     */
    private void updateReceiveRate() {
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(lastRateCalculation, now);

        if (duration.getSeconds() >= 1) {
            long currentTotal = subscriberStatus.getTotalReceived();
            long newReceived = currentTotal - lastTotalReceived;

            double rate = newReceived / (double) duration.getSeconds();
            subscriberStatus.setReceiveRate(Math.round(rate * 100.0) / 100.0);

            lastTotalReceived = currentTotal;
            lastRateCalculation = now;
        }
    }

    /**
     * 定期更新接收速率（每秒执行一次）
     */
    @Scheduled(fixedRate = 1000)
    public void scheduledUpdateReceiveRate() {
        if (subscribing) {
            updateReceiveRate();
        }
    }

    /**
     * 获取连接信息
     */
    public ConnectionInfo getConnectionInfo() {
        ConnectionInfo info = new ConnectionInfo();
        info.setBrokerUrl(brokerUrl);
        info.setClientId(clientId);
        info.setConnected(isConnected());
        info.setSubscribing(isSubscribing());
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
     * 根据主题获取传感器类型
     */
    private SensorType getSensorTypeFromTopic(String topic) {
        if (topic.equals(temperatureTopic)) {
            return SensorType.TEMPERATURE;
        } else if (topic.equals(humidityTopic)) {
            return SensorType.HUMIDITY;
        } else if (topic.equals(pressureTopic)) {
            return SensorType.PRESSURE;
        } else {
            throw new IllegalArgumentException("未知的主题: " + topic);
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        try {
            subscribing = false;
            subscriberStatus.setSubscribing(false);

            if (mqttClient != null && mqttClient.isConnected()) {
                // 先停止订阅
                stopSubscribing().get();
                // 再断开连接
                mqttClient.disconnect();
                logger.info("MQTT订阅客户端已断开连接");
            }
            connected = false;
        } catch (Exception e) {
            logger.error("断开MQTT连接失败: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("清理MQTT订阅客户端资源...");
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
        final boolean shouldResubscribe = this.subscribing;
        connected = false;
        subscribing = false;
        subscriberStatus.setSubscribing(false);

        // 自动重连逻辑：仅允许一个重连线程在跑，避免线程爆炸导致Web卡死
        if (!reconnecting.compareAndSet(false, true)) {
            return;
        }

        new Thread(() -> {
            try {
                Thread.sleep(5000); // 等待5秒后重连
                logger.info("尝试重新连接MQTT代理...");
                connectToBroker();

                // 如果之前在订阅，重新订阅
                if (shouldResubscribe) {
                    Thread.sleep(2000); // 等待连接稳定
                    startSubscribing();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                reconnecting.set(false);
            }
        }).start();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        try {
            String payload = new String(message.getPayload());
            logger.info("收到MQTT消息: {} -> {}", topic,
                    payload.substring(0, Math.min(100, payload.length())) + (payload.length() > 100 ? "..." : ""));

            // 解析传感器数据
            SensorData sensorData = objectMapper.readValue(payload, SensorData.class);

            // 验证数据类型是否与主题匹配
            SensorType expectedType = getSensorTypeFromTopic(topic);
            if (sensorData.getSensorType() != expectedType) {
                logger.warn("数据类型不匹配: 主题={}, 期望={}, 实际={}",
                        topic, expectedType, sensorData.getSensorType());
                subscriberStatus.incrementErrorCount();
                return;
            }

            // 创建接收数据记录
            ReceivedData receivedData = new ReceivedData(sensorData, topic);

            // 存储数据
            dataStorageService.storeData(receivedData);

            // 更新统计信息
            subscriberStatus.incrementCounter(sensorData.getSensorType());

            logger.info("成功存储传感器数据: {} 类型={}, 值={}",
                    sensorData.getSensorType(), sensorData.getSensorType(), sensorData.getValue());

            // 更新接收速率（每次接收时计算）
            updateReceiveRate();

        } catch (Exception e) {
            logger.error("处理MQTT消息失败: topic={}, message={}, error={}",
                    topic, new String(message.getPayload()), e.getMessage(), e);
            subscriberStatus.incrementErrorCount();
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // 订阅者通常不需要发送消息，但保留接口实现
        logger.debug("消息传送完成: {}", token.getMessageId());
    }

    /**
     * 连接信息模型
     */
    public static class ConnectionInfo {
        private String brokerUrl;
        private String clientId;
        private String serverURI;
        private boolean connected;
        private boolean subscribing;
        private String username;

        // Getters and Setters
        public String getBrokerUrl() {
            return brokerUrl;
        }

        public void setBrokerUrl(String brokerUrl) {
            this.brokerUrl = brokerUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getServerURI() {
            return serverURI;
        }

        public void setServerURI(String serverURI) {
            this.serverURI = serverURI;
        }

        public boolean isConnected() {
            return connected;
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public boolean isSubscribing() {
            return subscribing;
        }

        public void setSubscribing(boolean subscribing) {
            this.subscribing = subscribing;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}
