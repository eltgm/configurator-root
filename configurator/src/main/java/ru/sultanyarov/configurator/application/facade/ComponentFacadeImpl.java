package ru.sultanyarov.configurator.application.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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
import ru.sultanyarov.configurator.domain.model.ComponentImageUpload;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentFacadeImpl implements ComponentFacade {
    private final ComponentService componentService;
    private final ComponentMapper componentMapper;

    @Override
    public Component createComponent(CreateComponentRequest createComponentRequest) {
        log.info("Creating component");
        return componentMapper.toDto(
                componentService.create(
                        componentMapper.toEntity(createComponentRequest)
                )
        );
    }

    @Override
    public Component updateComponent(Long componentId, UpdateComponentRequest updateComponentRequest) {
        log.info("Updating component with id {}", componentId);
        return componentMapper.toDto(
                componentService.update(
                        componentId,
                        componentMapper.toEntity(updateComponentRequest)
                )
        );
    }

    @Override
    public void archiveComponent(Long componentId) {
        log.info("Archiving component with id {}", componentId);
        componentService.archiveById(componentId);
    }

    @Override
    public ComponentImage uploadComponentImage(Long componentId, MultipartFile file, Integer orderIndex) {
        log.info("Uploading image for component with id {}", componentId);
        if (file == null) {
            throw new ValidationException("Image file is required");
        }

        try {
            return componentMapper.toDto(
                    componentService.uploadImage(
                            componentId,
                            new ComponentImageUpload(file.getBytes(), file.getContentType()),
                            orderIndex
                    )
            );
        } catch (IOException exception) {
            throw new BusinessException(exception, "Failed to read uploaded image");
        }
    }

    @Override
    public ComponentPage getComponentsByDomainId(Long domainId, Long componentTypeId, String name, Integer page, Integer size) {
        log.info("Getting components by domain");
        return componentMapper.toComponentPageDto(
                componentService.getByPageByDomainId(domainId, componentTypeId, name, page, size)
        );
    }

    @Override
    public Component getComponentById(Long id) {
        log.info("Getting component");
        return componentMapper.toDto(
                componentService.getById(id)
        );
    }
}
