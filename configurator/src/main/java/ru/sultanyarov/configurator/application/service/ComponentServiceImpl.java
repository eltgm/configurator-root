package ru.sultanyarov.configurator.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.application.port.out.ComponentRepository;
import ru.sultanyarov.configurator.application.validator.ComponentValidator;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentServiceImpl implements ComponentService {
    private final ComponentRepository componentRepository;
    private final ComponentTypeService componentTypeService;
    private final AttributeValueService attributeValueService;
    private final ComponentValidator componentValidator;
    private final DomainService domainService;

    @Override
    public Component create(Component componentToCreate) {
        log.debug("create component {}", componentToCreate);
        Long componentTypeId = componentToCreate.getComponentTypeId();

        ComponentType componentType = componentTypeService.getById(componentTypeId);
        Map<Long, AttributeDefinition> componentTypeAttributesMap = getComponentAttributesDefinitionsMap(componentType);

        componentValidator.validateCreation(componentToCreate, componentType, componentTypeAttributesMap);

        List<AttributeValue> newComponentAttributes = enrichAttributes(componentToCreate.getAttributes(), componentTypeAttributesMap);
        componentToCreate.setAttributes(newComponentAttributes);

        var createdComponent = componentRepository.createComponent(componentToCreate)
                .orElseThrow(() -> new BusinessException("Failed to create component"));
        createdComponent.setAttributes(attributeValueService.createAttributeValues(newComponentAttributes, createdComponent.getId()));
        createdComponent.setImages(List.of());
        return createdComponent;
    }

    private Map<Long, AttributeDefinition> getComponentAttributesDefinitionsMap(ComponentType componentType) {
        List<AttributeDefinition> attributeDefinitions = componentType.attributeDefinitions() == null ? List.of() : componentType.attributeDefinitions();
        return attributeDefinitions
                .stream()
                .collect(
                        toMap(AttributeDefinition::id, attributeDefinition -> attributeDefinition)
                );
    }

    private List<AttributeValue> enrichAttributes(List<AttributeValue> attributes, Map<Long, AttributeDefinition> providedAttributeDefinitions) {
        return attributes.stream()
                .map(attributeValue -> {
                    AttributeDefinition attributeDefinition = providedAttributeDefinitions.get(attributeValue.attributeDefinitionId());
                    return AttributeValue.builder()
                            .id(attributeValue.id())
                            .attributeDefinitionId(attributeValue.attributeDefinitionId())
                            .name(attributeDefinition.name())
                            .label(attributeDefinition.label())
                            .dataType(attributeDefinition.dataType())
                            .value(attributeValue.value())
                            .build();
                })
                .toList();
    }

    @Override
    public Component update(Long id, Component component) {
        return null;
    }

    @Override
    public void deleteById(Long id) {

    }

    @Override
    public Component getById(Long id) {
        log.debug("get component by id {}", id);

        return componentRepository.getById(id)
                .orElseThrow(() -> new NotFoundException("Component with id {} not found", id));
    }

    @Override
    public Page<Component> getPage(int page, int pageSize) {
        return null;
    }

    @Override
    public Page<Component> getByPageByDomainId(Long domainId, Long componentTypeId, String name, Integer page, Integer size) {
        log.debug("get component by domain id {}, component type id {}", domainId, componentTypeId);
        Domain domain = domainService.getById(domainId);
        validateComponentType(componentTypeId, domain);

        return componentRepository.findPageByDomainIdComponentTypeIdName(domainId, componentTypeId, name, page, size);
    }

    private void validateComponentType(Long componentTypeId, Domain domain) {
        if (componentTypeId != null) {
            boolean isComponentTypeBelongsToDomain = domain.componentTypes()
                    .stream()
                    .anyMatch(componentType -> componentType.id().equals(componentTypeId));

            if (!isComponentTypeBelongsToDomain) {
                throw new ValidationException("Тип компонента не принадлежит указанному домену");
            }
        }
    }
}
