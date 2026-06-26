package ru.sultanyarov.configurator.application.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.application.mapper.ComponentMapper;
import ru.sultanyarov.configurator.application.service.ComponentService;

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
