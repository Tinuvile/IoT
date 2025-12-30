package com.tinuvile.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tinuvile.model.SensorData;
import com.tinuvile.model.SensorType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据文件读取服务
 * 负责从JSON数据文件中读取传感器数据
 * 
 * @author tinuvile
 */
@Service
public class DataReaderService {

    private static final Logger logger = LoggerFactory.getLogger(DataReaderService.class);

    @Value("${publisher.data.directory}")
    private String dataDirectory;

    @Value("${publisher.data.files.temperature}")
    private String temperatureFile;

    @Value("${publisher.data.files.humidity}")
    private String humidityFile;

    @Value("${publisher.data.files.pressure}")
    private String pressureFile;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 缓存所有传感器数据，按类型分组
    private final Map<SensorType, List<SensorData>> sensorDataCache = new ConcurrentHashMap<>();

    // 当前读取位置指针
    private final Map<SensorType, Integer> readPointers = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        logger.info("初始化数据读取服务...");
        logger.info("数据目录: {}", dataDirectory);

        // 初始化读取位置指针
        for (SensorType type : SensorType.values()) {
            readPointers.put(type, 0);
        }

        loadAllSensorData();
    }

    /**
     * 加载所有传感器数据到缓存
     */
    public void loadAllSensorData() {
        logger.info("开始加载传感器数据文件...");

        loadSensorTypeData(SensorType.TEMPERATURE, temperatureFile);
        loadSensorTypeData(SensorType.HUMIDITY, humidityFile);
        loadSensorTypeData(SensorType.PRESSURE, pressureFile);

        logger.info("数据加载完成！温度: {} 条, 湿度: {} 条, 气压: {} 条",
                getSensorDataCount(SensorType.TEMPERATURE),
                getSensorDataCount(SensorType.HUMIDITY),
                getSensorDataCount(SensorType.PRESSURE));
    }

    /**
     * 加载指定类型的传感器数据
     */
    private void loadSensorTypeData(SensorType sensorType, String filename) {
        try {
            Path filePath = Paths.get(dataDirectory, filename);
            if (!Files.exists(filePath)) {
                logger.warn("数据文件不存在: {}", filePath);
                sensorDataCache.put(sensorType, new ArrayList<>());
                return;
            }

            logger.info("读取 {} 数据文件: {}", sensorType.getDisplayName(), filePath);

            List<String> lines = Files.readAllLines(filePath);
            List<SensorData> dataList = new ArrayList<>();

            for (String line : lines) {
                if (line.trim().isEmpty())
                    continue;

                try {
                    JsonNode jsonNode = objectMapper.readTree(line);
                    List<SensorData> lineData = parseLine(jsonNode, sensorType);
                    dataList.addAll(lineData);
                } catch (Exception e) {
                    logger.warn("解析数据行失败: {}, 错误: {}", line, e.getMessage());
                }
            }

            // 按时间排序
            dataList.sort(Comparator.comparing(SensorData::getTimestamp));

            sensorDataCache.put(sensorType, dataList);
            logger.info("成功加载 {} 数据: {} 条记录", sensorType.getDisplayName(), dataList.size());

        } catch (IOException e) {
            logger.error("读取文件失败: {}", filename, e);
            sensorDataCache.put(sensorType, new ArrayList<>());
        }
    }

    /**
     * 解析JSON数据行
     * 格式: {"2014-02-13T06:20:00": "3.0", "2014-02-13T13:50:00": "7.0", ...}
     */
    private List<SensorData> parseLine(JsonNode jsonNode, SensorType sensorType) {
        List<SensorData> results = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            try {
                String timestampStr = field.getKey();
                String valueStr = field.getValue().asText();

                // 跳过空值或无效值
                if (valueStr == null || valueStr.trim().isEmpty()) {
                    logger.debug("跳过空值数据项: {}", timestampStr);
                    continue;
                }

                LocalDateTime timestamp = LocalDateTime.parse(timestampStr, formatter);
                Double value = Double.parseDouble(valueStr);

                SensorData sensorData = new SensorData(timestamp, sensorType, value, sensorType.getUnit());
                results.add(sensorData);

            } catch (Exception e) {
                logger.debug("解析数据项失败: {} -> {}", field.getKey(), field.getValue().asText(), e);
            }
        }

        return results;
    }

    /**
     * 获取下一个传感器数据
     */
    public SensorData getNextSensorData(SensorType sensorType) {
        List<SensorData> dataList = sensorDataCache.get(sensorType);
        if (dataList == null || dataList.isEmpty()) {
            return null;
        }

        int currentPointer = readPointers.get(sensorType);
        if (currentPointer >= dataList.size()) {
            // 重置到开头，循环读取
            currentPointer = 0;
            readPointers.put(sensorType, 0);
            logger.info("{} 数据读取完毕，重新开始循环读取", sensorType.getDisplayName());
        }

        SensorData data = dataList.get(currentPointer);
        readPointers.put(sensorType, currentPointer + 1);

        return data;
    }

    /**
     * 获取多个传感器数据
     */
    public List<SensorData> getNextBatchSensorData(SensorType sensorType, int batchSize) {
        List<SensorData> results = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            SensorData data = getNextSensorData(sensorType);
            if (data != null) {
                results.add(data);
            }
        }
        return results;
    }

    /**
     * 随机获取一个传感器数据（用于模拟实时数据）
     */
    public SensorData getRandomSensorData(SensorType sensorType) {
        List<SensorData> dataList = sensorDataCache.get(sensorType);
        if (dataList == null || dataList.isEmpty()) {
            return null;
        }

        Random random = new Random();
        SensorData originalData = dataList.get(random.nextInt(dataList.size()));

        // 创建一个新的数据对象，时间戳设为当前时间
        SensorData newData = new SensorData(
                LocalDateTime.now(),
                originalData.getSensorType(),
                originalData.getValue(),
                originalData.getUnit());
        newData.setNodeId(originalData.getNodeId());
        newData.setLocation(originalData.getLocation());

        return newData;
    }

    /**
     * 获取传感器数据总数
     */
    public int getSensorDataCount(SensorType sensorType) {
        List<SensorData> dataList = sensorDataCache.get(sensorType);
        return dataList != null ? dataList.size() : 0;
    }

    /**
     * 获取所有传感器数据总数
     */
    public int getTotalDataCount() {
        return sensorDataCache.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * 重置读取指针
     */
    public void resetReadPointers() {
        for (SensorType type : SensorType.values()) {
            readPointers.put(type, 0);
        }
        logger.info("已重置所有数据读取指针");
    }

    /**
     * 获取当前读取进度
     */
    public Map<String, Object> getReadProgress() {
        Map<String, Object> progress = new HashMap<>();

        for (SensorType type : SensorType.values()) {
            Map<String, Object> typeProgress = new HashMap<>();
            int current = readPointers.get(type);
            int total = getSensorDataCount(type);
            double percentage = total > 0 ? (double) current / total * 100 : 0;

            typeProgress.put("current", current);
            typeProgress.put("total", total);
            typeProgress.put("percentage", Math.round(percentage * 100.0) / 100.0);

            progress.put(type.getCode(), typeProgress);
        }

        return progress;
    }

    /**
     * 检查数据文件是否存在
     */
    public boolean checkDataFilesExist() {
        String[] files = { temperatureFile, humidityFile, pressureFile };
        for (String filename : files) {
            Path filePath = Paths.get(dataDirectory, filename);
            if (!Files.exists(filePath)) {
                logger.warn("数据文件不存在: {}", filePath);
                return false;
            }
        }
        return true;
    }
}