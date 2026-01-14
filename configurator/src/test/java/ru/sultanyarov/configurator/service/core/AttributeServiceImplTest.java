package ru.sultanyarov.configurator.service.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.domain.dto.CreateAttributeDefinitionRequest;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.repository.AttributeRepository;
import ru.sultanyarov.configurator.domain.repository.ComponentTypeRepository;
import ru.sultanyarov.configurator.service.mapper.AttributeMapper;
import ru.sultanyarov.configurator.test.data.AttributeDefinitionTestData;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttributeServiceImplTest {
    @Mock
    private AttributeRepository attributeRepository;

    @Mock
    private ComponentTypeRepository componentTypeRepository;

    @Mock
    private AttributeMapper attributeMapper;

    @InjectMocks
    private AttributeServiceImpl attributeService;

    @Test
    void create_shouldCreateAttributeDefinitionWhenValid() {
        // Arrange
        AttributeDefinition attributeDefinition = AttributeDefinitionTestData.attributeDefinition();

        when(componentTypeRepository.existsById(attributeDefinition.componentTypeId())).thenReturn(true);
        when(attributeRepository.hasByComponentTypeIdAndName(attributeDefinition.componentTypeId(), attributeDefinition.name())).thenReturn(false);
        when(attributeRepository.createAttributeDefinition(attributeDefinition)).thenReturn(Optional.of(attributeDefinition));

        // Act
        AttributeDefinition result = attributeService.create(attributeDefinition);

        // Assert
        assertThat(result).isEqualTo(attributeDefinition);
        verify(componentTypeRepository).existsById(attributeDefinition.componentTypeId());
        verify(attributeRepository).hasByComponentTypeIdAndName(attributeDefinition.componentTypeId(), attributeDefinition.name());
        verify(attributeRepository).createAttributeDefinition(attributeDefinition);
    }

    @Test
    void create_shouldCreateEnumAttributeDefinitionWhenValid() {
        // Arrange
        AttributeDefinition attributeDefinition = AttributeDefinitionTestData.enumAttributeDefinition();

        when(componentTypeRepository.existsById(attributeDefinition.componentTypeId())).thenReturn(true);
        when(attributeRepository.hasByComponentTypeIdAndName(attributeDefinition.componentTypeId(), attributeDefinition.name())).thenReturn(false);
        when(attributeRepository.createAttributeDefinition(attributeDefinition)).thenReturn(Optional.of(attributeDefinition));

        // Act
        AttributeDefinition result = attributeService.create(attributeDefinition);

        // Assert
        assertThat(result).isEqualTo(attributeDefinition);
        verify(componentTypeRepository).existsById(attributeDefinition.componentTypeId());
        verify(attributeRepository).hasByComponentTypeIdAndName(attributeDefinition.componentTypeId(), attributeDefinition.name());
        verify(attributeRepository).createAttributeDefinition(attributeDefinition);
    }

    @Test
    void create_shouldThrowNotFoundExceptionWhenComponentTypeDoesNotExist() {
        // Arrange
        AttributeDefinition attributeDefinition = AttributeDefinitionTestData.attributeDefinition();

        when(componentTypeRepository.existsById(attributeDefinition.componentTypeId())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> attributeService.create(attributeDefinition))
                .isInstanceOf(NotFoundException.class);

        verify(componentTypeRepository).existsById(attributeDefinition.componentTypeId());
        verify(attributeRepository, never()).hasByComponentTypeIdAndName(anyLong(), anyString());
        verify(attributeRepository, never()).createAttributeDefinition(any());
    }

    @Test
    void create_shouldThrowEntityAlreadyExistsExceptionWhenAttributeDefinitionWithSameNameAndComponentTypeExists() {
        // Arrange
        AttributeDefinition attributeDefinition = AttributeDefinitionTestData.attributeDefinition();

        when(componentTypeRepository.existsById(attributeDefinition.componentTypeId())).thenReturn(true);
        when(attributeRepository.hasByComponentTypeIdAndName(attributeDefinition.componentTypeId(), attributeDefinition.name())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> attributeService.create(attributeDefinition))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(componentTypeRepository).existsById(attributeDefinition.componentTypeId());
        verify(attributeRepository).hasByComponentTypeIdAndName(attributeDefinition.componentTypeId(), attributeDefinition.name());
        verify(attributeRepository, never()).createAttributeDefinition(any());
    }

    @Test
    void create_shouldThrowValidationExceptionWhenEnumAttributeDefinitionHasNoEnumValues() {
        // Arrange
        AttributeDefinition attributeDefinition = AttributeDefinitionTestData.enumAttributeDefinitionWithoutValues();

        when(componentTypeRepository.existsById(attributeDefinition.componentTypeId())).thenReturn(true);
        when(attributeRepository.hasByComponentTypeIdAndName(attributeDefinition.componentTypeId(), attributeDefinition.name())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> attributeService.create(attributeDefinition))
                .isInstanceOf(ValidationException.class);

        verify(componentTypeRepository).existsById(attributeDefinition.componentTypeId());
        verify(attributeRepository).hasByComponentTypeIdAndName(attributeDefinition.componentTypeId(), attributeDefinition.name());
        verify(attributeRepository, never()).createAttributeDefinition(any());
    }

    @Test
    void update_shouldUpdateAttributeDefinitionWhenValid() {
        // Arrange
        Long id = 1L;
        AttributeDefinition existingAttributeDefinition = AttributeDefinitionTestData.attributeDefinitionWithId(id);
        AttributeDefinition updatedAttributeDefinition = AttributeDefinitionTestData.attributeDefinitionWithIdAndName(id, "updated_attribute");
        CreateAttributeDefinitionRequest createAttributeDefinitionRequest = new CreateAttributeDefinitionRequest();

        when(attributeRepository.getById(id)).thenReturn(Optional.of(existingAttributeDefinition));
        when(attributeMapper.updateModel(existingAttributeDefinition, createAttributeDefinitionRequest)).thenReturn(updatedAttributeDefinition);
        when(attributeRepository.hasByComponentTypeIdAndName(updatedAttributeDefinition.componentTypeId(), updatedAttributeDefinition.name())).thenReturn(false);
        when(attributeRepository.updateAttribute(id, updatedAttributeDefinition)).thenReturn(Optional.of(updatedAttributeDefinition));

        // Act
        AttributeDefinition result = attributeService.update(id, createAttributeDefinitionRequest);

        // Assert
        assertThat(result).isEqualTo(updatedAttributeDefinition);
        verify(attributeRepository).hasByComponentTypeIdAndName(updatedAttributeDefinition.componentTypeId(), updatedAttributeDefinition.name());
        verify(attributeRepository).updateAttribute(id, updatedAttributeDefinition);
    }

    @Test
    void update_shouldThrowNotFoundExceptionWhenAttributeDefinitionDoesNotExist() {
        // Arrange
        Long id = 1L;
        AttributeDefinition updatedAttributeDefinition = AttributeDefinitionTestData.attributeDefinitionWithId(id);

        when(attributeRepository.getById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> attributeService.update(id, new CreateAttributeDefinitionRequest()))
                .isInstanceOf(NotFoundException.class);

        verify(attributeRepository, never()).hasByComponentTypeIdAndName(anyLong(), anyString());
        verify(attributeRepository, never()).updateAttribute(anyLong(), any());
    }

    @Test
    void update_shouldThrowEntityAlreadyExistsExceptionWhenAnotherAttributeDefinitionWithSameNameAndComponentTypeExists() {
        // Arrange
        Long id = 1L;
        AttributeDefinition existingAttributeDefinition = AttributeDefinitionTestData.attributeDefinitionWithId(id);
        AttributeDefinition updatedAttributeDefinition = AttributeDefinitionTestData.attributeDefinitionWithIdAndName(id, "updated_attribute");
        CreateAttributeDefinitionRequest createAttributeDefinitionRequest = new CreateAttributeDefinitionRequest();

        when(attributeRepository.getById(id)).thenReturn(Optional.of(existingAttributeDefinition));
        when(attributeMapper.updateModel(existingAttributeDefinition, createAttributeDefinitionRequest)).thenReturn(updatedAttributeDefinition);
        when(attributeRepository.hasByComponentTypeIdAndName(updatedAttributeDefinition.componentTypeId(), updatedAttributeDefinition.name())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> attributeService.update(id, createAttributeDefinitionRequest))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(attributeRepository).hasByComponentTypeIdAndName(updatedAttributeDefinition.componentTypeId(), updatedAttributeDefinition.name());
        verify(attributeRepository, never()).updateAttribute(anyLong(), any());
    }

    @Test
    void deleteById_shouldDeleteAttributeDefinitionWhenValid() {
        // Arrange
        Long id = 1L;

        when(attributeRepository.existsById(id)).thenReturn(true);

        // Act
        attributeService.deleteById(id);

        // Assert
        verify(attributeRepository).existsById(id);
        verify(attributeRepository).deleteById(id);
    }

    @Test
    void deleteById_shouldThrowNotFoundExceptionWhenAttributeDefinitionDoesNotExist() {
        // Arrange
        Long id = 1L;

        when(attributeRepository.existsById(id)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> attributeService.deleteById(id))
                .isInstanceOf(NotFoundException.class);

        verify(attributeRepository).existsById(id);
        verify(attributeRepository, never()).deleteById(anyLong());
    }

    @Test
    void getByComponentTypeId_shouldReturnAttributeDefinitionsWhenComponentTypeExists() {
        // Arrange
        Long componentTypeId = 1L;
        List<AttributeDefinition> expectedAttributeDefinitions = List.of(
                AttributeDefinitionTestData.attributeDefinitionWithId(1L),
                AttributeDefinitionTestData.attributeDefinitionWithId(2L)
        );

        when(componentTypeRepository.existsById(componentTypeId)).thenReturn(true);
        when(attributeRepository.getByComponentTypeId(componentTypeId)).thenReturn(expectedAttributeDefinitions);

        // Act
        List<AttributeDefinition> result = attributeService.getByComponentTypeId(componentTypeId);

        // Assert
        assertThat(result).isEqualTo(expectedAttributeDefinitions);
        verify(componentTypeRepository).existsById(componentTypeId);
        verify(attributeRepository).getByComponentTypeId(componentTypeId);
    }

    @Test
    void getByComponentTypeId_shouldThrowNotFoundExceptionWhenComponentTypeDoesNotExist() {
        // Arrange
        Long componentTypeId = 1L;

        when(componentTypeRepository.existsById(componentTypeId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> attributeService.getByComponentTypeId(componentTypeId))
                .isInstanceOf(NotFoundException.class);

        verify(componentTypeRepository).existsById(componentTypeId);
        verify(attributeRepository, never()).getByComponentTypeId(anyLong());
    }
}
