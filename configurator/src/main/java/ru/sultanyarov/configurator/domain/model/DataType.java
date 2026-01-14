package ru.sultanyarov.configurator.domain.model;

import ru.sultanyarov.configurator.domain.dto.CreateAttributeDefinitionRequest;

public enum DataType {
    STRING, NUMBER, BOOLEAN, ENUM;

    public static DataType of(CreateAttributeDefinitionRequest.DataTypeEnum dataTypeEnum) {
        return switch (dataTypeEnum) {
            case STRING -> STRING;
            case NUMBER -> NUMBER;
            case BOOLEAN -> BOOLEAN;
            case ENUM -> ENUM;
        };
    }
}
