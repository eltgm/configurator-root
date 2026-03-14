package ru.sultanyarov.configurator.service.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.domain.dto.Component;
import ru.sultanyarov.configurator.domain.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.service.core.ComponentService;
import ru.sultanyarov.configurator.service.mapper.ComponentMapper;

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
}
