package ru.sultanyarov.configurator.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.repository.AttributeValueRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttributeValueServiceImpl implements AttributeValueService {
    private final AttributeValueRepository attributeValueRepository;

    @Override
    public List<AttributeValue> createAttributeValues(List<AttributeValue> attributeValues, Long componentId) {
        return attributeValueRepository.createAttributeValues(
                attributeValues, componentId
        );
    }
}
