package ru.sultanyarov.configurator.service.core;


import ru.sultanyarov.configurator.domain.model.AttributeValue;

import java.util.List;

public interface AttributeValueService {
    List<AttributeValue> createAttributeValues(List<AttributeValue> attributeValues, Long componentId);
}
