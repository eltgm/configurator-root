package ru.sultanyarov.configurator.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Page;
import ru.sultanyarov.configurator.domain.repository.ComponentRepository;
import ru.sultanyarov.configurator.service.ComponentValidator;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentServiceImpl implements ComponentService {
    private final ComponentRepository componentRepository;
    private final ComponentTypeService componentTypeService;
    private final AttributeValueService attributeValueService;
    private final ComponentValidator componentValidator;

    @Override
    public Component create(Component componentToCreate) {
        log.debug("create component {}", componentToCreate);
        Long componentTypeId = componentToCreate.getComponentTypeId();
        ComponentType componentType = componentTypeService.getById(componentTypeId); // 1
        componentValidator.validateCreation(componentToCreate, componentType);

        List<AttributeValue> newComponentAttributes = componentToCreate.getAttributes();

        //4
        var createdComponent = componentRepository.createComponent(componentToCreate)
                .orElseThrow(() -> new BusinessException("Failed to create component"));
        //5
        createdComponent.setAttributes(attributeValueService.createAttributeValues(newComponentAttributes, createdComponent.getId()));
        return createdComponent;
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
        return null;
    }

    @Override
    public Page<Component> getPage(int page, int pageSize) {
        return null;
    }
}
