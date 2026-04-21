package com.emp_management.shared.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.sql.Time;
import java.time.LocalTime;

@Converter
public class SafeLocalTimeConverter implements AttributeConverter<LocalTime, Time> {

    @Override
    public Time convertToDatabaseColumn(LocalTime attribute) {
        if (attribute == null) return null;
        return Time.valueOf(attribute);
    }

    @Override
    public LocalTime convertToEntityAttribute(Time dbData) {
        if (dbData == null) return null;
        try {
            return dbData.toLocalTime();
        } catch (Exception e) {
            // Defensive: bad value in DB → return null instead of crashing
            return null;
        }
    }
}