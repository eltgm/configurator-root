package ru.sultanyarov.configurator.api.inbounds.rest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.sultanyarov.configurator.api.inbounds.rest.ComponentsApi;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateComponentRequest;
import ru.sultanyarov.configurator.application.facade.ComponentFacade;

import java.util.List;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
public class ComponentController implements ComponentsApi {
    private final ComponentFacade componentFacade;

    @Override
    public ResponseEntity<Void> componentsIdDelete(Long id) {
        componentFacade.archiveComponent(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Component> componentsIdGet(Long id) {
        return ResponseEntity.ok(componentFacade.getComponentById(id));
    }

    @Override
    public ResponseEntity<List<ComponentImage>> componentsIdImagesGet(Long id) {
        return null;
    }

    @Override
    public ResponseEntity<ComponentImage> componentsIdImagesPost(Long id, MultipartFile file, Integer orderIndex) {
        return null;
    }

    @Override
    public ResponseEntity<Component> componentsIdPut(Long id, UpdateComponentRequest updateComponentRequest) {
        return ResponseEntity.ok(componentFacade.updateComponent(id, updateComponentRequest));
    }

    @Override
    public ResponseEntity<Component> componentsPost(CreateComponentRequest createComponentRequest) {
        return ResponseEntity.status(CREATED)
                .body(componentFacade.createComponent(createComponentRequest));
    }

    @Override
    public ResponseEntity<ComponentPage> domainsDomainIdComponentsGet(Long domainId, Long componentTypeId, String name, Integer page, Integer size) {
        return ResponseEntity.ok(
                componentFacade.getComponentsByDomainId(domainId, componentTypeId, name, page, size)
        );
    }
}
