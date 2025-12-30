package com.tinuvile.model;

/**
 * 传感器类型枚举
 * 
 * @author tinuvile
 */
public enum SensorType {
    temperature("temperature", "温度", "°C"),
    humidity("humidity", "湿度", "%"),
    pressure("pressure", "气压", "hPa"),
    light("light", "光照", "lux"),
    air_quality("air_quality", "空气质量", "AQI");

    private final String value;
    private final String displayName;
    private final String unit;

    SensorType(String value, String displayName, String unit) {
        this.value = value;
        this.displayName = displayName;
        this.unit = unit;
    }

    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getUnit() {
        return unit;
    }

    public static SensorType fromValue(String value) {
        for (SensorType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sensor type: " + value);
    }
}
