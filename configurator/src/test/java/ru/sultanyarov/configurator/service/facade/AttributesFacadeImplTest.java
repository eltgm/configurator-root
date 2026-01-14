package ru.sultanyarov.configurator.service.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.domain.dto.AttributeDefinition;
import ru.sultanyarov.configurator.domain.dto.CreateAttributeDefinitionRequest;
import ru.sultanyarov.configurator.service.core.AttributeService;
import ru.sultanyarov.configurator.service.mapper.AttributeMapper;
import ru.sultanyarov.configurator.test.data.AttributeDefinitionTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttributesFacadeImplTest {

    @Mock
    private AttributeService attributeService;

    @Mock
    private AttributeMapper attributeMapper;

    @InjectMocks
    private AttributesFacadeImpl attributesFacade;

    @Test
    void createAttribute_shouldCallServiceAndCreateAttribute() {
        // Arrange
        Long componentTypeId = 1L;
        CreateAttributeDefinitionRequest request = new CreateAttributeDefinitionRequest();
        request.setName("Attribute Name");
        ru.sultanyarov.configurator.domain.model.AttributeDefinition model = AttributeDefinitionTestData.attributeDefinitionWithComponentTypeId(componentTypeId);
        AttributeDefinition dto = new AttributeDefinition();

        when(attributeMapper.toModel(componentTypeId, request)).thenReturn(model);
        when(attributeService.create(model)).thenReturn(model);
        when(attributeMapper.toDto(model)).thenReturn(dto);

        // Act
        AttributeDefinition result = attributesFacade.createAttribute(componentTypeId, request);

        // Assert
        assertThat(result).isEqualTo(dto);
        verify(attributeMapper).toModel(componentTypeId, request);
        verify(attributeService).create(model);
        verify(attributeMapper).toDto(model);
    }

    @Test
    void getAttributesByComponentTypeId_shouldCallServiceAndGetAttributes() {
        // Arrange
        Long componentTypeId = 1L;
        List<ru.sultanyarov.configurator.domain.model.AttributeDefinition> models = List.of(
                AttributeDefinitionTestData.attributeDefinitionWithId(1L),
                AttributeDefinitionTestData.attributeDefinitionWithId(2L)
        );
        List<AttributeDefinition> dtos = List.of(
                new AttributeDefinition(),
                new AttributeDefinition()
        );

        when(attributeService.getByComponentTypeId(componentTypeId)).thenReturn(models);
        when(attributeMapper.toDtoList(models)).thenReturn(dtos);

        // Act
        List<AttributeDefinition> result = attributesFacade.getAttributesByComponentTypeId(componentTypeId);

        // Assert
        assertThat(result).isEqualTo(dtos);
        verify(attributeService).getByComponentTypeId(componentTypeId);
        verify(attributeMapper).toDtoList(models);
    }
}
