package ru.sultanyarov.configurator.application.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateComponentRequest;
import ru.sultanyarov.configurator.application.mapper.ComponentMapper;
import ru.sultanyarov.configurator.application.service.ComponentService;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.ComponentImageContent;
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;
import ru.sultanyarov.configurator.domain.model.Page;

@ExtendWith(MockitoExtension.class)
class ComponentFacadeImplTest {

  @Mock private ComponentService componentService;

  @Mock private ComponentMapper componentMapper;

  @InjectMocks private ComponentFacadeImpl componentFacade;

  @Test
  void createComponent_shouldMapRequestToEntityAndBackToDto() {
    CreateComponentRequest request = new CreateComponentRequest();
    request.setComponentTypeId(1L);
    ru.sultanyarov.configurator.domain.model.Component entity =
        new ru.sultanyarov.configurator.domain.model.Component();
    ru.sultanyarov.configurator.domain.model.Component createdEntity =
        new ru.sultanyarov.configurator.domain.model.Component();
    Component dto = new Component();

    when(componentMapper.toEntity(request)).thenReturn(entity);
    when(componentService.create(entity)).thenReturn(createdEntity);
    when(componentMapper.toDto(createdEntity)).thenReturn(dto);

    Component result = componentFacade.createComponent(request);

    assertThat(result).isSameAs(dto);
    verify(componentMapper).toEntity(request);
    verify(componentService).create(entity);
    verify(componentMapper).toDto(createdEntity);
  }

  @Test
  void updateComponent_shouldMapRequestToEntityAndBackToDto() {
    UpdateComponentRequest request = new UpdateComponentRequest(1L, "Updated", List.of());
    ru.sultanyarov.configurator.domain.model.Component entity =
        new ru.sultanyarov.configurator.domain.model.Component();
    ru.sultanyarov.configurator.domain.model.Component updatedEntity =
        new ru.sultanyarov.configurator.domain.model.Component();
    Component dto = new Component();

    when(componentMapper.toEntity(request)).thenReturn(entity);
    when(componentService.update(7L, entity)).thenReturn(updatedEntity);
    when(componentMapper.toDto(updatedEntity)).thenReturn(dto);

    Component result = componentFacade.updateComponent(7L, request);

    assertThat(result).isSameAs(dto);
    verify(componentMapper).toEntity(request);
    verify(componentService).update(7L, entity);
    verify(componentMapper).toDto(updatedEntity);
  }

  @Test
  void getComponentsByDomainId_shouldDelegateSearchToServiceAndMapPageToDto() {
    Page<ru.sultanyarov.configurator.domain.model.Component> page = new Page<>(List.of(), 0, 10, 0);
    ComponentPage pageDto = new ComponentPage();

    when(componentService.getByPageByDomainId(1L, 2L, "name", 0, 10)).thenReturn(page);
    when(componentMapper.toComponentPageDto(page)).thenReturn(pageDto);

    ComponentPage result = componentFacade.getComponentsByDomainId(1L, 2L, "name", 0, 10);

    assertThat(result).isSameAs(pageDto);
    verify(componentService).getByPageByDomainId(1L, 2L, "name", 0, 10);
    verify(componentMapper).toComponentPageDto(page);
  }

  @Test
  void getComponentById_shouldDelegateRetrievalToServiceAndMapToDto() {
    ru.sultanyarov.configurator.domain.model.Component entity =
        ru.sultanyarov.configurator.domain.model.Component.builder().id(1L).build();
    Component dto = new Component();

    when(componentService.getById(1L)).thenReturn(entity);
    when(componentMapper.toDto(entity)).thenReturn(dto);

    Component result = componentFacade.getComponentById(1L);

    assertThat(result).isSameAs(dto);
    verify(componentService).getById(1L);
    verify(componentMapper).toDto(entity);
  }

  @Test
  void archiveComponent_shouldDelegateToService() {
    componentFacade.archiveComponent(7L);

    verify(componentService).archiveById(7L);
  }

  @Test
  void uploadComponentImage_shouldMapMultipartFileAndCreatedImage() {
    byte[] content = new byte[] {1, 2, 3};
    MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", content);
    ru.sultanyarov.configurator.domain.model.ComponentImage createdImage =
        new ru.sultanyarov.configurator.domain.model.ComponentImage(9L, 7L, "/image.png", 2);
    ComponentImage imageDto = new ComponentImage(9L, "/image.png");

    when(componentService.uploadImage(eq(7L), any(ComponentImageUpload.class), eq(2)))
        .thenReturn(createdImage);
    when(componentMapper.toDto(createdImage)).thenReturn(imageDto);

    ComponentImage result = componentFacade.uploadComponentImage(7L, file, 2);

    assertThat(result).isSameAs(imageDto);
    ArgumentCaptor<ComponentImageUpload> uploadCaptor =
        ArgumentCaptor.forClass(ComponentImageUpload.class);
    verify(componentService).uploadImage(eq(7L), uploadCaptor.capture(), eq(2));
    assertThat(uploadCaptor.getValue().content()).containsExactly(content);
    assertThat(uploadCaptor.getValue().contentType()).isEqualTo("image/png");
    verify(componentMapper).toDto(createdImage);
  }

  @Test
  void uploadComponentImage_shouldRejectMissingFile() {
    assertThatThrownBy(() -> componentFacade.uploadComponentImage(7L, null, 2))
        .isInstanceOf(ValidationException.class)
        .hasMessage("Image file is required");
  }

  @Test
  void uploadComponentImage_shouldWrapMultipartReadFailure() throws IOException {
    MultipartFile file = mock(MultipartFile.class);
    when(file.getBytes()).thenThrow(new IOException("read failed"));

    assertThatThrownBy(() -> componentFacade.uploadComponentImage(7L, file, 2))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Failed to read uploaded image")
        .hasCauseInstanceOf(IOException.class);
  }

  @Test
  void getComponentImages_shouldDelegateToServiceAndMapEveryImage() {
    ru.sultanyarov.configurator.domain.model.ComponentImage firstEntity =
        new ru.sultanyarov.configurator.domain.model.ComponentImage(9L, 7L, "/first.png", 0);
    ru.sultanyarov.configurator.domain.model.ComponentImage secondEntity =
        new ru.sultanyarov.configurator.domain.model.ComponentImage(10L, 7L, "/second.png", 1);
    ComponentImage firstDto = new ComponentImage(9L, "/first.png").orderIndex(0);
    ComponentImage secondDto = new ComponentImage(10L, "/second.png").orderIndex(1);

    when(componentService.getImagesByComponentId(7L))
        .thenReturn(List.of(firstEntity, secondEntity));
    when(componentMapper.toDto(firstEntity)).thenReturn(firstDto);
    when(componentMapper.toDto(secondEntity)).thenReturn(secondDto);

    List<ComponentImage> result = componentFacade.getComponentImages(7L);

    assertThat(result).containsExactly(firstDto, secondDto);
    verify(componentService).getImagesByComponentId(7L);
    verify(componentMapper).toDto(firstEntity);
    verify(componentMapper).toDto(secondEntity);
  }

  @Test
  void getComponentImageContent_shouldDelegateToService() {
    ComponentImageContent content = new ComponentImageContent(new byte[] {1, 2, 3}, "image/png");
    when(componentService.getImageContent(42L)).thenReturn(content);

    assertThat(componentFacade.getComponentImageContent(42L)).isSameAs(content);
    verify(componentService).getImageContent(42L);
  }

  @Test
  void deleteComponentImage_shouldDelegateToService() {
    componentFacade.deleteComponentImage(42L);

    verify(componentService).deleteImage(42L);
  }

  @Test
  void reorderComponentImages_shouldDelegateToServiceAndMapEveryImage() {
    ru.sultanyarov.configurator.domain.model.ComponentImage firstEntity =
        new ru.sultanyarov.configurator.domain.model.ComponentImage(42L, 7L, "first.webp", 0);
    ru.sultanyarov.configurator.domain.model.ComponentImage secondEntity =
        new ru.sultanyarov.configurator.domain.model.ComponentImage(41L, 7L, "second.webp", 1);
    ComponentImage firstDto = new ComponentImage(42L, "/component-images/42/content").orderIndex(0);
    ComponentImage secondDto =
        new ComponentImage(41L, "/component-images/41/content").orderIndex(1);
    List<Long> targetOrder = List.of(42L, 41L);
    when(componentService.reorderImages(7L, targetOrder))
        .thenReturn(List.of(firstEntity, secondEntity));
    when(componentMapper.toDto(firstEntity)).thenReturn(firstDto);
    when(componentMapper.toDto(secondEntity)).thenReturn(secondDto);

    assertThat(componentFacade.reorderComponentImages(7L, targetOrder))
        .containsExactly(firstDto, secondDto);
    verify(componentService).reorderImages(7L, targetOrder);
    verify(componentMapper).toDto(firstEntity);
    verify(componentMapper).toDto(secondEntity);
  }
}
