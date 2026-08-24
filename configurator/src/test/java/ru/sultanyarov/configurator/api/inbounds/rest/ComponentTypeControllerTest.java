package ru.sultanyarov.configurator.api.inbounds.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.sultanyarov.configurator.api.inbounds.rest.controller.ComponentTypeController;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentType;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentTypeRequest;
import ru.sultanyarov.configurator.application.facade.ComponentTypeFacade;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.EntityHasRelatedEntitiesException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class ComponentTypeControllerTest {
  @Mock private ComponentTypeFacade componentTypeFacade;

  @InjectMocks private ComponentTypeController componentTypeController;

  @Test
  void deleteComponentTypesById_shouldReturnOkWhenComponentTypeIsDeleted() {
    // Arrange
    Long id = 1L;

    // Act
    ResponseEntity<Void> response = componentTypeController.deleteComponentTypesById(id);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(componentTypeFacade).deleteComponentType(id);
  }

  @Test
  void deleteComponentTypesById_shouldThrowNotFoundExceptionWhenComponentTypeDoesNotExist() {
    // Arrange
    Long id = 1L;

    doThrow(new NotFoundException("ComponentType with id {} does not exist", id))
        .when(componentTypeFacade)
        .deleteComponentType(id);

    // Act & Assert
    assertThatThrownBy(() -> componentTypeController.deleteComponentTypesById(id))
        .isInstanceOf(NotFoundException.class);
    verify(componentTypeFacade).deleteComponentType(id);
  }

  @Test
  void
      deleteComponentTypesById_shouldThrowEntityHasRelatedEntitiesExceptionWhenComponentTypeHasRelatedEntities() {
    // Arrange
    Long id = 1L;

    doThrow(
            new EntityHasRelatedEntitiesException(
                "Cannot delete component type with id {} because it has related entities", id))
        .when(componentTypeFacade)
        .deleteComponentType(id);

    // Act & Assert
    assertThatThrownBy(() -> componentTypeController.deleteComponentTypesById(id))
        .isInstanceOf(EntityHasRelatedEntitiesException.class);
    verify(componentTypeFacade).deleteComponentType(id);
  }

  @Test
  void getComponentTypesById_shouldReturnComponentTypeWhenItExists() {
    // Arrange
    Long id = 1L;
    ComponentType componentType = new ComponentType();
    componentType.setId(id);
    componentType.setDomainId(1L);
    componentType.setName("Test Component Type");
    componentType.setCode("TEST_CODE");
    componentType.setDescription("Test Description");
    componentType.setOrderIndex(1);

    when(componentTypeFacade.getComponentType(id)).thenReturn(componentType);

    // Act
    ResponseEntity<ComponentType> response = componentTypeController.getComponentTypesById(id);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(componentType);
    verify(componentTypeFacade).getComponentType(id);
  }

  @Test
  void getComponentTypesById_shouldThrowNotFoundExceptionWhenComponentTypeDoesNotExist() {
    // Arrange
    Long id = 1L;

    when(componentTypeFacade.getComponentType(id))
        .thenThrow(new NotFoundException("ComponentType with id {} does not exist", id));

    // Act & Assert
    assertThatThrownBy(() -> componentTypeController.getComponentTypesById(id))
        .isInstanceOf(NotFoundException.class);
    verify(componentTypeFacade).getComponentType(id);
  }

  @Test
  void putComponentTypesById_shouldReturnUpdatedComponentTypeWhenRequestIsValid() {
    // Arrange
    Long id = 1L;
    CreateComponentTypeRequest request = new CreateComponentTypeRequest();
    request.setName("Updated Component Type");
    request.setCode("UPDATED_CODE");
    request.setDescription("Updated Description");
    request.setOrderIndex(2);

    ComponentType updatedComponentType = new ComponentType();
    updatedComponentType.setId(id);
    updatedComponentType.setDomainId(1L);
    updatedComponentType.setName("Updated Component Type");
    updatedComponentType.setCode("UPDATED_CODE");
    updatedComponentType.setDescription("Updated Description");
    updatedComponentType.setOrderIndex(2);

    when(componentTypeFacade.updateComponentType(anyLong(), any(CreateComponentTypeRequest.class)))
        .thenReturn(updatedComponentType);

    // Act
    ResponseEntity<ComponentType> response =
        componentTypeController.putComponentTypesById(id, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(updatedComponentType);
    verify(componentTypeFacade).updateComponentType(id, request);
  }

  @Test
  void putComponentTypesById_shouldThrowNotFoundExceptionWhenComponentTypeDoesNotExist() {
    // Arrange
    Long id = 1L;
    CreateComponentTypeRequest request = new CreateComponentTypeRequest();
    request.setName("Updated Component Type");
    request.setCode("UPDATED_CODE");
    request.setDescription("Updated Description");
    request.setOrderIndex(2);

    when(componentTypeFacade.updateComponentType(anyLong(), any(CreateComponentTypeRequest.class)))
        .thenThrow(new NotFoundException("ComponentType with id {} does not exist", id));

    // Act & Assert
    assertThatThrownBy(() -> componentTypeController.putComponentTypesById(id, request))
        .isInstanceOf(NotFoundException.class);
    verify(componentTypeFacade).updateComponentType(id, request);
  }

  @Test
  void getDomainsByIdComponentTypes_shouldReturnComponentTypesWhenDomainExists() {
    // Arrange
    Long domainId = 1L;

    ComponentType componentType1 = new ComponentType();
    componentType1.setId(1L);
    componentType1.setDomainId(domainId);
    componentType1.setName("Test Component Type 1");
    componentType1.setCode("TEST_CODE_1");
    componentType1.setDescription("Test Description 1");
    componentType1.setOrderIndex(1);

    ComponentType componentType2 = new ComponentType();
    componentType2.setId(2L);
    componentType2.setDomainId(domainId);
    componentType2.setName("Test Component Type 2");
    componentType2.setCode("TEST_CODE_2");
    componentType2.setDescription("Test Description 2");
    componentType2.setOrderIndex(2);

    List<ComponentType> componentTypes = List.of(componentType1, componentType2);

    when(componentTypeFacade.getComponentTypesByDomainId(domainId)).thenReturn(componentTypes);

    // Act
    ResponseEntity<List<ComponentType>> response =
        componentTypeController.getDomainsByIdComponentTypes(domainId);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(componentTypes);
    verify(componentTypeFacade).getComponentTypesByDomainId(domainId);
  }

  @Test
  void getDomainsByIdComponentTypes_shouldThrowBusinessExceptionWhenDomainDoesNotExist() {
    // Arrange
    Long domainId = 1L;

    when(componentTypeFacade.getComponentTypesByDomainId(domainId))
        .thenThrow(new BusinessException("Domain with id {} does not exist", domainId));

    // Act & Assert
    assertThatThrownBy(() -> componentTypeController.getDomainsByIdComponentTypes(domainId))
        .isInstanceOf(BusinessException.class);
    verify(componentTypeFacade).getComponentTypesByDomainId(domainId);
  }

  @Test
  void postDomainsByIdComponentTypes_shouldReturnCreatedComponentTypeWhenRequestIsValid() {
    // Arrange
    Long domainId = 1L;
    CreateComponentTypeRequest request = new CreateComponentTypeRequest();
    request.setName("Test Component Type");
    request.setCode("TEST_CODE");
    request.setDescription("Test Description");
    request.setOrderIndex(1);

    ComponentType createdComponentType = new ComponentType();
    createdComponentType.setId(1L);
    createdComponentType.setDomainId(domainId);
    createdComponentType.setName("Test Component Type");
    createdComponentType.setCode("TEST_CODE");
    createdComponentType.setDescription("Test Description");
    createdComponentType.setOrderIndex(1);

    when(componentTypeFacade.createComponentType(anyLong(), any(CreateComponentTypeRequest.class)))
        .thenReturn(createdComponentType);

    // Act
    ResponseEntity<ComponentType> response =
        componentTypeController.postDomainsByIdComponentTypes(domainId, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(createdComponentType);
    verify(componentTypeFacade).createComponentType(domainId, request);
  }

  @Test
  void postDomainsByIdComponentTypes_shouldThrowBusinessExceptionWhenDomainDoesNotExist() {
    // Arrange
    Long domainId = 1L;
    CreateComponentTypeRequest request = new CreateComponentTypeRequest();
    request.setName("Test Component Type");
    request.setCode("TEST_CODE");
    request.setDescription("Test Description");
    request.setOrderIndex(1);

    when(componentTypeFacade.createComponentType(anyLong(), any(CreateComponentTypeRequest.class)))
        .thenThrow(new BusinessException("Domain with id {} does not exist", domainId));

    // Act & Assert
    assertThatThrownBy(
            () -> componentTypeController.postDomainsByIdComponentTypes(domainId, request))
        .isInstanceOf(BusinessException.class);
    verify(componentTypeFacade).createComponentType(domainId, request);
  }

  @Test
  void
      postDomainsByIdComponentTypes_shouldThrowEntityAlreadyExistsExceptionWhenComponentTypeNameAlreadyExists() {
    // Arrange
    Long domainId = 1L;
    CreateComponentTypeRequest request = new CreateComponentTypeRequest();
    request.setName("Test Component Type");
    request.setCode("TEST_CODE");
    request.setDescription("Test Description");
    request.setOrderIndex(1);

    when(componentTypeFacade.createComponentType(anyLong(), any(CreateComponentTypeRequest.class)))
        .thenThrow(
            new EntityAlreadyExistsException(
                "ComponentType with name {} already exists", "Test Component Type"));

    // Act & Assert
    assertThatThrownBy(
            () -> componentTypeController.postDomainsByIdComponentTypes(domainId, request))
        .isInstanceOf(EntityAlreadyExistsException.class);
    verify(componentTypeFacade).createComponentType(domainId, request);
  }
}
