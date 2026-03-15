package ru.sultanyarov.configurator.service.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.facade.ComponentTypeFacadeImpl;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentType;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentTypeRequest;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.EntityHasRelatedEntitiesException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.application.service.ComponentTypeService;
import ru.sultanyarov.configurator.application.mapper.ComponentTypeMapper;
import ru.sultanyarov.configurator.test.data.ComponentTypeTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentTypeFacadeImplTest {
    @Mock
    private ComponentTypeService componentTypeService;

    @Mock
    private ComponentTypeMapper componentTypeMapper;

    @InjectMocks
    private ComponentTypeFacadeImpl componentTypeFacade;

    @Test
    void createComponentType_shouldCreateComponentTypeWhenRequestIsValid() {
        // Arrange
        Long domainId = 1L;
        CreateComponentTypeRequest request = new CreateComponentTypeRequest();
        request.setName("Test Component Type");
        request.setCode("TEST_CODE");
        request.setDescription("Test Description");
        request.setOrderIndex(1);

        ru.sultanyarov.configurator.domain.model.ComponentType componentType = ComponentTypeTestData.componentType();
        ru.sultanyarov.configurator.domain.model.ComponentType createdComponentType = ComponentTypeTestData.componentTypeWithId(1L);
        ComponentType expectedDto = new ComponentType();
        expectedDto.setId(1L);
        expectedDto.setDomainId(domainId);
        expectedDto.setName("Test Component Type");
        expectedDto.setCode("TEST_CODE");
        expectedDto.setDescription("Test Description");
        expectedDto.setOrderIndex(1);

        when(componentTypeMapper.toEntityWithDomain(domainId, request)).thenReturn(componentType);
        when(componentTypeService.create(any(ru.sultanyarov.configurator.domain.model.ComponentType.class))).thenReturn(createdComponentType);
        when(componentTypeMapper.toDto(createdComponentType)).thenReturn(expectedDto);

        // Act
        ComponentType result = componentTypeFacade.createComponentType(domainId, request);

        // Assert
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedDto);
        verify(componentTypeMapper).toEntityWithDomain(domainId, request);
        verify(componentTypeService).create(any(ru.sultanyarov.configurator.domain.model.ComponentType.class));
        verify(componentTypeMapper).toDto(createdComponentType);
    }

    @Test
    void updateComponentType_shouldUpdateComponentTypeWhenRequestIsValid() {
        // Arrange
        Long componentTypeId = 1L;
        CreateComponentTypeRequest request = new CreateComponentTypeRequest();
        request.setName("Updated Component Type");
        request.setCode("UPDATED_CODE");
        request.setDescription("Updated Description");
        request.setOrderIndex(2);

        ru.sultanyarov.configurator.domain.model.ComponentType componentType = ComponentTypeTestData.componentTypeWithName("Updated Component Type");
        ru.sultanyarov.configurator.domain.model.ComponentType updatedComponentType = ComponentTypeTestData.componentTypeWithIdAndName(componentTypeId, "Updated Component Type");
        ComponentType expectedDto = new ComponentType();
        expectedDto.setId(componentTypeId);
        expectedDto.setDomainId(1L);
        expectedDto.setName("Updated Component Type");
        expectedDto.setCode("UPDATED_CODE");
        expectedDto.setDescription("Updated Description");
        expectedDto.setOrderIndex(2);

        when(componentTypeMapper.toEntity(componentTypeId, request)).thenReturn(componentType);
        when(componentTypeService.update(anyLong(), any(ru.sultanyarov.configurator.domain.model.ComponentType.class))).thenReturn(updatedComponentType);
        when(componentTypeMapper.toDto(updatedComponentType)).thenReturn(expectedDto);

        // Act
        ComponentType result = componentTypeFacade.updateComponentType(componentTypeId, request);

        // Assert
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedDto);
        verify(componentTypeMapper).toEntity(componentTypeId, request);
        verify(componentTypeService).update(anyLong(), any(ru.sultanyarov.configurator.domain.model.ComponentType.class));
        verify(componentTypeMapper).toDto(updatedComponentType);
    }

    @Test
    void deleteComponentType_shouldDeleteComponentTypeWhenItExists() {
        // Arrange
        Long id = 1L;

        // Act
        componentTypeFacade.deleteComponentType(id);

        // Assert
        verify(componentTypeService).deleteById(id);
    }

    @Test
    void deleteComponentType_shouldThrowNotFoundExceptionWhenComponentTypeDoesNotExist() {
        // Arrange
        Long id = 1L;

        doThrow(new NotFoundException("ComponentType with id {} does not exist", id))
                .when(componentTypeService).deleteById(id);

        // Act & Assert
        assertThatThrownBy(() -> componentTypeFacade.deleteComponentType(id))
                .isInstanceOf(NotFoundException.class);

        verify(componentTypeService).deleteById(id);
    }

    @Test
    void deleteComponentType_shouldThrowEntityHasRelatedEntitiesExceptionWhenComponentTypeHasRelatedEntities() {
        // Arrange
        Long id = 1L;

        doThrow(new EntityHasRelatedEntitiesException("Cannot delete component type with id {} because it has related entities", id))
                .when(componentTypeService).deleteById(id);

        // Act & Assert
        assertThatThrownBy(() -> componentTypeFacade.deleteComponentType(id))
                .isInstanceOf(EntityHasRelatedEntitiesException.class);

        verify(componentTypeService).deleteById(id);
    }

    @Test
    void getComponentType_shouldReturnComponentTypeWhenItExists() {
        // Arrange
        Long id = 1L;
        ru.sultanyarov.configurator.domain.model.ComponentType componentType = ComponentTypeTestData.componentTypeWithId(id);
        ComponentType expectedDto = new ComponentType();
        expectedDto.setId(id);
        expectedDto.setDomainId(1L);
        expectedDto.setName("Test Component Type");
        expectedDto.setCode("TEST_CODE");
        expectedDto.setDescription("Test Description");
        expectedDto.setOrderIndex(1);

        when(componentTypeService.getById(id)).thenReturn(componentType);
        when(componentTypeMapper.toDto(componentType)).thenReturn(expectedDto);

        // Act
        ComponentType result = componentTypeFacade.getComponentType(id);

        // Assert
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedDto);
        verify(componentTypeService).getById(id);
        verify(componentTypeMapper).toDto(componentType);
    }

    @Test
    void getComponentType_shouldThrowNotFoundExceptionWhenComponentTypeDoesNotExist() {
        // Arrange
        Long id = 1L;

        when(componentTypeService.getById(id)).thenThrow(new NotFoundException("ComponentType with id {} does not exist", id));

        // Act & Assert
        assertThatThrownBy(() -> componentTypeFacade.getComponentType(id))
                .isInstanceOf(NotFoundException.class);

        verify(componentTypeService).getById(id);
    }

    @Test
    void getComponentTypesByDomainId_shouldReturnComponentTypesWhenDomainExists() {
        // Arrange
        Long domainId = 1L;
        List<ru.sultanyarov.configurator.domain.model.ComponentType> componentTypes = List.of(
                ComponentTypeTestData.componentTypeWithId(1L),
                ComponentTypeTestData.componentTypeWithId(2L)
        );

        ComponentType componentTypeDto1 = new ComponentType();
        componentTypeDto1.setId(1L);
        componentTypeDto1.setDomainId(domainId);
        componentTypeDto1.setName("Test Component Type 1");
        componentTypeDto1.setCode("TEST_CODE_1");
        componentTypeDto1.setDescription("Test Description 1");
        componentTypeDto1.setOrderIndex(1);

        ComponentType componentTypeDto2 = new ComponentType();
        componentTypeDto2.setId(2L);
        componentTypeDto2.setDomainId(domainId);
        componentTypeDto2.setName("Test Component Type 2");
        componentTypeDto2.setCode("TEST_CODE_2");
        componentTypeDto2.setDescription("Test Description 2");
        componentTypeDto2.setOrderIndex(2);

        List<ComponentType> expectedDtos = List.of(componentTypeDto1, componentTypeDto2);

        when(componentTypeService.getByDomainId(domainId)).thenReturn(componentTypes);
        when(componentTypeMapper.toDtoList(componentTypes)).thenReturn(expectedDtos);

        // Act
        List<ComponentType> result = componentTypeFacade.getComponentTypesByDomainId(domainId);

        // Assert
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedDtos);
        verify(componentTypeService).getByDomainId(domainId);
        verify(componentTypeMapper).toDtoList(componentTypes);
    }

    @Test
    void getComponentTypesByDomainId_shouldThrowBusinessExceptionWhenDomainDoesNotExist() {
        // Arrange
        Long domainId = 1L;

        when(componentTypeService.getByDomainId(domainId)).thenThrow(new BusinessException("Domain with id {} does not exist", domainId));

        // Act & Assert
        assertThatThrownBy(() -> componentTypeFacade.getComponentTypesByDomainId(domainId))
                .isInstanceOf(BusinessException.class);

        verify(componentTypeService).getByDomainId(domainId);
    }
}
