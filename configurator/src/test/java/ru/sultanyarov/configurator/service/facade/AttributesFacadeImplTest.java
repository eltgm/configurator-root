package ru.sultanyarov.configurator.service.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentTypeAttributeSettingsRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateAttributeDefinitionRequest;
import ru.sultanyarov.configurator.application.facade.AttributesFacadeImpl;
import ru.sultanyarov.configurator.application.mapper.AttributeDefinitionMapper;
import ru.sultanyarov.configurator.application.service.AttributeService;
import ru.sultanyarov.configurator.test.data.AttributeDefinitionTestData;

@ExtendWith(MockitoExtension.class)
class AttributesFacadeImplTest {

  @Mock private AttributeService attributeService;

  @Mock private AttributeDefinitionMapper attributeDefinitionMapper;

  @InjectMocks private AttributesFacadeImpl attributesFacade;

  @Test
  void createAttribute_shouldCallServiceAndCreateAttribute() {
    // Arrange
    Long componentTypeId = 1L;
    CreateAttributeDefinitionRequest request = new CreateAttributeDefinitionRequest();
    request.setName("Attribute Name");
    ru.sultanyarov.configurator.domain.model.AttributeDefinition model =
        AttributeDefinitionTestData.attributeDefinitionWithComponentTypeId(componentTypeId);
    AttributeDefinition dto = new AttributeDefinition();

    when(attributeDefinitionMapper.toModel(componentTypeId, request)).thenReturn(model);
    when(attributeService.create(model)).thenReturn(model);
    when(attributeDefinitionMapper.toDto(model)).thenReturn(dto);

    // Act
    AttributeDefinition result = attributesFacade.createAttribute(componentTypeId, request);

    // Assert
    assertThat(result).isEqualTo(dto);
    verify(attributeDefinitionMapper).toModel(componentTypeId, request);
    verify(attributeService).create(model);
    verify(attributeDefinitionMapper).toDto(model);
  }

  @Test
  void getAttributesByComponentTypeId_shouldCallServiceAndGetAttributes() {
    // Arrange
    Long componentTypeId = 1L;
    List<ru.sultanyarov.configurator.domain.model.AttributeDefinition> models =
        List.of(
            AttributeDefinitionTestData.attributeDefinitionWithId(1L),
            AttributeDefinitionTestData.attributeDefinitionWithId(2L));
    List<AttributeDefinition> dtos = List.of(new AttributeDefinition(), new AttributeDefinition());

    when(attributeService.getByComponentTypeId(componentTypeId)).thenReturn(models);
    when(attributeDefinitionMapper.toDtoList(models)).thenReturn(dtos);

    // Act
    List<AttributeDefinition> result =
        attributesFacade.getAttributesByComponentTypeId(componentTypeId);

    // Assert
    assertThat(result).isEqualTo(dtos);
    verify(attributeService).getByComponentTypeId(componentTypeId);
    verify(attributeDefinitionMapper).toDtoList(models);
  }

  @Test
  void catalogAndLinkOperations_shouldMapAndDelegate() {
    Long domainId = 1L;
    Long componentTypeId = 10L;
    Long attributeId = 101L;
    CreateAttributeDefinitionRequest request = new CreateAttributeDefinitionRequest();
    request.setName("socket");
    ComponentTypeAttributeSettingsRequest settings =
        new ComponentTypeAttributeSettingsRequest().isRequired(true).orderIndex(2);
    ru.sultanyarov.configurator.domain.model.AttributeDefinition model =
        AttributeDefinitionTestData.attributeDefinitionWithId(attributeId);
    AttributeDefinition dto = new AttributeDefinition();

    when(attributeDefinitionMapper.toModel(request)).thenReturn(model);
    when(attributeService.createInDomain(domainId, model)).thenReturn(model);
    when(attributeService.attachToComponentType(componentTypeId, attributeId, true, 2))
        .thenReturn(model);
    when(attributeService.getByDomainId(domainId)).thenReturn(List.of(model));
    when(attributeService.getComponentTypeIds(attributeId)).thenReturn(List.of(componentTypeId));
    when(attributeDefinitionMapper.toDto(model)).thenReturn(dto);

    assertThat(attributesFacade.createCatalogAttribute(domainId, request)).isEqualTo(dto);
    assertThat(attributesFacade.attachAttribute(componentTypeId, attributeId, settings))
        .isEqualTo(dto);
    assertThat(attributesFacade.getAttributesByDomainId(domainId)).containsExactly(dto);
    assertThat(dto.getComponentTypeIds()).containsExactly(componentTypeId);

    attributesFacade.detachAttribute(componentTypeId, attributeId);
    attributesFacade.deleteAttribute(attributeId);
    verify(attributeService).detachFromComponentType(componentTypeId, attributeId);
    verify(attributeService).deleteById(attributeId);
  }
}
