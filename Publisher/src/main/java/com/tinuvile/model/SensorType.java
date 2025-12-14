package com.tinuvile.model;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 传感器类型枚举
 * 
 * @author tinuvile
 */
public enum SensorType {
    TEMPERATURE("temperature", "°C", "温度"),
    HUMIDITY("humidity", "%RH", "湿度"),
    PRESSURE("pressure", "hPa", "气压");
    
    private final String code;
    private final String unit;
    private final String displayName;
    
    SensorType(String code, String unit, String displayName) {
        this.code = code;
        this.unit = unit;
        this.displayName = displayName;
    }
    
    @JsonValue
    public String getCode() {
        return code;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public static SensorType fromCode(String code) {
        for (SensorType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown sensor type code: " + code);
    }
    
    @Override
    public String toString() {
        return displayName;
    }
}