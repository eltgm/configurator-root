package ru.sultanyarov.configurator.api.inbounds.rest.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.api.inbounds.rest.ComponentTypesApi;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentType;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentTypeRequest;
import ru.sultanyarov.configurator.application.facade.ComponentTypeFacade;

@RestController
@RequiredArgsConstructor
public class ComponentTypeController implements ComponentTypesApi {
  private final ComponentTypeFacade componentTypeFacade;

  @Override
  public ResponseEntity<Void> deleteComponentTypesById(Long id) {
    componentTypeFacade.deleteComponentType(id);
    return ResponseEntity.status(NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<ComponentType> getComponentTypesById(Long id) {
    return ResponseEntity.ok(componentTypeFacade.getComponentType(id));
  }

  @Override
  public ResponseEntity<ComponentType> putComponentTypesById(
      Long id, CreateComponentTypeRequest createComponentTypeRequest) {
    return ResponseEntity.ok(
        componentTypeFacade.updateComponentType(id, createComponentTypeRequest));
  }

  @Override
  public ResponseEntity<List<ComponentType>> getDomainsByIdComponentTypes(Long id) {
    return ResponseEntity.ok(componentTypeFacade.getComponentTypesByDomainId(id));
  }

  @Override
  public ResponseEntity<ComponentType> postDomainsByIdComponentTypes(
      Long domainId, CreateComponentTypeRequest createComponentTypeRequest) {
    return ResponseEntity.status(CREATED)
        .body(componentTypeFacade.createComponentType(domainId, createComponentTypeRequest));
  }
}
