package com.tinuvile.converter;

import com.tinuvile.model.SensorType;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * SensorType枚举转换器
 * 用于在数据库字符串和枚举之间进行转换
 * 
 * @author tinuvile
 */
@Converter(autoApply = true)
public class SensorTypeConverter implements AttributeConverter<SensorType, String> {

    @Override
    public String convertToDatabaseColumn(SensorType sensorType) {
        if (sensorType == null) {
            return null;
        }
        return sensorType.getCode();
    }

    @Override
    public SensorType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        return SensorType.fromCode(dbData);
    }
}
