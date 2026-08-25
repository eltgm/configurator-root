package ru.sultanyarov.configurator.application.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateAttributeDefinitionRequest;
import ru.sultanyarov.configurator.domain.model.DataType;

@Mapper(componentModel = "spring")
public interface AttributeDefinitionMapper {
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "domainId", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  ru.sultanyarov.configurator.domain.model.AttributeDefinition toModel(
      Long componentTypeId, CreateAttributeDefinitionRequest createAttributeDefinitionRequest);

  @Mapping(target = "componentTypeId", ignore = true)
  @Mapping(target = "domainId", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  ru.sultanyarov.configurator.domain.model.AttributeDefinition toModel(
      CreateAttributeDefinitionRequest createAttributeDefinitionRequest);

  @Mapping(target = "componentTypeIds", ignore = true)
  AttributeDefinition toDto(ru.sultanyarov.configurator.domain.model.AttributeDefinition update);

  List<AttributeDefinition> toDtoList(
      List<ru.sultanyarov.configurator.domain.model.AttributeDefinition> byComponentTypeId);

  default DataType toDataType(CreateAttributeDefinitionRequest.DataTypeEnum dataType) {
    return dataType == null ? null : DataType.valueOf(dataType.name());
  }
}
