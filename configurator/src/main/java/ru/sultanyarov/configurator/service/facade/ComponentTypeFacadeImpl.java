package ru.sultanyarov.configurator.service.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.domain.dto.ComponentType;
import ru.sultanyarov.configurator.domain.dto.CreateComponentTypeRequest;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.service.core.ComponentTypeService;
import ru.sultanyarov.configurator.service.mapper.ComponentTypeMapper;

import java.util.List;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentTypeFacadeImpl implements ComponentTypeFacade {
    private final ComponentTypeService componentTypeService;
    private final ComponentTypeMapper componentTypeMapper;

    @Override
    public ComponentType createComponentType(Long domainId, CreateComponentTypeRequest createComponentTypeRequest) {
        log.info("Creating component type in domain {}", domainId);
        validateCreateComponentTypeRequest(createComponentTypeRequest);

        return componentTypeMapper.toDto(
                componentTypeService.create(
                        componentTypeMapper.toEntityWithDomain(domainId, createComponentTypeRequest)
                )
        );
    }

    private void validateCreateComponentTypeRequest(CreateComponentTypeRequest createComponentTypeRequest) {
        String name = createComponentTypeRequest.getName();
        validateName(name);
    }

    private static void validateName(String name) {
        if (!hasText(name)) {
            throw new ValidationException("Name is required");
        }
    }

    @Override
    public ComponentType updateComponentType(Long componentTypeId, CreateComponentTypeRequest createComponentTypeRequest) {
        log.info("Updating component type with id {}", componentTypeId);
        validateCreateComponentTypeRequest(createComponentTypeRequest);

        return componentTypeMapper.toDto(
                componentTypeService.update(
                        componentTypeId,
                        componentTypeMapper.toEntity(componentTypeId, createComponentTypeRequest)
                )
        );
    }

    @Override
    public void deleteComponentType(Long id) {
        log.info("Deleting component type with id {}", id);
        componentTypeService.deleteById(id);
    }

    @Override
    public ComponentType getComponentType(Long id) {
        log.info("Getting component type with id {}", id);
        return componentTypeMapper.toDto(
                componentTypeService.getById(id)
        );
    }

    @Override
    public List<ComponentType> getComponentTypesByDomainId(Long domainId) {
        return componentTypeMapper.toDtoList(
                componentTypeService.getByDomainId(domainId)
        );
    }
}
