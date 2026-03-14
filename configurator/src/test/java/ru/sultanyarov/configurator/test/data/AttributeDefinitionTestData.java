package ru.sultanyarov.configurator.test.data;

import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.DataType;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class AttributeDefinitionTestData {
    private static final AtomicLong idGenerator = new AtomicLong(1);

    public static AttributeDefinition attributeDefinition() {
        return new AttributeDefinition(
                idGenerator.getAndIncrement(),
                1L,
                "test_attribute",
                "Test Attribute",
                DataType.STRING,
                null,
                true,
                1,
                LocalDateTime.now()
        );
    }

    public static AttributeDefinition attributeDefinitionWithName(String name) {
        return new AttributeDefinition(
                idGenerator.getAndIncrement(),
                1L,
                name,
                "Test Attribute",
                DataType.STRING,
                null,
                true,
                1,
                LocalDateTime.now()
        );
    }

    public static AttributeDefinition attributeDefinitionWithId(Long id) {
        return new AttributeDefinition(
                id,
                1L,
                "test_attribute",
                "Test Attribute",
                DataType.STRING,
                null,
                true,
                1,
                LocalDateTime.now()
        );
    }

    public static AttributeDefinition attributeDefinitionWithIdAndName(Long id, String name) {
        return new AttributeDefinition(
                id,
                1L,
                name,
                "Test Attribute",
                DataType.STRING,
                null,
                true,
                1,
                LocalDateTime.now()
        );
    }

    public static AttributeDefinition attributeDefinitionWithComponentTypeId(Long componentTypeId) {
        return new AttributeDefinition(
                idGenerator.getAndIncrement(),
                componentTypeId,
                "test_attribute",
                "Test Attribute",
                DataType.STRING,
                null,
                true,
                1,
                LocalDateTime.now()
        );
    }

    public static AttributeDefinition enumAttributeDefinition() {
        return new AttributeDefinition(
                idGenerator.getAndIncrement(),
                1L,
                "enum_attribute",
                "Enum Attribute",
                DataType.ENUM,
                Set.of("VALUE1", "VALUE2", "VALUE3"),
                true,
                1,
                LocalDateTime.now()
        );
    }

    public static AttributeDefinition enumAttributeDefinitionWithoutValues() {
        return new AttributeDefinition(
                idGenerator.getAndIncrement(),
                1L,
                "enum_attribute",
                "Enum Attribute",
                DataType.ENUM,
                null,
                true,
                1,
                LocalDateTime.now()
        );
    }
}
