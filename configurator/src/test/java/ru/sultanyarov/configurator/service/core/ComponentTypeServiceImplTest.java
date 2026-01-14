package ru.sultanyarov.configurator.service.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.EntityHasRelatedEntitiesException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.repository.AttributeRepository;
import ru.sultanyarov.configurator.domain.repository.ComponentTypeRepository;
import ru.sultanyarov.configurator.domain.repository.DomainRepository;
import ru.sultanyarov.configurator.test.data.ComponentTypeTestData;
import ru.sultanyarov.configurator.test.data.DomainTestData;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentTypeServiceImplTest {
    @Mock
    private ComponentTypeRepository componentTypeRepository;

    @Mock
    private DomainRepository domainRepository;

    @Mock
    private AttributeRepository attributesRepository;

    @InjectMocks
    private ComponentTypeServiceImpl componentTypeService;

    @Test
    void create_shouldCreateComponentTypeWhenValid() {
        // Arrange
        ComponentType componentType = ComponentTypeTestData.componentType();
        Domain domain = DomainTestData.domainWithId(componentType.domainId());

        when(domainRepository.getDomainById(componentType.domainId())).thenReturn(Optional.of(domain));
        when(componentTypeRepository.createComponentType(componentType)).thenReturn(Optional.of(componentType));

        // Act
        ComponentType result = componentTypeService.create(componentType);

        // Assert
        assertThat(result).isEqualTo(componentType);
        verify(domainRepository).getDomainById(componentType.domainId());
        verify(componentTypeRepository).createComponentType(componentType);
    }

    @Test
    void create_shouldThrowBusinessExceptionWhenDomainDoesNotExist() {
        // Arrange
        ComponentType componentType = ComponentTypeTestData.componentType();

        when(domainRepository.getDomainById(componentType.domainId())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> componentTypeService.create(componentType))
                .isInstanceOf(BusinessException.class);

        verify(domainRepository).getDomainById(componentType.domainId());
        verify(componentTypeRepository, never()).createComponentType(any());
    }

    @Test
    void create_shouldThrowEntityAlreadyExistsExceptionWhenComponentTypeWithSameNameExistsInDomain() {
        // Arrange
        ComponentType componentType = ComponentTypeTestData.componentType();
        ComponentType existingComponentType = ComponentTypeTestData.componentTypeWithIdAndName(2L, componentType.name());
        Domain domain = new Domain(
                componentType.domainId(),
                "Test Domain",
                "Test Description",
                1L,
                List.of(existingComponentType),
                java.time.LocalDateTime.now()
        );

        when(domainRepository.getDomainById(componentType.domainId())).thenReturn(Optional.of(domain));

        // Act & Assert
        assertThatThrownBy(() -> componentTypeService.create(componentType))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(domainRepository).getDomainById(componentType.domainId());
        verify(componentTypeRepository, never()).createComponentType(any());
    }

    @Test
    void update_shouldUpdateComponentTypeWhenValid() {
        // Arrange
        Long id = 1L;
        ComponentType existingComponentType = ComponentTypeTestData.componentTypeWithId(id);
        ComponentType updatedComponentType = ComponentTypeTestData.componentTypeWithIdAndName(id, "Updated Name");

        when(componentTypeRepository.getComponentTypeById(id)).thenReturn(Optional.of(existingComponentType));
        when(componentTypeRepository.updateComponentType(id, updatedComponentType)).thenReturn(Optional.of(updatedComponentType));
        when(domainRepository.getDomainById(updatedComponentType.domainId())).thenReturn(Optional.of(DomainTestData.domainWithId(updatedComponentType.domainId())));

        // Act
        ComponentType result = componentTypeService.update(id, updatedComponentType);

        // Assert
        assertThat(result).isEqualTo(updatedComponentType);
        verify(componentTypeRepository).getComponentTypeById(id);
        verify(componentTypeRepository).updateComponentType(id, updatedComponentType);
    }

    @Test
    void update_shouldThrowNotFoundExceptionWhenComponentTypeDoesNotExist() {
        // Arrange
        Long id = 1L;
        ComponentType updatedComponentType = ComponentTypeTestData.componentTypeWithId(id);

        when(componentTypeRepository.getComponentTypeById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> componentTypeService.update(id, updatedComponentType))
                .isInstanceOf(NotFoundException.class);

        verify(componentTypeRepository).getComponentTypeById(id);
        verify(componentTypeRepository, never()).updateComponentType(anyLong(), any());
    }

    @Test
    void deleteById_shouldDeleteComponentTypeWhenValid() {
        // Arrange
        Long id = 1L;

        when(componentTypeRepository.existsById(id)).thenReturn(true);
        when(attributesRepository.hasByComponentTypeId(id)).thenReturn(false);

        // Act
        componentTypeService.deleteById(id);

        // Assert
        verify(componentTypeRepository).existsById(id);
        verify(attributesRepository).hasByComponentTypeId(id);
        verify(componentTypeRepository).deleteComponentTypeById(id);
    }

    @Test
    void deleteById_shouldThrowNotFoundExceptionWhenComponentTypeDoesNotExist() {
        // Arrange
        Long id = 1L;

        when(componentTypeRepository.existsById(id)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> componentTypeService.deleteById(id))
                .isInstanceOf(NotFoundException.class);

        verify(componentTypeRepository).existsById(id);
        verify(attributesRepository, never()).hasByComponentTypeId(anyLong());
        verify(componentTypeRepository, never()).deleteComponentTypeById(anyLong());
    }

    @Test
    void deleteById_shouldThrowEntityHasRelatedEntitiesExceptionWhenComponentTypeHasRelatedAttributes() {
        // Arrange
        Long id = 1L;

        when(componentTypeRepository.existsById(id)).thenReturn(true);
        when(attributesRepository.hasByComponentTypeId(id)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> componentTypeService.deleteById(id))
                .isInstanceOf(EntityHasRelatedEntitiesException.class);

        verify(componentTypeRepository).existsById(id);
        verify(attributesRepository).hasByComponentTypeId(id);
        verify(componentTypeRepository, never()).deleteComponentTypeById(anyLong());
    }

    @Test
    void getById_shouldReturnComponentTypeWhenItExists() {
        // Arrange
        Long id = 1L;
        ComponentType expectedComponentType = ComponentTypeTestData.componentTypeWithId(id);

        when(componentTypeRepository.getComponentTypeById(id)).thenReturn(Optional.of(expectedComponentType));

        // Act
        ComponentType result = componentTypeService.getById(id);

        // Assert
        assertThat(result).isEqualTo(expectedComponentType);
        verify(componentTypeRepository).getComponentTypeById(id);
    }

    @Test
    void getById_shouldThrowNotFoundExceptionWhenComponentTypeDoesNotExist() {
        // Arrange
        Long id = 1L;

        when(componentTypeRepository.getComponentTypeById(id)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> componentTypeService.getById(id))
                .isInstanceOf(NotFoundException.class);

        verify(componentTypeRepository).getComponentTypeById(id);
    }

    @Test
    void getByDomainId_shouldReturnComponentTypesWhenDomainExists() {
        // Arrange
        Long domainId = 1L;
        List<ComponentType> expectedComponentTypes = List.of(
                ComponentTypeTestData.componentTypeWithId(1L),
                ComponentTypeTestData.componentTypeWithId(2L)
        );

        when(domainRepository.existsById(domainId)).thenReturn(true);
        when(componentTypeRepository.getComponentTypesByDomainId(domainId)).thenReturn(expectedComponentTypes);

        // Act
        List<ComponentType> result = componentTypeService.getByDomainId(domainId);

        // Assert
        assertThat(result).isEqualTo(expectedComponentTypes);
        verify(domainRepository).existsById(domainId);
        verify(componentTypeRepository).getComponentTypesByDomainId(domainId);
    }

    @Test
    void getByDomainId_shouldThrowBusinessExceptionWhenDomainDoesNotExist() {
        // Arrange
        Long domainId = 1L;

        when(domainRepository.existsById(domainId)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> componentTypeService.getByDomainId(domainId))
                .isInstanceOf(BusinessException.class);

        verify(domainRepository).existsById(domainId);
        verify(componentTypeRepository, never()).getComponentTypesByDomainId(anyLong());
    }
}
