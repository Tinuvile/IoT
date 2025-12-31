package com.tinuvile.service;

import com.tinuvile.model.AnalysisData;
import com.tinuvile.model.PredictionData;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Apache Commons Math imports for advanced mathematical computations
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.exception.MathIllegalArgumentException;

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
     * 生成数据预测
     * 基于历史数据使用多种算法进行预测
     */
    public List<PredictionData> generatePrediction(SensorType sensorType, LocalDateTime startTime, LocalDateTime endTime, int predictionHours) {
        return generatePrediction(sensorType, startTime, endTime, predictionHours, "auto");
    }
    
    /**
     * 生成数据预测
     * 支持多种预测算法
     */
    public List<PredictionData> generatePrediction(SensorType sensorType, LocalDateTime startTime, LocalDateTime endTime, int predictionHours, String method) {
        try {
            logger.info("开始生成预测: sensorType={}, startTime={}, endTime={}, predictionHours={}, method={}", 
                    sensorType, startTime, endTime, predictionHours, method);
            
            // 获取历史数据用于预测
            List<SensorDataEntity> entities = sensorDataRepository
                    .findBySensorTypeAndTimestampBetween(sensorType, startTime, endTime);
            
            if (entities.isEmpty() || entities.size() < 3) {
                logger.warn("历史数据不足，无法进行预测: dataSize={}", entities.size());
                return Collections.emptyList();
            }
            
            // 按时间排序
            entities.sort((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()));
            
            // 根据方法选择预测算法
            switch (method.toLowerCase()) {
                case "linear":
                    return performLinearRegressionPrediction(entities, predictionHours);
                case "polynomial":
                    return performPolynomialRegressionPrediction(entities, predictionHours);
                case "moving_average":
                    return performMovingAveragePrediction(entities, predictionHours);
                case "exponential_smoothing":
                    return performExponentialSmoothingPrediction(entities, predictionHours);
                case "auto":
                default:
                    return performAutoPrediction(entities, predictionHours);
            }
            
        } catch (Exception e) {
            logger.error("生成预测失败: sensorType={}, error={}", sensorType, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 自动选择最佳预测算法 - 使用Apache Commons Math增强分析
     */
    private List<PredictionData> performAutoPrediction(List<SensorDataEntity> historicalData, int predictionHours) {
        try {
            int n = historicalData.size();
            double[] values = historicalData.stream().mapToDouble(e -> e.getValue().doubleValue()).toArray();
            
            // 使用DescriptiveStatistics分析数据特征
            DescriptiveStatistics stats = new DescriptiveStatistics(values);
            double skewness = stats.getSkewness();
            double kurtosis = stats.getKurtosis();
            
            // 计算数据的线性度和波动性
            double linearCorrelation = calculateLinearCorrelation(historicalData);
            double volatility = calculateVolatility(values);
            
            // 计算数据的趋势强度
            double trendStrength = calculateTrendStrength(values);
            
            logger.info("数据特征分析: n={}, 线性相关性={}, 波动性={}, 趋势强度={}, 偏度={}, 峰度={}", 
                    n, linearCorrelation, volatility, trendStrength, skewness, kurtosis);
            
            // 智能算法选择逻辑
            if (n < 5) {
                // 数据点太少，使用简单移动平均
                logger.info("数据点不足，选择移动平均预测算法");
                return performMovingAveragePrediction(historicalData, predictionHours);
            }
            
            // 高线性相关性且趋势明显
            if (Math.abs(linearCorrelation) > 0.85 && trendStrength > 0.3) {
                logger.info("高线性相关性且趋势明显，选择线性回归预测算法");
                return performLinearRegressionPrediction(historicalData, predictionHours);
            }
            
            // 数据平稳且波动性低
            if (volatility < 0.1 && trendStrength < 0.2) {
                logger.info("数据平稳且波动性低，选择移动平均预测算法");
                return performMovingAveragePrediction(historicalData, predictionHours);
            }
            
            // 足够数据点且存在非线性特征
            if (n > 8 && (Math.abs(skewness) > 0.5 || Math.abs(linearCorrelation) < 0.7)) {
                // 尝试多项式拟合，检验是否比线性拟合更好
                if (shouldUsePolynomialRegression(historicalData)) {
                    logger.info("检测到非线性特征，选择多项式回归预测算法");
                    return performPolynomialRegressionPrediction(historicalData, predictionHours);
                }
            }
            
            // 有明显趋势变化
            if (trendStrength > 0.4 || volatility > 0.2) {
                logger.info("存在趋势变化，选择指数平滑预测算法");
                return performExponentialSmoothingPrediction(historicalData, predictionHours);
            }
            
            // 默认情况：根据数据量选择
            if (n > 10) {
                logger.info("默认选择多项式回归预测算法");
                return performPolynomialRegressionPrediction(historicalData, predictionHours);
            } else {
                logger.info("默认选择指数平滑预测算法");
                return performExponentialSmoothingPrediction(historicalData, predictionHours);
            }
            
        } catch (Exception e) {
            logger.error("自动预测选择失败，使用线性回归: error={}", e.getMessage(), e);
            return performLinearRegressionPrediction(historicalData, predictionHours);
        }
    }
    
    /**
     * 计算趋势强度
     */
    private double calculateTrendStrength(double[] values) {
        if (values.length < 3) return 0;
        
        try {
            // 计算相邻差值的均值和标准差
            DescriptiveStatistics diffStats = new DescriptiveStatistics();
            for (int i = 1; i < values.length; i++) {
                diffStats.addValue(values[i] - values[i-1]);
            }
            
            double meanDiff = Math.abs(diffStats.getMean());
            double stdDiff = diffStats.getStandardDeviation();
            
            // 趋势强度 = 平均变化/变化标准差，值越大表示趋势越明显
            return stdDiff > 0 ? meanDiff / stdDiff : 0;
        } catch (Exception e) {
            logger.warn("趋势强度计算失败: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 判断是否应该使用多项式回归
     */
    private boolean shouldUsePolynomialRegression(List<SensorDataEntity> historicalData) {
        try {
            // 比较线性和二次多项式的拟合效果
            int n = historicalData.size();
            LocalDateTime baseTime = historicalData.get(0).getTimestamp();
            
            // 准备数据
            SimpleRegression linearRegression = new SimpleRegression();
            WeightedObservedPoints points = new WeightedObservedPoints();
            
            for (int i = 0; i < n; i++) {
                SensorDataEntity entity = historicalData.get(i);
                double x = java.time.Duration.between(baseTime, entity.getTimestamp()).toHours();
                double y = entity.getValue().doubleValue();
                
                linearRegression.addData(x, y);
                points.add(x, y);
            }
            
            double linearR2 = linearRegression.getRSquare();
            
            // 尝试二次多项式拟合
            try {
                PolynomialCurveFitter fitter = PolynomialCurveFitter.create(2);
                double[] polyCoeffs = fitter.fit(points.toList());
                PolynomialFunction polynomial = new PolynomialFunction(polyCoeffs);
                
                double[] xValues = new double[n];
                double[] yValues = new double[n];
                for (int i = 0; i < n; i++) {
                    SensorDataEntity entity = historicalData.get(i);
                    xValues[i] = java.time.Duration.between(baseTime, entity.getTimestamp()).toHours();
                    yValues[i] = entity.getValue().doubleValue();
                }
                
                double polyR2 = calculatePolynomialRSquare(xValues, yValues, polynomial);
                
                // 如果多项式拟合明显更好（R²提升超过0.05），则使用多项式
                boolean usePolynomial = (polyR2 - linearR2) > 0.05;
                logger.debug("拟合比较: 线性R²={}, 多项式R²={}, 选择多项式={}", linearR2, polyR2, usePolynomial);
                return usePolynomial;
                
            } catch (Exception e) {
                logger.debug("多项式拟合测试失败，使用线性: {}", e.getMessage());
                return false;
            }
            
        } catch (Exception e) {
            logger.warn("多项式回归判断失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 多项式回归预测 - 使用Apache Commons Math
     */
    private List<PredictionData> performPolynomialRegressionPrediction(List<SensorDataEntity> historicalData, int predictionHours) {
        List<PredictionData> predictions = new ArrayList<>();
        
        try {
            int n = historicalData.size();
            LocalDateTime baseTime = historicalData.get(0).getTimestamp();
            
            // 准备观测点数据
            WeightedObservedPoints points = new WeightedObservedPoints();
            double[] xValues = new double[n];
            double[] yValues = new double[n];
            
            for (int i = 0; i < n; i++) {
                SensorDataEntity entity = historicalData.get(i);
                double x = java.time.Duration.between(baseTime, entity.getTimestamp()).toHours();
                double y = entity.getValue().doubleValue();
                
                // 添加权重（最近的数据点权重更高）
                double weight = 1.0 + (i / (double) n) * 0.5; // 权重范围 1.0 到 1.5
                points.add(weight, x, y);
                
                xValues[i] = x;
                yValues[i] = y;
            }
            
            // 选择合适的多项式度数
            int degree = Math.min(3, n - 1); // 最高三次多项式，且不超过数据点数-1
            if (degree < 1) degree = 1;
            
            // 使用PolynomialCurveFitter进行拟合
            PolynomialCurveFitter fitter = PolynomialCurveFitter.create(degree);
            double[] coefficients = fitter.fit(points.toList());
            
            // 创建多项式函数
            PolynomialFunction polynomial = new PolynomialFunction(coefficients);
            
            // 计算拟合优度 (R²)
            double rSquare = calculatePolynomialRSquare(xValues, yValues, polynomial);
            
            logger.info("Commons Math多项式回归: degree={}, coefficients={}, R²={}", 
                    degree, java.util.Arrays.toString(coefficients), rSquare);
            
            // 生成预测数据
            LocalDateTime lastTime = historicalData.get(n - 1).getTimestamp();
            SensorType sensorType = historicalData.get(0).getSensorType();
            String unit = historicalData.get(0).getUnit();
            
            for (int i = 1; i <= predictionHours; i++) {
                LocalDateTime predictionTime = lastTime.plusHours(i);
                double xPred = java.time.Duration.between(baseTime, predictionTime).toHours();
                double yPred = polynomial.value(xPred);
                
                // 检查多项式外推的合理性（避免过度外推）
                double maxHistoricalX = xValues[n - 1];
                if (xPred > maxHistoricalX * 1.5) { // 超出历史范围50%时降低置信度
                    // 使用线性外推替代高次多项式外推
                    double lastY = yValues[n - 1];
                    double secondLastY = n > 1 ? yValues[n - 2] : lastY;
                    double trend = lastY - secondLastY;
                    yPred = lastY + trend * i;
                }
                
                // 应用传感器类型的合理范围限制
                yPred = applySensorRangeLimits(sensorType, yPred);
                
                PredictionData prediction = new PredictionData();
                prediction.setTimestamp(predictionTime);
                prediction.setPredictedValue(new BigDecimal(yPred).setScale(2, RoundingMode.HALF_UP));
                prediction.setSensorType(sensorType);
                prediction.setUnit(unit);
                prediction.setPredictionMethod("polynomial_regression");
                
                // 基于R²和预测距离计算置信度
                double distanceEffect = Math.max(0.3, 1.0 - (i - 1) * 0.04); // 距离衰减
                double confidence = Math.max(0.1, rSquare * distanceEffect);
                prediction.setConfidenceLevel(new BigDecimal(confidence).setScale(2, RoundingMode.HALF_UP));
                
                predictions.add(prediction);
            }
            
            logger.info("多项式回归预测成功生成{}个数据点，模型R²={}", predictions.size(), rSquare);
            
        } catch (MathIllegalArgumentException e) {
            logger.warn("多项式拟合失败，降级使用线性回归: {}", e.getMessage());
            return performLinearRegressionPrediction(historicalData, predictionHours);
        } catch (Exception e) {
            logger.error("多项式回归预测失败: error={}", e.getMessage(), e);
        }
        
        return predictions;
    }
    
    /**
     * 移动平均预测 - 使用Apache Commons Math
     */
    private List<PredictionData> performMovingAveragePrediction(List<SensorDataEntity> historicalData, int predictionHours) {
        List<PredictionData> predictions = new ArrayList<>();
        
        try {
            int n = historicalData.size();
            int windowSize = Math.min(8, Math.max(3, n / 3)); // 自适应窗口大小
            
            // 使用DescriptiveStatistics计算移动统计
            DescriptiveStatistics stats = new DescriptiveStatistics(windowSize);
            DescriptiveStatistics trendStats = new DescriptiveStatistics(windowSize - 1);
            
            // 添加最近的数据点
            double[] recentValues = new double[windowSize];
            for (int i = 0; i < windowSize; i++) {
                SensorDataEntity entity = historicalData.get(n - windowSize + i);
                double value = entity.getValue().doubleValue();
                stats.addValue(value);
                recentValues[i] = value;
            }
            
            // 计算移动平均和统计指标
            double movingAverage = stats.getMean();
            double standardDeviation = stats.getStandardDeviation();
            
            // 计算趋势（相邻点的差值）
            for (int i = 1; i < windowSize; i++) {
                double trend = recentValues[i] - recentValues[i - 1];
                trendStats.addValue(trend);
            }
            
            double averageTrend = trendStats.getMean();
            double trendStability = 1.0 / (1.0 + trendStats.getStandardDeviation()); // 趋势稳定性
            
            logger.info("Commons Math移动平均预测: 窗口大小={}, 平均值={}, 标准差={}, 平均趋势={}, 趋势稳定性={}", 
                    windowSize, movingAverage, standardDeviation, averageTrend, trendStability);
            
            // 生成预测数据
            LocalDateTime lastTime = historicalData.get(n - 1).getTimestamp();
            SensorType sensorType = historicalData.get(0).getSensorType();
            String unit = historicalData.get(0).getUnit();
            
            for (int i = 1; i <= predictionHours; i++) {
                LocalDateTime predictionTime = lastTime.plusHours(i);
                
                // 使用趋势衰减的移动平均预测
                double trendDecay = Math.pow(0.95, i - 1); // 趋势衰减因子
                double yPred = movingAverage + averageTrend * i * trendDecay;
                
                // 应用传感器类型的合理范围限制
                yPred = applySensorRangeLimits(sensorType, yPred);
                
                PredictionData prediction = new PredictionData();
                prediction.setTimestamp(predictionTime);
                prediction.setPredictedValue(new BigDecimal(yPred).setScale(2, RoundingMode.HALF_UP));
                prediction.setSensorType(sensorType);
                prediction.setUnit(unit);
                prediction.setPredictionMethod("moving_average");
                
                // 基于数据稳定性和趋势稳定性计算置信度
                double dataStability = Math.max(0.1, 1.0 - (standardDeviation / Math.abs(movingAverage)));
                double distanceEffect = Math.max(0.3, 1.0 - i * 0.03);
                double confidence = Math.max(0.2, (dataStability * 0.6 + trendStability * 0.4) * distanceEffect);
                prediction.setConfidenceLevel(new BigDecimal(confidence).setScale(2, RoundingMode.HALF_UP));
                
                predictions.add(prediction);
            }
            
            logger.info("移动平均预测成功生成{}个数据点，数据稳定性={}", predictions.size(), 
                    1.0 - (standardDeviation / Math.abs(movingAverage)));
            
        } catch (Exception e) {
            logger.error("移动平均预测失败: error={}", e.getMessage(), e);
        }
        
        return predictions;
    }
    
    /**
     * 指数平滑预测 - 使用Apache Commons Math增强
     */
    private List<PredictionData> performExponentialSmoothingPrediction(List<SensorDataEntity> historicalData, int predictionHours) {
        List<PredictionData> predictions = new ArrayList<>();
        
        try {
            int n = historicalData.size();
            double[] values = historicalData.stream().mapToDouble(e -> e.getValue().doubleValue()).toArray();
            
            // 使用DescriptiveStatistics分析数据特征以优化参数
            DescriptiveStatistics stats = new DescriptiveStatistics(values);
            double mean = stats.getMean();
            double coefficientOfVariation = stats.getStandardDeviation() / Math.abs(mean);
            
            // 根据数据波动性自适应调整平滑参数
            double alpha = Math.min(0.5, Math.max(0.1, 0.3 + coefficientOfVariation * 0.2)); // 0.1-0.5
            double beta = Math.min(0.3, Math.max(0.05, alpha * 0.4)); // 0.05-0.3
            
            logger.info("自适应指数平滑参数: alpha={}, beta={}, 变异系数={}", alpha, beta, coefficientOfVariation);
            
            // Holt双指数平滑
            double level = values[0];
            double trend = n > 1 ? values[1] - values[0] : 0;
            
            // 存储中间结果用于误差分析
            double[] fittedValues = new double[n];
            fittedValues[0] = level;
            
            // 指数平滑计算
            for (int i = 1; i < n; i++) {
                double prevLevel = level;
                level = alpha * values[i] + (1 - alpha) * (prevLevel + trend);
                trend = beta * (level - prevLevel) + (1 - beta) * trend;
                fittedValues[i] = level + trend;
            }
            
            // 计算模型拟合误差
            double mse = calculateMeanSquaredError(values, fittedValues);
            double mae = calculateMeanAbsoluteError(values, fittedValues);
            
            logger.info("指数平滑模型评估: level={}, trend={}, MSE={}, MAE={}", 
                    level, trend, mse, mae);
            
            // 生成预测数据
            LocalDateTime lastTime = historicalData.get(n - 1).getTimestamp();
            SensorType sensorType = historicalData.get(0).getSensorType();
            String unit = historicalData.get(0).getUnit();
            
            for (int i = 1; i <= predictionHours; i++) {
                LocalDateTime predictionTime = lastTime.plusHours(i);
                double yPred = level + trend * i;
                
                // 应用传感器类型的合理范围限制
                yPred = applySensorRangeLimits(sensorType, yPred);
                
                PredictionData prediction = new PredictionData();
                prediction.setTimestamp(predictionTime);
                prediction.setPredictedValue(new BigDecimal(yPred).setScale(2, RoundingMode.HALF_UP));
                prediction.setSensorType(sensorType);
                prediction.setUnit(unit);
                prediction.setPredictionMethod("exponential_smoothing");
                
                // 基于模型误差和预测距离计算置信度
                double errorFactor = Math.max(0.5, 1.0 - Math.sqrt(mse) / mean);
                double distanceEffect = Math.max(0.3, 1.0 - i * 0.02);
                double confidence = Math.max(0.3, errorFactor * distanceEffect);
                prediction.setConfidenceLevel(new BigDecimal(confidence).setScale(2, RoundingMode.HALF_UP));
                
                predictions.add(prediction);
            }
            
            logger.info("指数平滑预测成功生成{}个数据点，模型误差因子={}", predictions.size(), 
                    1.0 - Math.sqrt(mse) / mean);
            
        } catch (Exception e) {
            logger.error("指数平滑预测失败: error={}", e.getMessage(), e);
        }
        
        return predictions;
    }

    /**
     * 执行线性回归预测 - 使用Apache Commons Math
     */
    private List<PredictionData> performLinearRegressionPrediction(List<SensorDataEntity> historicalData, int predictionHours) {
        List<PredictionData> predictions = new ArrayList<>();
        
        try {
            int n = historicalData.size();
            LocalDateTime baseTime = historicalData.get(0).getTimestamp();
            
            // 使用Apache Commons Math的SimpleRegression
            SimpleRegression regression = new SimpleRegression();
            
            // 添加数据点到回归模型
            for (int i = 0; i < n; i++) {
                SensorDataEntity entity = historicalData.get(i);
                double x = java.time.Duration.between(baseTime, entity.getTimestamp()).toHours();
                double y = entity.getValue().doubleValue();
                regression.addData(x, y);
            }
            
            // 获取回归参数
            double slope = regression.getSlope();
            double intercept = regression.getIntercept();
            double rSquare = regression.getRSquare(); // 决定系数 R²
            double correlation = Math.sqrt(Math.abs(rSquare)) * (slope >= 0 ? 1 : -1); // 相关系数
            double meanSquareError = regression.getMeanSquareError(); // 均方误差
            
            logger.info("Commons Math线性回归参数: slope={}, intercept={}, R²={}, correlation={}, MSE={}", 
                    slope, intercept, rSquare, correlation, meanSquareError);
            
            // 生成预测数据
            LocalDateTime lastTime = historicalData.get(n - 1).getTimestamp();
            SensorType sensorType = historicalData.get(0).getSensorType();
            String unit = historicalData.get(0).getUnit();
            
            for (int i = 1; i <= predictionHours; i++) {
                LocalDateTime predictionTime = lastTime.plusHours(i);
                double xPred = java.time.Duration.between(baseTime, predictionTime).toHours();
                double yPred = regression.predict(xPred);
                
                // 应用传感器类型的合理范围限制
                yPred = applySensorRangeLimits(sensorType, yPred);
                
                PredictionData prediction = new PredictionData();
                prediction.setTimestamp(predictionTime);
                prediction.setPredictedValue(new BigDecimal(yPred).setScale(2, RoundingMode.HALF_UP));
                prediction.setSensorType(sensorType);
                prediction.setUnit(unit);
                prediction.setPredictionMethod("linear_regression");
                
                // 基于R²和预测距离计算置信度
                double distanceEffect = Math.max(0.5, 1.0 - i * 0.03); // 距离衰减
                double confidence = Math.max(0.2, rSquare * distanceEffect); 
                prediction.setConfidenceLevel(new BigDecimal(confidence).setScale(2, RoundingMode.HALF_UP));
                
                predictions.add(prediction);
            }
            
            logger.info("线性回归预测成功生成{}个数据点，模型R²={}", predictions.size(), rSquare);
            
        } catch (Exception e) {
            logger.error("线性回归预测失败: error={}", e.getMessage(), e);
        }
        
        return predictions;
    }
    
    
    /**
     * 计算线性相关系数 - 使用Apache Commons Math
     */
    private double calculateLinearCorrelation(List<SensorDataEntity> historicalData) {
        int n = historicalData.size();
        double[] x = new double[n];
        double[] y = new double[n];
        
        LocalDateTime baseTime = historicalData.get(0).getTimestamp();
        
        for (int i = 0; i < n; i++) {
            SensorDataEntity entity = historicalData.get(i);
            x[i] = java.time.Duration.between(baseTime, entity.getTimestamp()).toHours();
            y[i] = entity.getValue().doubleValue();
        }
        
        try {
            // 使用Pearson相关系数
            PearsonsCorrelation correlation = new PearsonsCorrelation();
            return correlation.correlation(x, y);
        } catch (Exception e) {
            logger.warn("Pearson相关性计算失败，使用备用方法: {}", e.getMessage());
            // 备用方法：使用SimpleRegression
            SimpleRegression regression = new SimpleRegression();
            for (int i = 0; i < n; i++) {
                regression.addData(x[i], y[i]);
            }
            double rSquare = regression.getRSquare();
            return Math.sqrt(Math.abs(rSquare)) * (regression.getSlope() >= 0 ? 1 : -1);
        }
    }
    
    /**
     * 计算数据波动性 - 使用Apache Commons Math
     */
    private double calculateVolatility(double[] values) {
        if (values.length < 2) return 0;
        
        try {
            DescriptiveStatistics stats = new DescriptiveStatistics(values);
            double mean = stats.getMean();
            double stdDev = stats.getStandardDeviation();
            
            // 返回变异系数（标准差/均值）
            return mean != 0 ? stdDev / Math.abs(mean) : 0;
        } catch (Exception e) {
            logger.warn("波动性计算失败: {}", e.getMessage());
            return 0;
        }
    }
    
    /**
     * 计算多项式拟合的R²值
     */
    private double calculatePolynomialRSquare(double[] x, double[] y, PolynomialFunction polynomial) {
        try {
            DescriptiveStatistics yStats = new DescriptiveStatistics(y);
            double yMean = yStats.getMean();
            
            double totalSumSquares = 0;
            double residualSumSquares = 0;
            
            for (int i = 0; i < x.length; i++) {
                double yPred = polynomial.value(x[i]);
                totalSumSquares += (y[i] - yMean) * (y[i] - yMean);
                residualSumSquares += (y[i] - yPred) * (y[i] - yPred);
            }
            
            return totalSumSquares != 0 ? 1.0 - (residualSumSquares / totalSumSquares) : 0;
        } catch (Exception e) {
            logger.warn("R²计算失败: {}", e.getMessage());
            return 0;
        }
    }
    
    
    /**
     * 计算均方误差 (MSE)
     */
    private double calculateMeanSquaredError(double[] actual, double[] predicted) {
        if (actual.length != predicted.length) {
            return Double.MAX_VALUE;
        }
        
        double sumSquaredErrors = 0;
        int count = 0;
        
        for (int i = 0; i < actual.length; i++) {
            if (!Double.isNaN(predicted[i])) {
                double error = actual[i] - predicted[i];
                sumSquaredErrors += error * error;
                count++;
            }
        }
        
        return count > 0 ? sumSquaredErrors / count : Double.MAX_VALUE;
    }
    
    /**
     * 计算平均绝对误差 (MAE)
     */
    private double calculateMeanAbsoluteError(double[] actual, double[] predicted) {
        if (actual.length != predicted.length) {
            return Double.MAX_VALUE;
        }
        
        double sumAbsoluteErrors = 0;
        int count = 0;
        
        for (int i = 0; i < actual.length; i++) {
            if (!Double.isNaN(predicted[i])) {
                sumAbsoluteErrors += Math.abs(actual[i] - predicted[i]);
                count++;
            }
        }
        
        return count > 0 ? sumAbsoluteErrors / count : Double.MAX_VALUE;
    }
    
    /**
     * 应用传感器类型的合理范围限制
     */
    private double applySensorRangeLimits(SensorType sensorType, double value) {
        switch (sensorType) {
            case temperature:
                // 温度范围: -40°C 到 80°C
                return Math.max(-40, Math.min(80, value));
            case humidity:
                // 湿度范围: 0% 到 100%
                return Math.max(0, Math.min(100, value));
            case pressure:
                // 气压范围: 800hPa 到 1200hPa
                return Math.max(800, Math.min(1200, value));
            case light:
                // 光照范围: 0lux 到 100000lux
                return Math.max(0, Math.min(100000, value));
            case air_quality:
                // 空气质量指数: 0 到 500
                return Math.max(0, Math.min(500, value));
            default:
                return value;
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
