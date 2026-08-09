package ru.sultanyarov.configurator.application.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import ru.sultanyarov.configurator.application.port.out.*;
import ru.sultanyarov.configurator.application.validator.*;
import ru.sultanyarov.configurator.domain.exception.*;
import ru.sultanyarov.configurator.domain.model.*;

@ExtendWith(MockitoExtension.class)
class ComponentServiceImplTest {

  @Mock private ComponentRepository componentRepository;

  @Mock private ComponentTypeService componentTypeService;

  @Mock private AttributeValueService attributeValueService;

  @Mock private ComponentValidator componentValidator;

  @Mock private ComponentImageValidator componentImageValidator;

  @Mock private ComponentImageStorage componentImageStorage;

  @Mock private DomainService domainService;

  @InjectMocks private ComponentServiceImpl componentService;

  @Test
  void create_shouldValidateCreateComponentPersistItAndAttachCreatedAttributes() {
    AttributeValue attributeValue =
        AttributeValue.builder().attributeDefinitionId(11L).value("42").build();
    Component componentToCreate =
        Component.builder()
            .componentTypeId(5L)
            .name("Component")
            .attributes(List.of(attributeValue))
            .build();
    AttributeDefinition attributeDefinition =
        AttributeDefinition.builder()
            .id(11L)
            .componentTypeId(5L)
            .name("force")
            .label("Force")
            .dataType(DataType.NUMBER)
            .build();
    ComponentType componentType =
        ComponentType.builder().id(5L).attributeDefinitions(List.of(attributeDefinition)).build();
    Component createdComponent =
        Component.builder().id(100L).componentTypeId(5L).name("Component").build();
    List<AttributeValue> createdAttributeValues =
        List.of(AttributeValue.builder().id(1L).attributeDefinitionId(11L).value("42").build());
    Map<Long, AttributeDefinition> attributeDefinitionsMap = Map.of(11L, attributeDefinition);

    when(componentTypeService.getById(5L)).thenReturn(componentType);
    when(componentRepository.createComponent(any(Component.class)))
        .thenReturn(Optional.of(createdComponent));
    when(attributeValueService.createAttributeValues(anyList(), eq(100L)))
        .thenReturn(createdAttributeValues);

    Component result = componentService.create(componentToCreate);

    assertThat(result).isSameAs(createdComponent);
    assertThat(result.getAttributes()).isEqualTo(createdAttributeValues);
    assertThat(result.getImages()).isEmpty();
    verify(componentTypeService).getById(5L);
    verify(componentValidator)
        .validateCreation(componentToCreate, componentType, attributeDefinitionsMap);
    verify(componentRepository).createComponent(any(Component.class));
    verify(attributeValueService).createAttributeValues(anyList(), eq(100L));
  }

  @Test
  void create_shouldThrowBusinessExceptionWhenRepositoryDidNotCreateComponent() {
    Component componentToCreate =
        Component.builder().componentTypeId(5L).name("Component").attributes(List.of()).build();

    when(componentTypeService.getById(5L))
        .thenReturn(ComponentType.builder().id(5L).attributeDefinitions(List.of()).build());
    when(componentRepository.createComponent(any(Component.class))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> componentService.create(componentToCreate))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Failed to create component");
  }

  @Test
  void update_shouldReplaceEditableStateAndAttributesWhilePreservingImages() {
    ComponentImage image =
        ComponentImage.builder()
            .id(30L)
            .componentId(7L)
            .objectKey("components/7/image.jpg")
            .orderIndex(1)
            .build();
    Component existingComponent =
        Component.builder()
            .id(7L)
            .componentTypeId(5L)
            .name("Existing")
            .archived(false)
            .images(List.of(image))
            .build();
    AttributeDefinition attributeDefinition =
        AttributeDefinition.builder()
            .id(11L)
            .componentTypeId(5L)
            .name("force")
            .label("Force")
            .dataType(DataType.NUMBER)
            .build();
    ComponentType componentType =
        ComponentType.builder()
            .id(5L)
            .attributeDefinitions(List.of(attributeDefinition))
            .components(List.of(existingComponent))
            .build();
    Component componentToUpdate =
        Component.builder()
            .componentTypeId(5L)
            .name(" Updated ")
            .brand("Brand")
            .description("Description")
            .attributes(
                List.of(AttributeValue.builder().attributeDefinitionId(11L).value("55").build()))
            .build();
    Component persistedComponent =
        Component.builder()
            .id(7L)
            .componentTypeId(5L)
            .name("Updated")
            .brand("Brand")
            .description("Description")
            .archived(false)
            .build();
    List<AttributeValue> persistedAttributes =
        List.of(
            AttributeValue.builder()
                .id(41L)
                .attributeDefinitionId(11L)
                .name("force")
                .label("Force")
                .dataType(DataType.NUMBER)
                .value("55")
                .build());

    when(componentRepository.getById(7L)).thenReturn(Optional.of(existingComponent));
    when(componentTypeService.getById(5L)).thenReturn(componentType);
    when(componentRepository.updateComponent(7L, componentToUpdate))
        .thenReturn(Optional.of(persistedComponent));
    when(attributeValueService.replaceAttributeValues(anyList(), eq(7L)))
        .thenReturn(persistedAttributes);

    Component result = componentService.update(7L, componentToUpdate);

    assertThat(componentToUpdate.getName()).isEqualTo("Updated");
    assertThat(result).isSameAs(persistedComponent);
    assertThat(result.getAttributes()).isEqualTo(persistedAttributes);
    assertThat(result.getImages()).containsExactly(image);
    verify(componentValidator)
        .validateUpdate(
            eq(componentToUpdate),
            eq(existingComponent),
            eq(componentType),
            eq(Map.of(11L, attributeDefinition)));
    verify(componentRepository).updateComponent(7L, componentToUpdate);
    verify(attributeValueService)
        .replaceAttributeValues(
            eq(
                List.of(
                    AttributeValue.builder()
                        .attributeDefinitionId(11L)
                        .name("force")
                        .label("Force")
                        .dataType(DataType.NUMBER)
                        .value("55")
                        .build())),
            eq(7L));
  }

  @Test
  void update_shouldRejectInvalidComponentBeforePersistence() {
    Component existingComponent =
        Component.builder().id(7L).componentTypeId(5L).name("Existing").build();
    Component componentToUpdate =
        Component.builder().componentTypeId(6L).name("Updated").attributes(List.of()).build();
    ComponentType componentType =
        ComponentType.builder().id(5L).attributeDefinitions(List.of()).build();

    when(componentRepository.getById(7L)).thenReturn(Optional.of(existingComponent));
    when(componentTypeService.getById(5L)).thenReturn(componentType);
    doThrow(new ValidationException("Changing component type is not supported"))
        .when(componentValidator)
        .validateUpdate(componentToUpdate, existingComponent, componentType, Map.of());

    assertThatThrownBy(() -> componentService.update(7L, componentToUpdate))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Changing component type");

    verify(componentRepository, never()).updateComponent(any(), any());
    verifyNoInteractions(attributeValueService);
  }

  @Test
  void update_shouldThrowBusinessExceptionWhenRepositoryDidNotUpdateComponent() {
    Component existingComponent =
        Component.builder().id(7L).componentTypeId(5L).name("Existing").build();
    Component componentToUpdate =
        Component.builder().componentTypeId(5L).name("Updated").attributes(List.of()).build();
    ComponentType componentType =
        ComponentType.builder().id(5L).attributeDefinitions(List.of()).build();

    when(componentRepository.getById(7L)).thenReturn(Optional.of(existingComponent));
    when(componentTypeService.getById(5L)).thenReturn(componentType);
    when(componentRepository.updateComponent(7L, componentToUpdate)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> componentService.update(7L, componentToUpdate))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Failed to update component");

    verifyNoInteractions(attributeValueService);
  }

  @Test
  void getById_shouldReturnComponentFromRepository() {
    Component component = Component.builder().id(7L).build();
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));

    assertThat(componentService.getById(7L)).isSameAs(component);
  }

  @Test
  void getById_shouldThrowNotFoundExceptionWhenComponentDoesNotExist() {
    when(componentRepository.getById(7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> componentService.getById(7L))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("7");
  }

  @Test
  void getImagesByComponentId_shouldReturnImagesForArchivedComponent() {
    ComponentImage firstImage =
        ComponentImage.builder()
            .id(9L)
            .componentId(7L)
            .objectKey("components/7/first.png")
            .orderIndex(0)
            .build();
    ComponentImage secondImage =
        ComponentImage.builder()
            .id(10L)
            .componentId(7L)
            .objectKey("components/7/second.png")
            .orderIndex(1)
            .build();
    Component component =
        Component.builder().id(7L).archived(true).images(List.of(firstImage, secondImage)).build();
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));

    List<ComponentImage> result = componentService.getImagesByComponentId(7L);

    assertThat(result).containsExactly(firstImage, secondImage);
    verify(componentRepository).getById(7L);
    verifyNoInteractions(componentImageStorage);
  }

  @Test
  void getImagesByComponentId_shouldReturnEmptyListWhenRepositoryHasNoImagesCollection() {
    Component component = Component.builder().id(7L).images(null).build();
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));

    assertThat(componentService.getImagesByComponentId(7L)).isEmpty();
  }

  @Test
  void getImagesByComponentId_shouldThrowNotFoundExceptionWhenComponentDoesNotExist() {
    when(componentRepository.getById(7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> componentService.getImagesByComponentId(7L))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("7");
  }

  @Test
  void getImageContent_shouldReadStoredObjectByImageId() {
    ComponentImage image =
        ComponentImage.builder()
            .id(42L)
            .componentId(7L)
            .objectKey("components/7/image.webp")
            .build();
    ComponentImageContent content = new ComponentImageContent(new byte[] {1, 2, 3}, "image/webp");
    when(componentRepository.getImageById(42L)).thenReturn(Optional.of(image));
    when(componentImageStorage.read("components/7/image.webp")).thenReturn(content);

    assertThat(componentService.getImageContent(42L)).isSameAs(content);
    verify(componentRepository).getImageById(42L);
    verify(componentImageStorage).read("components/7/image.webp");
  }

  @Test
  void getImageContent_shouldThrowNotFoundExceptionBeforeStorageWhenImageDoesNotExist() {
    when(componentRepository.getImageById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> componentService.getImageContent(404L))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("404");

    verifyNoInteractions(componentImageStorage);
  }

  @Test
  void deleteImage_shouldDeleteStoredObjectBeforeMetadata() {
    ComponentImage image = new ComponentImage(42L, 7L, "components/7/image.webp", 0);
    Component component = Component.builder().id(7L).archived(false).images(List.of(image)).build();
    when(componentRepository.getImageById(42L)).thenReturn(Optional.of(image));
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    when(componentRepository.deleteImageById(42L)).thenReturn(true);

    componentService.deleteImage(42L);

    InOrder deletionOrder = inOrder(componentImageStorage, componentRepository);
    deletionOrder.verify(componentImageStorage).delete("components/7/image.webp");
    deletionOrder.verify(componentRepository).deleteImageById(42L);
  }

  @Test
  void deleteImage_shouldThrowNotFoundBeforeStorageWhenImageDoesNotExist() {
    when(componentRepository.getImageById(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> componentService.deleteImage(404L))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("404");

    verifyNoInteractions(componentImageStorage);
    verify(componentRepository, never()).deleteImageById(anyLong());
  }

  @Test
  void deleteImage_shouldRejectArchivedComponentBeforeStorage() {
    ComponentImage image = new ComponentImage(42L, 7L, "components/7/image.webp", 0);
    Component component = Component.builder().id(7L).archived(true).images(List.of(image)).build();
    when(componentRepository.getImageById(42L)).thenReturn(Optional.of(image));
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));

    assertThatThrownBy(() -> componentService.deleteImage(42L))
        .isInstanceOf(ComponentArchivedException.class)
        .hasMessageContaining("7");

    verifyNoInteractions(componentImageStorage);
    verify(componentRepository, never()).deleteImageById(anyLong());
  }

  @Test
  void deleteImage_shouldKeepMetadataWhenStorageDeletionFails() {
    ComponentImage image = new ComponentImage(42L, 7L, "components/7/image.webp", 0);
    Component component = Component.builder().id(7L).archived(false).images(List.of(image)).build();
    ExternalStorageException storageException =
        new ExternalStorageException(new IllegalStateException("offline"), "unavailable");
    when(componentRepository.getImageById(42L)).thenReturn(Optional.of(image));
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    doThrow(storageException).when(componentImageStorage).delete(image.objectKey());

    assertThatThrownBy(() -> componentService.deleteImage(42L)).isSameAs(storageException);

    verify(componentRepository, never()).deleteImageById(anyLong());
  }

  @Test
  void deleteImage_shouldThrowBusinessExceptionWhenMetadataWasNotDeleted() {
    ComponentImage image = new ComponentImage(42L, 7L, "components/7/image.webp", 0);
    Component component = Component.builder().id(7L).archived(false).images(List.of(image)).build();
    when(componentRepository.getImageById(42L)).thenReturn(Optional.of(image));
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    when(componentRepository.deleteImageById(42L)).thenReturn(false);

    assertThatThrownBy(() -> componentService.deleteImage(42L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("42");
  }

  @Test
  void reorderImages_shouldPersistAndReturnContiguousTargetOrder() {
    ComponentImage first = new ComponentImage(41L, 7L, "components/7/first.webp", 5);
    ComponentImage second = new ComponentImage(42L, 7L, "components/7/second.webp", null);
    Component component =
        Component.builder().id(7L).archived(false).images(List.of(first, second)).build();
    List<Long> targetOrder = List.of(42L, 41L);
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    when(componentRepository.updateImageOrder(7L, targetOrder)).thenReturn(2);

    List<ComponentImage> result = componentService.reorderImages(7L, targetOrder);

    assertThat(result)
        .extracting(ComponentImage::id, ComponentImage::orderIndex)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(42L, 0),
            org.assertj.core.groups.Tuple.tuple(41L, 1));
    verify(componentImageValidator).validateOrder(component.getImages(), targetOrder);
    verify(componentRepository).updateImageOrder(7L, targetOrder);
  }

  @Test
  void reorderImages_shouldAcceptEmptyOrderForEmptyGalleryWithoutPersistenceCall() {
    Component component = Component.builder().id(7L).archived(false).images(List.of()).build();
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));

    assertThat(componentService.reorderImages(7L, List.of())).isEmpty();

    verify(componentImageValidator).validateOrder(List.of(), List.of());
    verify(componentRepository, never()).updateImageOrder(anyLong(), anyList());
  }

  @Test
  void reorderImages_shouldRejectArchivedComponentBeforeValidation() {
    Component component = Component.builder().id(7L).archived(true).images(List.of()).build();
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));

    assertThatThrownBy(() -> componentService.reorderImages(7L, List.of()))
        .isInstanceOf(ComponentArchivedException.class)
        .hasMessageContaining("7");

    verifyNoInteractions(componentImageValidator);
    verify(componentRepository, never()).updateImageOrder(anyLong(), anyList());
  }

  @Test
  void reorderImages_shouldNotPersistWhenValidationFails() {
    ComponentImage image = new ComponentImage(41L, 7L, "components/7/first.webp", 0);
    Component component = Component.builder().id(7L).archived(false).images(List.of(image)).build();
    List<Long> invalidOrder = List.of(99L);
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    doThrow(new ValidationException("invalid order"))
        .when(componentImageValidator)
        .validateOrder(component.getImages(), invalidOrder);

    assertThatThrownBy(() -> componentService.reorderImages(7L, invalidOrder))
        .isInstanceOf(ValidationException.class);

    verify(componentRepository, never()).updateImageOrder(anyLong(), anyList());
  }

  @Test
  void reorderImages_shouldThrowWhenRepositoryDidNotUpdateEveryImage() {
    ComponentImage image = new ComponentImage(41L, 7L, "components/7/first.webp", 0);
    Component component = Component.builder().id(7L).archived(false).images(List.of(image)).build();
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    when(componentRepository.updateImageOrder(7L, List.of(41L))).thenReturn(0);

    assertThatThrownBy(() -> componentService.reorderImages(7L, List.of(41L)))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("7");
  }

  @Test
  void getByPageByDomainId_shouldDelegateSearchWithoutComponentTypeFilter() {
    Domain domain = Domain.builder().id(1L).componentTypes(List.of()).build();
    Page<Component> page = new Page<>(List.of(), 0, 10, 0);

    when(domainService.getById(1L)).thenReturn(domain);
    when(componentRepository.findPageByDomainIdComponentTypeIdNameArchived(
            1L, null, "name", true, 0, 10))
        .thenReturn(page);

    Page<Component> result = componentService.getByPageByDomainId(1L, null, "name", true, 0, 10);

    assertThat(result).isSameAs(page);
    verify(domainService).getById(1L);
    verify(componentRepository)
        .findPageByDomainIdComponentTypeIdNameArchived(1L, null, "name", true, 0, 10);
  }

  @Test
  void getByPageByDomainId_shouldUseDefaultPaginationWhenParametersAreMissing() {
    Domain domain = Domain.builder().id(1L).componentTypes(List.of()).build();
    Page<Component> page = new Page<>(List.of(), 0, 10, 0);

    when(domainService.getById(1L)).thenReturn(domain);
    when(componentRepository.findPageByDomainIdComponentTypeIdNameArchived(
            1L, null, null, null, 0, 10))
        .thenReturn(page);

    assertThat(componentService.getByPageByDomainId(1L, null, null, null, null, null))
        .isSameAs(page);
    verify(componentRepository)
        .findPageByDomainIdComponentTypeIdNameArchived(1L, null, null, null, 0, 10);
  }

  @Test
  void getByPageByDomainId_shouldRejectInvalidPagination() {
    Domain domain = Domain.builder().id(1L).componentTypes(List.of()).build();
    when(domainService.getById(1L)).thenReturn(domain);

    assertThatThrownBy(() -> componentService.getByPageByDomainId(1L, null, null, null, -1, 10))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Page index");
    assertThatThrownBy(() -> componentService.getByPageByDomainId(1L, null, null, null, 0, 0))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Page size");
    assertThatThrownBy(() -> componentService.getByPageByDomainId(1L, null, null, null, 0, 101))
        .isInstanceOf(ValidationException.class)
        .hasMessageContaining("Page size");

    verifyNoInteractions(componentRepository);
  }

  @Test
  void getByPageByDomainId_shouldDelegateSearchWhenComponentTypeBelongsToDomain() {
    Domain domain =
        Domain.builder()
            .id(1L)
            .componentTypes(List.of(ComponentType.builder().id(2L).domainId(1L).build()))
            .build();
    Page<Component> page = new Page<>(List.of(), 1, 5, 0);

    when(domainService.getById(1L)).thenReturn(domain);
    when(componentRepository.findPageByDomainIdComponentTypeIdNameArchived(
            1L, 2L, null, false, 1, 5))
        .thenReturn(page);

    Page<Component> result = componentService.getByPageByDomainId(1L, 2L, null, false, 1, 5);

    assertThat(result).isSameAs(page);
    verify(componentRepository)
        .findPageByDomainIdComponentTypeIdNameArchived(1L, 2L, null, false, 1, 5);
  }

  @Test
  void getByPageByDomainId_shouldRejectComponentTypeFromAnotherDomain() {
    Domain domain =
        Domain.builder()
            .id(1L)
            .componentTypes(List.of(ComponentType.builder().id(3L).domainId(1L).build()))
            .build();

    when(domainService.getById(1L)).thenReturn(domain);

    assertThatThrownBy(() -> componentService.getByPageByDomainId(1L, 2L, null, null, 0, 10))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Тип компонента не принадлежит указанному домену");

    verifyNoInteractions(componentRepository);
  }

  @Test
  void getById_shouldReturnComponentWhenItExists() {
    Component component = Component.builder().id(1L).name("Component").build();

    when(componentRepository.getById(1L)).thenReturn(Optional.of(component));

    Component result = componentService.getById(1L);

    assertThat(result).isSameAs(component);
    verify(componentRepository).getById(1L);
  }

  @Test
  void archiveById_shouldArchiveExistingActiveComponent() {
    Component component = Component.builder().id(7L).archived(false).build();
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    when(componentRepository.archiveComponentById(7L)).thenReturn(true);

    componentService.archiveById(7L);

    verify(componentRepository).archiveComponentById(7L);
  }

  @Test
  void archiveById_shouldBeIdempotentForAlreadyArchivedComponent() {
    Component component = Component.builder().id(7L).archived(true).build();
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));

    componentService.archiveById(7L);

    verify(componentRepository, never()).archiveComponentById(anyLong());
  }

  @Test
  void archiveById_shouldThrowNotFoundExceptionWhenComponentDoesNotExist() {
    when(componentRepository.getById(7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> componentService.archiveById(7L))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("7");

    verify(componentRepository, never()).archiveComponentById(anyLong());
  }

  @Test
  void archiveById_shouldThrowBusinessExceptionWhenRepositoryDidNotArchiveComponent() {
    Component component = Component.builder().id(7L).archived(false).build();
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    when(componentRepository.archiveComponentById(7L)).thenReturn(false);

    assertThatThrownBy(() -> componentService.archiveById(7L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Failed to archive component");
  }

  @Test
  void restoreById_shouldRestoreArchivedComponentAndPreserveRelatedData() {
    AttributeValue attribute = AttributeValue.builder().id(11L).value("42").build();
    ComponentImage image = new ComponentImage(21L, 7L, "components/7/image.png", 0);
    Component component =
        Component.builder()
            .id(7L)
            .archived(true)
            .attributes(List.of(attribute))
            .images(List.of(image))
            .build();
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    when(componentRepository.restoreComponentById(7L)).thenReturn(true);

    Component result = componentService.restoreById(7L);

    assertThat(result).isSameAs(component);
    assertThat(result.getArchived()).isFalse();
    assertThat(result.getAttributes()).containsExactly(attribute);
    assertThat(result.getImages()).containsExactly(image);
    verify(componentRepository).restoreComponentById(7L);
  }

  @Test
  void restoreById_shouldBeIdempotentForActiveComponent() {
    Component component = Component.builder().id(7L).archived(false).build();
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));

    assertThat(componentService.restoreById(7L)).isSameAs(component);

    verify(componentRepository, never()).restoreComponentById(anyLong());
  }

  @Test
  void restoreById_shouldThrowNotFoundExceptionWhenComponentDoesNotExist() {
    when(componentRepository.getById(7L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> componentService.restoreById(7L))
        .isInstanceOf(NotFoundException.class)
        .hasMessageContaining("7");

    verify(componentRepository, never()).restoreComponentById(anyLong());
  }

  @Test
  void restoreById_shouldThrowBusinessExceptionWhenRepositoryDidNotRestoreComponent() {
    Component component = Component.builder().id(7L).archived(true).build();
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    when(componentRepository.restoreComponentById(7L)).thenReturn(false);

    assertThatThrownBy(() -> componentService.restoreById(7L))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Failed to restore component");
    assertThat(component.getArchived()).isTrue();
  }

  @Test
  void uploadImage_shouldStoreAndPersistImageWithNextOrderIndex() {
    Component component = Component.builder().id(7L).archived(false).build();
    ComponentImageUpload upload = new ComponentImageUpload(new byte[] {1, 2, 3}, "image/png");
    StoredImage storedImage = new StoredImage("components/7/image.png");
    ComponentImage expectedMetadata =
        ComponentImage.builder()
            .componentId(7L)
            .objectKey(storedImage.objectKey())
            .orderIndex(4)
            .build();
    ComponentImage createdImage =
        ComponentImage.builder()
            .id(12L)
            .componentId(7L)
            .objectKey(storedImage.objectKey())
            .orderIndex(4)
            .build();

    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    when(componentRepository.getNextImageOrderIndex(7L)).thenReturn(4);
    when(componentImageStorage.store(7L, upload)).thenReturn(storedImage);
    when(componentRepository.createImage(expectedMetadata)).thenReturn(Optional.of(createdImage));

    ComponentImage result = componentService.uploadImage(7L, upload, null);

    assertThat(result).isSameAs(createdImage);
    verify(componentImageValidator).validate(upload, null);
    verify(componentRepository).getNextImageOrderIndex(7L);
    verify(componentImageStorage).store(7L, upload);
    verify(componentRepository).createImage(expectedMetadata);
  }

  @Test
  void uploadImage_shouldUseExplicitOrderIndex() {
    Component component = Component.builder().id(7L).archived(false).build();
    ComponentImageUpload upload = new ComponentImageUpload(new byte[] {1, 2, 3}, "image/png");
    StoredImage storedImage = new StoredImage("components/7/image.png");
    ComponentImage createdImage =
        ComponentImage.builder().id(12L).componentId(7L).orderIndex(9).build();

    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    when(componentImageStorage.store(7L, upload)).thenReturn(storedImage);
    when(componentRepository.createImage(any(ComponentImage.class)))
        .thenReturn(Optional.of(createdImage));

    assertThat(componentService.uploadImage(7L, upload, 9)).isSameAs(createdImage);

    verify(componentRepository, never()).getNextImageOrderIndex(anyLong());
    verify(componentRepository)
        .createImage(
            ComponentImage.builder()
                .componentId(7L)
                .objectKey(storedImage.objectKey())
                .orderIndex(9)
                .build());
  }

  @Test
  void uploadImage_shouldRejectArchivedComponentBeforeStorage() {
    Component component = Component.builder().id(7L).archived(true).build();
    ComponentImageUpload upload = new ComponentImageUpload(new byte[] {1, 2, 3}, "image/png");
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));

    assertThatThrownBy(() -> componentService.uploadImage(7L, upload, null))
        .isInstanceOf(ComponentArchivedException.class)
        .hasMessageContaining("7");

    verifyNoInteractions(componentImageValidator, componentImageStorage);
    verify(componentRepository, never()).createImage(any());
  }

  @Test
  void uploadImage_shouldNotStoreImageWhenValidationFails() {
    Component component = Component.builder().id(7L).archived(false).build();
    ComponentImageUpload upload = new ComponentImageUpload(new byte[0], "image/png");
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    doThrow(new ValidationException("Image file must not be empty"))
        .when(componentImageValidator)
        .validate(upload, null);

    assertThatThrownBy(() -> componentService.uploadImage(7L, upload, null))
        .isInstanceOf(ValidationException.class);

    verifyNoInteractions(componentImageStorage);
    verify(componentRepository, never()).createImage(any());
  }

  @Test
  void uploadImage_shouldDeleteStoredObjectWhenMetadataPersistenceFails() {
    Component component = Component.builder().id(7L).archived(false).build();
    ComponentImageUpload upload = new ComponentImageUpload(new byte[] {1, 2, 3}, "image/png");
    StoredImage storedImage = new StoredImage("components/7/image.png");
    when(componentRepository.getById(7L)).thenReturn(Optional.of(component));
    when(componentRepository.getNextImageOrderIndex(7L)).thenReturn(0);
    when(componentImageStorage.store(7L, upload)).thenReturn(storedImage);
    when(componentRepository.createImage(any(ComponentImage.class))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> componentService.uploadImage(7L, upload, null))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Failed to persist image metadata");

    verify(componentImageStorage).delete(storedImage.objectKey());
  }
}
