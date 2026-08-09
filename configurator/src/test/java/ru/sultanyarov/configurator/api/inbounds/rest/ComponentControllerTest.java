package ru.sultanyarov.configurator.api.inbounds.rest;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.mock.web.MockMultipartFile;
import ru.sultanyarov.configurator.api.inbounds.rest.controller.ComponentController;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ReorderComponentImagesRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateComponentRequest;
import ru.sultanyarov.configurator.application.facade.ComponentFacade;

@ExtendWith(MockitoExtension.class)
class ComponentControllerTest {

  @Mock private ComponentFacade componentFacade;

  @InjectMocks private ComponentController componentController;

  @Test
  void componentsPost_shouldDelegateCreationToFacade() {
    CreateComponentRequest request = new CreateComponentRequest();
    Component component = new Component();

    when(componentFacade.createComponent(request)).thenReturn(component);

    ResponseEntity<Component> response = componentController.componentsPost(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isSameAs(component);
    verify(componentFacade).createComponent(request);
  }

  @Test
  void domainsDomainIdComponentsGet_shouldDelegateSearchToFacade() {
    ComponentPage componentPage = new ComponentPage();

    when(componentFacade.getComponentsByDomainId(1L, 2L, "name", true, 0, 10))
        .thenReturn(componentPage);

    ResponseEntity<ComponentPage> response =
        componentController.domainsDomainIdComponentsGet(1L, 2L, "name", true, 0, 10);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(componentPage);
    verify(componentFacade).getComponentsByDomainId(1L, 2L, "name", true, 0, 10);
  }

  @Test
  void componentsIdPut_shouldDelegateUpdateToFacade() {
    UpdateComponentRequest request = new UpdateComponentRequest(1L, "Updated", List.of());
    Component component = new Component();

    when(componentFacade.updateComponent(7L, request)).thenReturn(component);

    ResponseEntity<Component> response = componentController.componentsIdPut(7L, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(component);
    verify(componentFacade).updateComponent(7L, request);
  }

  @Test
  void componentsIdGet_shouldDelegateRetrievalToFacade() {
    Component component = new Component();

    when(componentFacade.getComponentById(1L)).thenReturn(component);

    ResponseEntity<Component> response = componentController.componentsIdGet(1L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(component);
    verify(componentFacade).getComponentById(1L);
  }

  @Test
  void componentsIdDelete_shouldDelegateArchivingToFacade() {
    ResponseEntity<Void> response = componentController.componentsIdDelete(7L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    assertThat(response.getBody()).isNull();
    verify(componentFacade).archiveComponent(7L);
  }

  @Test
  void componentsIdRestorePost_shouldDelegateRestorationToFacade() {
    Component component = new Component();
    when(componentFacade.restoreComponent(7L)).thenReturn(component);

    ResponseEntity<Component> response = componentController.componentsIdRestorePost(7L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(component);
    verify(componentFacade).restoreComponent(7L);
  }

  @Test
  void componentsIdImagesPost_shouldDelegateUploadToFacade() {
    MockMultipartFile file =
        new MockMultipartFile("file", "image.png", "image/png", new byte[] {1, 2, 3});
    ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage image =
        new ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage(10L, "/image.png");
    when(componentFacade.uploadComponentImage(7L, file, 2)).thenReturn(image);

    ResponseEntity<ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage> response =
        componentController.componentsIdImagesPost(7L, file, 2);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isSameAs(image);
    verify(componentFacade).uploadComponentImage(7L, file, 2);
  }

  @Test
  void componentsIdImagesGet_shouldDelegateRetrievalToFacade() {
    List<ComponentImage> images = List.of(new ComponentImage(10L, "/image.png").orderIndex(2));
    when(componentFacade.getComponentImages(7L)).thenReturn(images);

    ResponseEntity<List<ComponentImage>> response = componentController.componentsIdImagesGet(7L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(images);
    verify(componentFacade).getComponentImages(7L);
  }

  @Test
  void componentsIdImagesOrderPut_shouldDelegateReplacementToFacade() {
    ReorderComponentImagesRequest request = new ReorderComponentImagesRequest(List.of(42L, 41L));
    List<ComponentImage> images =
        List.of(
            new ComponentImage(42L, "/component-images/42/content").orderIndex(0),
            new ComponentImage(41L, "/component-images/41/content").orderIndex(1));
    when(componentFacade.reorderComponentImages(7L, request.getImageIds())).thenReturn(images);

    ResponseEntity<List<ComponentImage>> response =
        componentController.componentsIdImagesOrderPut(7L, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(images);
    verify(componentFacade).reorderComponentImages(7L, request.getImageIds());
  }
}
