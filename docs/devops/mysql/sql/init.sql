-- IoT数据管理系统数据库初始化脚本
-- 创建数据库
CREATE DATABASE IF NOT EXISTS iot_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE iot_db;

-- 创建传感器节点表
CREATE TABLE sensor_nodes (
    id INT AUTO_INCREMENT PRIMARY KEY,
    node_name VARCHAR(50) NOT NULL COMMENT '节点名称',
    location VARCHAR(100) NOT NULL COMMENT '节点位置',
    description TEXT COMMENT '节点描述',
    status ENUM('active', 'inactive', 'maintenance') DEFAULT 'active' COMMENT '节点状态',
    last_seen DATETIME COMMENT '最后上线时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_node_name (node_name),
    INDEX idx_location (location),
    INDEX idx_status (status)
) COMMENT '传感器节点表';

-- 创建传感器数据表
CREATE TABLE sensor_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id INT NOT NULL COMMENT '节点ID',
    sensor_type ENUM('temperature', 'humidity', 'pressure', 'light', 'air_quality') NOT NULL COMMENT '传感器类型',
    sensor_location VARCHAR(100) COMMENT '传感器具体位置',
    value DECIMAL(10,3) NOT NULL COMMENT '传感器数值',
    unit VARCHAR(10) NOT NULL COMMENT '单位',
    timestamp DATETIME NOT NULL COMMENT '数据时间戳',
    mqtt_topic VARCHAR(255) COMMENT 'MQTT主题',
    received_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '接收时间',
    raw_data JSON COMMENT '原始数据JSON',
    quality_score TINYINT DEFAULT 100 COMMENT '数据质量评分(0-100)',
    anomaly_detected BOOLEAN DEFAULT FALSE COMMENT '是否检测到异常',
    is_valid BOOLEAN DEFAULT TRUE COMMENT '数据是否有效',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间',
    FOREIGN KEY fk_sensor_node (node_id) REFERENCES sensor_nodes(id),
    INDEX idx_sensor_type (sensor_type),
    INDEX idx_timestamp (timestamp),
    INDEX idx_node_sensor (node_id, sensor_type),
    INDEX idx_timestamp_type (timestamp, sensor_type),
    INDEX idx_received_time (received_time),
    INDEX idx_location (sensor_location)
) COMMENT '传感器数据表';

-- 创建数据统计表
CREATE TABLE data_statistics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id INT NOT NULL COMMENT '节点ID',
    sensor_type ENUM('temperature', 'humidity', 'pressure', 'light', 'air_quality') NOT NULL COMMENT '传感器类型',
    stat_date DATE NOT NULL COMMENT '统计日期',
    stat_hour TINYINT COMMENT '统计小时(0-23)',
    avg_value DECIMAL(10,3) COMMENT '平均值',
    min_value DECIMAL(10,3) COMMENT '最小值',
    max_value DECIMAL(10,3) COMMENT '最大值',
    count_total INT DEFAULT 0 COMMENT '总数据条数',
    count_valid INT DEFAULT 0 COMMENT '有效数据条数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY fk_stat_node (node_id) REFERENCES sensor_nodes(id),
    UNIQUE KEY uk_stat_daily (node_id, sensor_type, stat_date, stat_hour),
    INDEX idx_stat_date (stat_date),
    INDEX idx_node_type_date (node_id, sensor_type, stat_date)
) COMMENT '数据统计表';

-- 创建告警规则表
CREATE TABLE alert_rules (
    id INT AUTO_INCREMENT PRIMARY KEY,
    node_id INT COMMENT '节点ID(NULL表示全局规则)',
    sensor_type ENUM('temperature', 'humidity', 'pressure', 'light', 'air_quality') NOT NULL COMMENT '传感器类型',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    condition_type ENUM('greater_than', 'less_than', 'range', 'deviation') NOT NULL COMMENT '条件类型',
    threshold_min DECIMAL(10,3) COMMENT '最小阈值',
    threshold_max DECIMAL(10,3) COMMENT '最大阈值',
    is_enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY fk_alert_node (node_id) REFERENCES sensor_nodes(id),
    INDEX idx_sensor_type_enabled (sensor_type, is_enabled)
) COMMENT '告警规则表';

-- 创建告警记录表
CREATE TABLE alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_id INT NOT NULL COMMENT '规则ID',
    data_id BIGINT NOT NULL COMMENT '触发告警的数据ID',
    alert_level ENUM('info', 'warning', 'error', 'critical') NOT NULL COMMENT '告警级别',
    alert_message TEXT NOT NULL COMMENT '告警消息',
    is_resolved BOOLEAN DEFAULT FALSE COMMENT '是否已解决',
    resolved_at DATETIME COMMENT '解决时间',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY fk_alert_rule (rule_id) REFERENCES alert_rules(id),
    FOREIGN KEY fk_alert_data (data_id) REFERENCES sensor_data(id),
    INDEX idx_level_created (alert_level, created_at),
    INDEX idx_resolved (is_resolved, created_at)
) COMMENT '告警记录表';

-- 插入示例传感器节点
INSERT INTO sensor_nodes (node_name, location, description, status) VALUES
('室内环境监测01', '实验室A区', '温湿度及空气质量监测', 'active'),
('室内环境监测02', '实验室B区', '温湿度及空气质量监测', 'active'),
('室外气象站01', '楼顶气象站', '户外环境监测站', 'active'),
('仓库环境监测', '设备仓库', '仓库环境监控', 'inactive');

-- 插入示例告警规则
INSERT INTO alert_rules (node_id, sensor_type, rule_name, condition_type, threshold_min, threshold_max, is_enabled) VALUES
(1, 'temperature', '室内温度过高告警', 'greater_than', NULL, 35.0, TRUE),
(1, 'temperature', '室内温度过低告警', 'less_than', 10.0, NULL, TRUE),
(1, 'humidity', '室内湿度异常', 'range', 30.0, 70.0, TRUE),
(2, 'temperature', '实验室B温度监控', 'range', 18.0, 28.0, TRUE),
(NULL, 'pressure', '气压异常告警', 'range', 950.0, 1050.0, TRUE);

-- 创建视图：最新传感器数据
CREATE VIEW v_latest_sensor_data AS
SELECT 
    sn.node_name,
    sn.location,
    sd.sensor_type,
    sd.value,
    sd.unit,
    sd.timestamp,
    sd.quality_score,
    sn.status as node_status
FROM sensor_data sd
JOIN sensor_nodes sn ON sd.node_id = sn.id
JOIN (
    SELECT node_id, sensor_type, MAX(timestamp) as max_timestamp
    FROM sensor_data
    WHERE is_valid = TRUE
    GROUP BY node_id, sensor_type
) latest ON sd.node_id = latest.node_id 
    AND sd.sensor_type = latest.sensor_type 
    AND sd.timestamp = latest.max_timestamp
WHERE sd.is_valid = TRUE;

-- 创建视图：日统计数据
CREATE VIEW v_daily_statistics AS
SELECT 
    sn.node_name,
    sn.location,
    ds.sensor_type,
    ds.stat_date,
    ds.avg_value,
    ds.min_value,
    ds.max_value,
    ds.count_valid,
    ds.count_total,
    ROUND(ds.count_valid * 100.0 / ds.count_total, 2) as data_quality_percent
FROM data_statistics ds
JOIN sensor_nodes sn ON ds.node_id = sn.id
WHERE ds.stat_hour IS NULL  -- 日统计数据
ORDER BY ds.stat_date DESC, sn.node_name, ds.sensor_type;

-- 创建存储过程：计算日统计数据
DELIMITER $$
CREATE PROCEDURE CalculateDailyStatistics(IN target_date DATE)
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_node_id INT;
    DECLARE v_sensor_type VARCHAR(20);
    
    DECLARE cur CURSOR FOR 
        SELECT DISTINCT node_id, sensor_type 
        FROM sensor_data 
        WHERE DATE(timestamp) = target_date AND is_valid = TRUE;
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    stat_loop: LOOP
        FETCH cur INTO v_node_id, v_sensor_type;
        IF done THEN
            LEAVE stat_loop;
        END IF;
        
        INSERT INTO data_statistics (
            node_id, sensor_type, stat_date, stat_hour,
            avg_value, min_value, max_value, count_total, count_valid
        )
        SELECT 
            v_node_id,
            v_sensor_type,
            target_date,
            NULL,
            AVG(value),
            MIN(value),
            MAX(value),
            COUNT(*),
            COUNT(*)
        FROM sensor_data
        WHERE node_id = v_node_id 
            AND sensor_type = v_sensor_type
            AND DATE(timestamp) = target_date
            AND is_valid = TRUE
        ON DUPLICATE KEY UPDATE
            avg_value = VALUES(avg_value),
            min_value = VALUES(min_value),
            max_value = VALUES(max_value),
            count_total = VALUES(count_total),
            count_valid = VALUES(count_valid),
            updated_at = CURRENT_TIMESTAMP;
            
    END LOOP;
    
    CLOSE cur;
END$$
DELIMITER ;

-- 创建存储过程：数据质量检查
DELIMITER $$
CREATE PROCEDURE CheckDataQuality()
BEGIN
    -- 标记异常数据为无效
    UPDATE sensor_data SET is_valid = FALSE, quality_score = 0
    WHERE (
        (sensor_type = 'temperature' AND (value < -50 OR value > 100)) OR
        (sensor_type = 'humidity' AND (value < 0 OR value > 100)) OR
        (sensor_type = 'pressure' AND (value < 800 OR value > 1200))
    ) AND is_valid = TRUE;
    
    -- 更新质量评分
    UPDATE sensor_data SET quality_score = 
        CASE 
            WHEN timestamp > DATE_SUB(NOW(), INTERVAL 1 HOUR) THEN 100
            WHEN timestamp > DATE_SUB(NOW(), INTERVAL 6 HOUR) THEN 90
            WHEN timestamp > DATE_SUB(NOW(), INTERVAL 24 HOUR) THEN 80
            ELSE 70
        END
    WHERE is_valid = TRUE AND quality_score = 100;
END$$
DELIMITER ;

-- 创建事件调度器（每小时执行数据质量检查）
-- CREATE EVENT evt_data_quality_check
-- ON SCHEDULE EVERY 1 HOUR
-- DO CALL CheckDataQuality();

COMMIT;