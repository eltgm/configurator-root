package ru.sultanyarov.configurator.api.inbounds.rest.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.api.inbounds.rest.AttributesApi;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentTypeAttributeSettingsRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateAttributeDefinitionRequest;
import ru.sultanyarov.configurator.application.facade.AttributesFacade;

@RestController
@RequiredArgsConstructor
public class AttributesController implements AttributesApi {
  private final AttributesFacade attributesFacade;

  @Override
  public ResponseEntity<AttributeDefinition> putAttributesById(
      Long id, CreateAttributeDefinitionRequest createAttributeDefinitionRequest) {
    return ResponseEntity.ok(
        attributesFacade.updateAttribute(id, createAttributeDefinitionRequest));
  }

  @Override
  public ResponseEntity<List<AttributeDefinition>> getComponentTypesByIdAttributes(Long id) {
    return ResponseEntity.ok(attributesFacade.getAttributesByComponentTypeId(id));
  }

  @Override
  public ResponseEntity<AttributeDefinition> postComponentTypesByIdAttributes(
      Long id, CreateAttributeDefinitionRequest createAttributeDefinitionRequest) {
    return ResponseEntity.status(CREATED)
        .body(attributesFacade.createAttribute(id, createAttributeDefinitionRequest));
  }

  @Override
  public ResponseEntity<List<AttributeDefinition>> getDomainsByDomainIdAttributes(Long domainId) {
    return ResponseEntity.ok(attributesFacade.getAttributesByDomainId(domainId));
  }

  @Override
  public ResponseEntity<AttributeDefinition> postDomainsByDomainIdAttributes(
      Long domainId, CreateAttributeDefinitionRequest createAttributeDefinitionRequest) {
    return ResponseEntity.status(CREATED)
        .body(attributesFacade.createCatalogAttribute(domainId, createAttributeDefinitionRequest));
  }

  @Override
  public ResponseEntity<AttributeDefinition>
      putComponentTypesByComponentTypeIdAttributesByAttributeId(
          Long componentTypeId,
          Long attributeId,
          ComponentTypeAttributeSettingsRequest componentTypeAttributeSettingsRequest) {
    return ResponseEntity.ok(
        attributesFacade.attachAttribute(
            componentTypeId, attributeId, componentTypeAttributeSettingsRequest));
  }

  @Override
  public ResponseEntity<Void> deleteComponentTypesByComponentTypeIdAttributesByAttributeId(
      Long componentTypeId, Long attributeId) {
    attributesFacade.detachAttribute(componentTypeId, attributeId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> deleteAttributesById(Long id) {
    attributesFacade.deleteAttribute(id);
    return ResponseEntity.noContent().build();
  }
}
