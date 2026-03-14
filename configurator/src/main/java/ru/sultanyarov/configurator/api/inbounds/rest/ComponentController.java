package ru.sultanyarov.configurator.api.inbounds.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.sultanyarov.configurator.domain.dto.Component;
import ru.sultanyarov.configurator.domain.dto.ComponentImage;
import ru.sultanyarov.configurator.domain.dto.ComponentPage;
import ru.sultanyarov.configurator.domain.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.repository.ComponentTypeRepository;
import ru.sultanyarov.configurator.service.facade.ComponentFacade;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class ComponentController implements ComponentsApi {
    private final ComponentTypeRepository componentTypeRepository;
    private final ComponentFacade componentFacade;

    /*@Override
    public ResponseEntity<Void> componentsIdDelete(Long id) {
        componentFacade.deleteComponentById(id);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Component> componentsIdGet(Long id) {
        return ResponseEntity.ok(componentFacade.getComponentById(id));
    }

    @Override
    public ResponseEntity<List<ComponentImage>> componentsIdImagesGet(Long id) {
        return ResponseEntity.ok(componentFacade.getComponentImagesById(id));
    }

    @Override
    public ResponseEntity<ComponentImage> componentsIdImagesPost(Long id, MultipartFile file, Integer orderIndex) {
        return ResponseEntity.ok(componentFacade.createComponentImage(id, file, orderIndex));
    }

    @Override
    public ResponseEntity<Component> componentsIdPut(Long id, CreateComponentRequest createComponentRequest) {
        return ResponseEntity.ok(componentFacade.updateComponent(id, createComponentRequest));
    }*/

    @Override
    public ResponseEntity<Void> componentsIdDelete(Long id) {
        return null;
    }

    @Override
    public ResponseEntity<Component> componentsIdGet(Long id) {
        return null;
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
    public ResponseEntity<Component> componentsIdPut(Long id, CreateComponentRequest createComponentRequest) {
        return null;
    }

    @Override
    public ResponseEntity<Component> componentsPost(CreateComponentRequest createComponentRequest) {
        return ResponseEntity.ok(componentFacade.createComponent(createComponentRequest));
    }

    @Override
    public ResponseEntity<ComponentPage> domainsDomainIdComponentsGet(Long domainId, Long componentTypeId, String name, Integer page, Integer size) {
        return null;
    }

/*    @Override
    public ResponseEntity<ComponentPage> domainsDomainIdComponentsGet(Long domainId, Long componentTypeId, String name, Integer page, Integer size) {
        return ResponseEntity.ok(componentFacade.getComponentsByDomainId(domainId, componentTypeId, name, page, size));
    }*/

    @GetMapping("/test")
    public ResponseEntity<Void> getAttributesByComponentTypeId() {
        Optional<ComponentType> componentTypeById = componentTypeRepository.getComponentTypeById(1L);
        return null;
    }
}
