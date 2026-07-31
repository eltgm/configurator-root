package ru.sultanyarov.configurator.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.application.port.out.AttributeValueRepository;
import ru.sultanyarov.configurator.domain.model.AttributeValue;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeValueServiceImpl implements AttributeValueService {
    private final AttributeValueRepository attributeValueRepository;

    @Override
    public List<AttributeValue> createAttributeValues(List<AttributeValue> attributeValues, Long componentId) {
        log.debug("createAttributeValues for component {}", componentId);
        return attributeValueRepository.createAttributeValues(
                attributeValues, componentId
        );
    }

    @Override
    public List<AttributeValue> replaceAttributeValues(List<AttributeValue> attributeValues, Long componentId) {
        log.debug("replaceAttributeValues for component {}", componentId);
        attributeValueRepository.deleteByComponentId(componentId);
        return attributeValueRepository.createAttributeValues(attributeValues, componentId);
    }
}
