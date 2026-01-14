package ru.sultanyarov.configurator.application.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.jooq.Converter;
import org.jooq.exception.DataTypeException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StringToListConverter implements Converter<String, List<String>> {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CollectionType type = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class);

    @Override
    public List<String> from(String databaseString) {
        if (databaseString == null) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(databaseString, type);
        } catch (IOException e) {
            throw new DataTypeException("Error converting String to List<MyItem>", e);
        }
    }

    @Override
    public String to(List<String> pojoList) {
        if (pojoList == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(pojoList);
        } catch (JsonProcessingException e) {
            throw new DataTypeException("Error converting List<MyItem> to String", e);
        }
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @Override
    public Class<List<String>> toType() {
        return (Class<List<String>>) (Class<?>) List.class;
    }
}
