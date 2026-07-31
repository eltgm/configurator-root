package ru.sultanyarov.configurator.service.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateAttributeDefinitionRequest;
import ru.sultanyarov.configurator.application.mapper.AttributeDefinitionMapper;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.DataType;
import ru.sultanyarov.configurator.test.data.AttributeDefinitionTestData;

class AttributeDefinitionMapperTest {

  private final AttributeDefinitionMapper attributeDefinitionMapper =
      Mappers.getMapper(AttributeDefinitionMapper.class);

  @Test
  void toModel_withComponentTypeId_shouldMapCorrectly_whenRequestIsNotNull() {
    // Arrange
    Long componentTypeId = 1L;
    CreateAttributeDefinitionRequest request =
        createCreateAttributeDefinitionRequest(
            "test_attribute", "Test Attribute", DataType.STRING, null, true, 1);

    // Act
    AttributeDefinition result = attributeDefinitionMapper.toModel(componentTypeId, request);

    // Assert
    assertThat(result)
        .isNotNull()
        .satisfies(
            ad -> {
              assertThat(ad.id()).isNull();
              assertThat(ad.componentTypeId()).isEqualTo(componentTypeId);
              assertThat(ad.name()).isEqualTo(request.getName());
              assertThat(ad.label()).isEqualTo(request.getLabel());
              assertThat(ad.dataType().name()).isEqualTo(request.getDataType().name());
              assertThat(ad.isRequired()).isEqualTo(request.getIsRequired());
              assertThat(ad.orderIndex()).isEqualTo(request.getOrderIndex());
            });
  }

  @Test
  void toModel_withoutComponentTypeId_shouldMapCorrectly_whenRequestIsNotNull() {
    // Arrange
    CreateAttributeDefinitionRequest request =
        createCreateAttributeDefinitionRequest(
            "test_attribute", "Test Attribute", DataType.STRING, null, true, 1);

    // Act
    AttributeDefinition result = attributeDefinitionMapper.toModel(request);

    // Assert
    assertThat(result)
        .isNotNull()
        .satisfies(
            ad -> {
              assertThat(ad.id()).isNull();
              assertThat(ad.componentTypeId()).isNull();
              assertThat(ad.name()).isEqualTo(request.getName());
              assertThat(ad.label()).isEqualTo(request.getLabel());
              assertThat(ad.dataType().name()).isEqualTo(request.getDataType().name());
              assertThat(ad.isRequired()).isEqualTo(request.getIsRequired());
              assertThat(ad.orderIndex()).isEqualTo(request.getOrderIndex());
            });
  }

  @Test
  void toDto_shouldMapCorrectly_whenAttributeDefinitionIsNotNull() {
    // Arrange
    AttributeDefinition attributeDefinition =
        AttributeDefinitionTestData.attributeDefinitionWithId(1L);

    // Act
    ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition result =
        attributeDefinitionMapper.toDto(attributeDefinition);

    // Assert
    assertThat(result)
        .isNotNull()
        .satisfies(
            ad -> {
              assertThat(ad.getId()).isEqualTo(attributeDefinition.id());
              assertThat(ad.getComponentTypeId()).isEqualTo(attributeDefinition.componentTypeId());
              assertThat(ad.getName()).isEqualTo(attributeDefinition.name());
              assertThat(ad.getLabel()).isEqualTo(attributeDefinition.label());
              assertThat(ad.getDataType().name()).isEqualTo(attributeDefinition.dataType().name());
              assertThat(ad.getIsRequired()).isEqualTo(attributeDefinition.isRequired());
              assertThat(ad.getOrderIndex()).isEqualTo(attributeDefinition.orderIndex());
            });
  }

  @Test
  void toDto_shouldReturnNull_whenAttributeDefinitionIsNull() {
    // Arrange
    AttributeDefinition attributeDefinition = null;

    // Act
    ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition result =
        attributeDefinitionMapper.toDto(attributeDefinition);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  void toDtoList_shouldMapCorrectly_whenAttributeDefinitionListIsNotEmpty() {
    // Arrange
    List<AttributeDefinition> attributeDefinitions =
        List.of(
            AttributeDefinitionTestData.attributeDefinitionWithId(1L),
            AttributeDefinitionTestData.attributeDefinitionWithId(2L));

    // Act
    List<ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition> result =
        attributeDefinitionMapper.toDtoList(attributeDefinitions);

    // Assert
    assertThat(result)
        .isNotNull()
        .hasSize(2)
        .satisfies(
            list -> {
              assertThat(list.get(0).getId()).isEqualTo(attributeDefinitions.get(0).id());
              assertThat(list.get(0).getComponentTypeId())
                  .isEqualTo(attributeDefinitions.get(0).componentTypeId());
              assertThat(list.get(0).getName()).isEqualTo(attributeDefinitions.get(0).name());
              assertThat(list.get(1).getId()).isEqualTo(attributeDefinitions.get(1).id());
              assertThat(list.get(1).getComponentTypeId())
                  .isEqualTo(attributeDefinitions.get(1).componentTypeId());
              assertThat(list.get(1).getName()).isEqualTo(attributeDefinitions.get(1).name());
            });
  }

  @Test
  void toDtoList_shouldReturnEmptyList_whenAttributeDefinitionListIsEmpty() {
    // Arrange
    List<AttributeDefinition> attributeDefinitions = List.of();

    // Act
    List<ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition> result =
        attributeDefinitionMapper.toDtoList(attributeDefinitions);

    // Assert
    assertThat(result).isNotNull().isEmpty();
  }

  @Test
  void toDtoList_shouldReturnNull_whenAttributeDefinitionListIsNull() {
    // Arrange
    List<AttributeDefinition> attributeDefinitions = null;

    // Act
    List<ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition> result =
        attributeDefinitionMapper.toDtoList(attributeDefinitions);

    // Assert
    assertThat(result).isNull();
  }

  @Test
  void toModel_shouldMapAllMutableFieldsWithoutTransportIdentityFields() {
    CreateAttributeDefinitionRequest request =
        createCreateAttributeDefinitionRequest(
            "updated_attribute", "Updated Attribute", DataType.ENUM, List.of("A", "B"), false, 9);

    AttributeDefinition result = attributeDefinitionMapper.toModel(request);

    assertThat(result.id()).isNull();
    assertThat(result.componentTypeId()).isNull();
    assertThat(result.createdAt()).isNull();
    assertThat(result.name()).isEqualTo("updated_attribute");
    assertThat(result.label()).isEqualTo("Updated Attribute");
    assertThat(result.dataType()).isEqualTo(DataType.ENUM);
    assertThat(result.enumValues()).containsExactlyInAnyOrder("A", "B");
    assertThat(result.isRequired()).isFalse();
    assertThat(result.orderIndex()).isEqualTo(9);
  }

  private CreateAttributeDefinitionRequest createCreateAttributeDefinitionRequest(
      String name,
      String label,
      DataType dataType,
      List<String> enumValues,
      Boolean isRequired,
      Integer orderIndex) {
    CreateAttributeDefinitionRequest request = new CreateAttributeDefinitionRequest();
    request.setName(name);
    request.setLabel(label);
    request.setDataType(CreateAttributeDefinitionRequest.DataTypeEnum.fromValue(dataType.name()));
    request.setEnumValues(enumValues);
    request.setIsRequired(isRequired);
    request.setOrderIndex(orderIndex);
    return request;
  }
}
