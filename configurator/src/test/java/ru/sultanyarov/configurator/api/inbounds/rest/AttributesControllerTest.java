package ru.sultanyarov.configurator.api.inbounds.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import ru.sultanyarov.configurator.api.inbounds.rest.controller.AttributesController;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateAttributeDefinitionRequest;
import ru.sultanyarov.configurator.application.facade.AttributesFacade;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttributesControllerTest {

    @Mock
    private AttributesFacade attributesFacade;

    @InjectMocks
    private AttributesController attributesController;

    @Test
    void attributesIdPut_shouldUpdateAttributeAndReturnOk() {
        // Arrange
        Long id = 1L;
        CreateAttributeDefinitionRequest request = new CreateAttributeDefinitionRequest();
        AttributeDefinition updatedAttribute = new AttributeDefinition();

        when(attributesFacade.updateAttribute(id, request)).thenReturn(updatedAttribute);

        // Act
        ResponseEntity<AttributeDefinition> response = attributesController.attributesIdPut(id, request);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(updatedAttribute);
        verify(attributesFacade).updateAttribute(id, request);
    }

    @Test
    void componentTypesIdAttributesGet_shouldGetAttributesAndReturnOk() {
        // Arrange
        Long componentTypeId = 1L;
        List<AttributeDefinition> attributes = List.of(
                new AttributeDefinition(),
                new AttributeDefinition()
        );

        when(attributesFacade.getAttributesByComponentTypeId(componentTypeId)).thenReturn(attributes);

        // Act
        ResponseEntity<List<AttributeDefinition>> response = attributesController.componentTypesIdAttributesGet(componentTypeId);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(attributes);
        verify(attributesFacade).getAttributesByComponentTypeId(componentTypeId);
    }

    @Test
    void componentTypesIdAttributesPost_shouldCreateAttributeAndReturnOk() {
        // Arrange
        Long componentTypeId = 1L;
        CreateAttributeDefinitionRequest request = new CreateAttributeDefinitionRequest();
        AttributeDefinition createdAttribute = new AttributeDefinition();

        when(attributesFacade.createAttribute(componentTypeId, request)).thenReturn(createdAttribute);

        // Act
        ResponseEntity<AttributeDefinition> response = attributesController.componentTypesIdAttributesPost(componentTypeId, request);

        // Assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(createdAttribute);
        verify(attributesFacade).createAttribute(componentTypeId, request);
    }
}
