package com.emp_management.shared.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.math.BigDecimal;

@Converter
public class AESBigDecimalConverter implements AttributeConverter<BigDecimal, String> {

    @Override
    public String convertToDatabaseColumn(BigDecimal attribute) {
        if (attribute == null) return null;
        return AESUtil.encrypt(attribute.toString());
    }

    @Override
    public BigDecimal convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return new BigDecimal(AESUtil.decrypt(dbData));
    }
}