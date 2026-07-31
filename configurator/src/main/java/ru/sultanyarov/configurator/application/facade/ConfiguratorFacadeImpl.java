package ru.sultanyarov.configurator.application.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorResponse;
import ru.sultanyarov.configurator.application.mapper.ConfiguratorMapper;
import ru.sultanyarov.configurator.application.service.ConfiguratorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfiguratorFacadeImpl implements ConfiguratorFacade {
    private final ConfiguratorService configuratorService;
    private final ConfiguratorMapper configuratorMapper;

    @Override
    public ConfiguratorResponse getCompatibleComponents(
            Long domainId,
            Long baseComponentId,
            boolean includeTransitive
    ) {
        log.info(
                "Getting compatible components in domain with id {} for base component with id {}, "
                        + "include transitive: {}",
                domainId,
                baseComponentId,
                includeTransitive
        );
        return configuratorMapper.toDto(
                configuratorService.getCompatibleComponents(
                        domainId,
                        baseComponentId,
                        includeTransitive
                )
        );
    }
}
