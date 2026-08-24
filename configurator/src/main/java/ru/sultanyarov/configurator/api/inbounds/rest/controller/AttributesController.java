package ru.sultanyarov.configurator.api.inbounds.rest.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.api.inbounds.rest.AttributesApi;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeDefinition;
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
}
