package ru.sultanyarov.configurator.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.EntityHasRelatedEntitiesException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.repository.AttributeRepository;
import ru.sultanyarov.configurator.domain.repository.ComponentTypeRepository;
import ru.sultanyarov.configurator.domain.repository.DomainRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentTypeServiceImpl implements ComponentTypeService {
    private final ComponentTypeRepository componentTypeRepository;
    private final DomainRepository domainRepository;
    //private final ComponentRepository componentRepository;
    private final AttributeRepository attributesRepository;

    @Override
    public ComponentType create(ComponentType componentType) {
        log.debug("Creating component type: {}", componentType);

        Long domainId = componentType.domainId();
        Domain domainById = domainRepository.getDomainById(domainId)
                .orElseThrow(() -> new NotFoundException("Domain with id {} does not exist", domainId));
        validateNameUniqueInDomain(componentType, domainById);

        return componentTypeRepository.createComponentType(componentType)
                .orElseThrow(() -> new BusinessException("Failed to create component type"));
    }

    @Override
    public ComponentType update(Long id, ComponentType updatedComponentType) {
        log.debug("Updating component type: {}", updatedComponentType);

        ComponentType existedComponentType = getById(id);

        if (!existedComponentType.name().equals(updatedComponentType.name())) {
            Long domainId = existedComponentType.domainId();
            Domain domainById = domainRepository.getDomainById(domainId)
                    .orElseThrow(() -> new NotFoundException("Domain with id {} does not exist", domainId));
            validateNameUniqueInDomain(updatedComponentType, domainById);
        }

        return componentTypeRepository.updateComponentType(id, updatedComponentType)
                .orElseThrow(() -> new BusinessException("Failed to update component type with id {}", id));
    }

    private void validateNameUniqueInDomain(ComponentType componentType, Domain domainById) {
        domainById.componentTypes()
                .stream()
                .filter(componentTypeExisted -> componentTypeExisted.name().equals(componentType.name()))
                .findAny()
                .ifPresent(componentTypeExisted -> {
                            throw new EntityAlreadyExistsException("ComponentType with name {} already exists", componentType.name());
                        }
                );
    }

    private void validateIsNotExists(Long id) {
        if (!componentTypeRepository.existsById(id)) {
            throw new NotFoundException("ComponentType with id {} does not exist", id);
        }
    }

    @Override
    public void deleteById(Long id) {
        log.debug("Deleting component type with id: {}", id);
        validateIsNotExists(id);
        validateHasNotRelatedEntities(id);

        componentTypeRepository.deleteComponentTypeById(id);
    }

    private void validateHasNotRelatedEntities(Long id) {
        if (attributesRepository.hasByComponentTypeId(id)) {//|| componentRepository.hasByComponentyTypeId(id)) { //TODO after resolving con1-36
            throw new EntityHasRelatedEntitiesException("Cannot delete component type with id {} because it has related entities", id);
        }
    }

    @Override
    public ComponentType getById(Long id) {
        log.debug("Getting component type with id: {}", id);
        return componentTypeRepository.getComponentTypeById(id)
                .orElseThrow(() -> new NotFoundException("ComponentType with id {} does not exist", id));
    }

    @Override
    public List<ComponentType> getByDomainId(Long domainId) {
        log.debug("Getting component types by domain id: {}", domainId);
        validateIsDomainExists(domainId);

        return componentTypeRepository.getComponentTypesByDomainId(domainId);
    }

    private void validateIsDomainExists(Long domainId) {
        if (!domainRepository.existsById(domainId)) {
            throw new NotFoundException("Domain with id {} does not exist", domainId);
        }
    }
}
