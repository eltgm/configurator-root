package ru.sultanyarov.configurator.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sultanyarov.configurator.domain.dto.AttributeDefinition;
import ru.sultanyarov.configurator.domain.dto.CreateAttributeDefinitionRequest;
import ru.sultanyarov.configurator.domain.model.DataType;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AttributeDefinitionMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ru.sultanyarov.configurator.domain.model.AttributeDefinition toModel(Long componentTypeId, CreateAttributeDefinitionRequest createAttributeDefinitionRequest);

    @Mapping(target = "componentTypeId", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ru.sultanyarov.configurator.domain.model.AttributeDefinition toModel(CreateAttributeDefinitionRequest createAttributeDefinitionRequest);

    AttributeDefinition toDto(ru.sultanyarov.configurator.domain.model.AttributeDefinition update);

    List<AttributeDefinition> toDtoList(List<ru.sultanyarov.configurator.domain.model.AttributeDefinition> byComponentTypeId);


    default ru.sultanyarov.configurator.domain.model.AttributeDefinition updateModel(ru.sultanyarov.configurator.domain.model.AttributeDefinition attributeDefinition, CreateAttributeDefinitionRequest createAttributeDefinitionRequest) {
        return ru.sultanyarov.configurator.domain.model.AttributeDefinition.builder()
                .id(attributeDefinition.id())
                .componentTypeId(attributeDefinition.componentTypeId())
                .createdAt(attributeDefinition.createdAt())

                .name(createAttributeDefinitionRequest.getName())
                .label(createAttributeDefinitionRequest.getLabel())
                .dataType(DataType.of(createAttributeDefinitionRequest.getDataType()))
                .enumValues(createAttributeDefinitionRequest.getEnumValues())
                .isRequired(createAttributeDefinitionRequest.getIsRequired())
                .orderIndex(createAttributeDefinitionRequest.getOrderIndex())
                .build();
    }
}
