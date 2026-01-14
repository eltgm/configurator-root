package ru.sultanyarov.configurator.api.inbounds.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.domain.dto.ComponentType;
import ru.sultanyarov.configurator.domain.dto.CreateComponentTypeRequest;
import ru.sultanyarov.configurator.service.facade.ComponentTypeFacade;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequiredArgsConstructor
public class ComponentTypeController implements ComponentTypesApi {
    private final ComponentTypeFacade componentTypeFacade;

    @Override
    public ResponseEntity<Void> componentTypesIdDelete(Long id) {
        componentTypeFacade.deleteComponentType(id);
        return ResponseEntity.status(NO_CONTENT).build();
    }

    @Override
    public ResponseEntity<ComponentType> componentTypesIdGet(Long id) {
        return ResponseEntity.ok(componentTypeFacade.getComponentType(id));
    }

    @Override
    public ResponseEntity<ComponentType> componentTypesIdPut(Long id, CreateComponentTypeRequest createComponentTypeRequest) {
        return ResponseEntity.ok(componentTypeFacade.updateComponentType(id, createComponentTypeRequest));
    }

    @Override
    public ResponseEntity<List<ComponentType>> domainsIdComponentTypesGet(Long id) {
        return ResponseEntity.ok(componentTypeFacade.getComponentTypesByDomainId(id));
    }

    @Override
    public ResponseEntity<ComponentType> domainsIdComponentTypesPost(Long domainId, CreateComponentTypeRequest createComponentTypeRequest) {
        return ResponseEntity.status(CREATED)
                .body(componentTypeFacade.createComponentType(domainId, createComponentTypeRequest));
    }
}
