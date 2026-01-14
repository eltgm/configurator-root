package ru.sultanyarov.configurator.api.inbounds.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.domain.dto.AttributeDefinition;
import ru.sultanyarov.configurator.domain.dto.CreateAttributeDefinitionRequest;
import ru.sultanyarov.configurator.service.facade.AttributesFacade;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
public class AttributesController implements AttributesApi {
    private final AttributesFacade attributesFacade;

    @Override
    public ResponseEntity<AttributeDefinition> attributesIdPut(Long id, CreateAttributeDefinitionRequest createAttributeDefinitionRequest) {
        return ResponseEntity.ok(
                attributesFacade.updateAttribute(id, createAttributeDefinitionRequest)
        );
    }

    @Override
    public ResponseEntity<List<AttributeDefinition>> componentTypesIdAttributesGet(Long id) {
        return ResponseEntity.ok(
                attributesFacade.getAttributesByComponentTypeId(id)
        );
    }

    @Override
    public ResponseEntity<AttributeDefinition> componentTypesIdAttributesPost(Long id, CreateAttributeDefinitionRequest createAttributeDefinitionRequest) {
        return ResponseEntity.status(CREATED)
                .body(
                        attributesFacade.createAttribute(id, createAttributeDefinitionRequest)
                );
    }
}
