package ru.sultanyarov.configurator.domain.repository;

import ru.sultanyarov.configurator.domain.model.AttributeValue;

import java.util.List;

public interface AttributeValueRepository {

    List<AttributeValue> createAttributeValues(List<AttributeValue> newComponentAttributes, Long componentId);
}
