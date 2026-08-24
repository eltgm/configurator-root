package ru.sultanyarov.configurator.api.inbounds.rest.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.sultanyarov.configurator.api.inbounds.rest.ComponentsApi;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ReorderComponentImagesRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateComponentRequest;
import ru.sultanyarov.configurator.application.facade.ComponentFacade;

@RestController
@RequiredArgsConstructor
public class ComponentController implements ComponentsApi {
  private final ComponentFacade componentFacade;

  @Override
  public ResponseEntity<Void> deleteComponentsById(Long id) {
    componentFacade.archiveComponent(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Component> getComponentsById(Long id) {
    return ResponseEntity.ok(componentFacade.getComponentById(id));
  }

  @Override
  public ResponseEntity<List<ComponentImage>> getComponentsByIdImages(Long id) {
    return ResponseEntity.ok(componentFacade.getComponentImages(id));
  }

  @Override
  public ResponseEntity<List<ComponentImage>> putComponentsByIdImagesOrder(
      Long id, ReorderComponentImagesRequest reorderComponentImagesRequest) {
    return ResponseEntity.ok(
        componentFacade.reorderComponentImages(id, reorderComponentImagesRequest.getImageIds()));
  }

  @Override
  public ResponseEntity<ComponentImage> postComponentsByIdImages(
      Long id, MultipartFile file, Integer orderIndex) {
    return ResponseEntity.status(CREATED)
        .body(componentFacade.uploadComponentImage(id, file, orderIndex));
  }

  @Override
  public ResponseEntity<Component> putComponentsById(
      Long id, UpdateComponentRequest updateComponentRequest) {
    return ResponseEntity.ok(componentFacade.updateComponent(id, updateComponentRequest));
  }

  @Override
  public ResponseEntity<Component> postComponentsByIdRestore(Long id) {
    return ResponseEntity.ok(componentFacade.restoreComponent(id));
  }

  @Override
  public ResponseEntity<Component> postComponents(CreateComponentRequest createComponentRequest) {
    return ResponseEntity.status(CREATED)
        .body(componentFacade.createComponent(createComponentRequest));
  }

  @Override
  public ResponseEntity<ComponentPage> getDomainsByDomainIdComponents(
      Long domainId,
      Long componentTypeId,
      String name,
      Boolean archived,
      Integer page,
      Integer size) {
    return ResponseEntity.ok(
        componentFacade.getComponentsByDomainId(
            domainId, componentTypeId, name, archived, page, size));
  }
}
