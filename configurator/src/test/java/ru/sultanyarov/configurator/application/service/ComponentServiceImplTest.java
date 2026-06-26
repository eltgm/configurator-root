package ru.sultanyarov.configurator.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.port.out.ComponentRepository;
import ru.sultanyarov.configurator.application.validator.ComponentValidator;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.DataType;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentServiceImplTest {

    @Mock
    private ComponentRepository componentRepository;

    @Mock
    private ComponentTypeService componentTypeService;

    @Mock
    private AttributeValueService attributeValueService;

    @Mock
    private ComponentValidator componentValidator;

    @Mock
    private DomainService domainService;

    @InjectMocks
    private ComponentServiceImpl componentService;

    @Test
    void create_shouldValidateCreateComponentPersistItAndAttachCreatedAttributes() {
        AttributeValue attributeValue = AttributeValue.builder().attributeDefinitionId(11L).value("42").build();
        Component componentToCreate = Component.builder()
                .componentTypeId(5L)
                .name("Component")
                .attributes(List.of(attributeValue))
                .build();
        AttributeDefinition attributeDefinition = AttributeDefinition.builder()
                .id(11L)
                .componentTypeId(5L)
                .name("force")
                .label("Force")
                .dataType(DataType.NUMBER)
                .build();
        ComponentType componentType = ComponentType.builder()
                .id(5L)
                .attributeDefinitions(List.of(attributeDefinition))
                .build();
        Component createdComponent = Component.builder().id(100L).componentTypeId(5L).name("Component").build();
        List<AttributeValue> createdAttributeValues = List.of(AttributeValue.builder().id(1L).attributeDefinitionId(11L).value("42").build());
        Map<Long, AttributeDefinition> attributeDefinitionsMap = Map.of(11L, attributeDefinition);

        when(componentTypeService.getById(5L)).thenReturn(componentType);
        when(componentRepository.createComponent(any(Component.class))).thenReturn(Optional.of(createdComponent));
        when(attributeValueService.createAttributeValues(anyList(), eq(100L))).thenReturn(createdAttributeValues);

        Component result = componentService.create(componentToCreate);

        assertThat(result).isSameAs(createdComponent);
        assertThat(result.getAttributes()).isEqualTo(createdAttributeValues);
        assertThat(result.getImages()).isEmpty();
        verify(componentTypeService).getById(5L);
        verify(componentValidator).validateCreation(componentToCreate, componentType, attributeDefinitionsMap);
        verify(componentRepository).createComponent(any(Component.class));
        verify(attributeValueService).createAttributeValues(anyList(), eq(100L));
    }

    @Test
    void create_shouldThrowBusinessExceptionWhenRepositoryDidNotCreateComponent() {
        Component componentToCreate = Component.builder()
                .componentTypeId(5L)
                .name("Component")
                .attributes(List.of())
                .build();

        when(componentTypeService.getById(5L)).thenReturn(ComponentType.builder().id(5L).attributeDefinitions(List.of()).build());
        when(componentRepository.createComponent(any(Component.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> componentService.create(componentToCreate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Failed to create component");
    }

    @Test
    void getByPageByDomainId_shouldDelegateSearchWithoutComponentTypeFilter() {
        Domain domain = Domain.builder()
                .id(1L)
                .componentTypes(List.of())
                .build();
        Page<Component> page = new Page<>(List.of(), 0, 10, 0);

        when(domainService.getById(1L)).thenReturn(domain);
        when(componentRepository.findPageByDomainIdComponentTypeIdName(1L, null, "name", 0, 10))
                .thenReturn(page);

        Page<Component> result = componentService.getByPageByDomainId(1L, null, "name", 0, 10);

        assertThat(result).isSameAs(page);
        verify(domainService).getById(1L);
        verify(componentRepository).findPageByDomainIdComponentTypeIdName(1L, null, "name", 0, 10);
    }

    @Test
    void getByPageByDomainId_shouldDelegateSearchWhenComponentTypeBelongsToDomain() {
        Domain domain = Domain.builder()
                .id(1L)
                .componentTypes(List.of(ComponentType.builder().id(2L).domainId(1L).build()))
                .build();
        Page<Component> page = new Page<>(List.of(), 1, 5, 0);

        when(domainService.getById(1L)).thenReturn(domain);
        when(componentRepository.findPageByDomainIdComponentTypeIdName(1L, 2L, null, 1, 5))
                .thenReturn(page);

        Page<Component> result = componentService.getByPageByDomainId(1L, 2L, null, 1, 5);

        assertThat(result).isSameAs(page);
        verify(componentRepository).findPageByDomainIdComponentTypeIdName(1L, 2L, null, 1, 5);
    }

    @Test
    void getByPageByDomainId_shouldRejectComponentTypeFromAnotherDomain() {
        Domain domain = Domain.builder()
                .id(1L)
                .componentTypes(List.of(ComponentType.builder().id(3L).domainId(1L).build()))
                .build();

        when(domainService.getById(1L)).thenReturn(domain);

        assertThatThrownBy(() -> componentService.getByPageByDomainId(1L, 2L, null, 0, 10))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Тип компонента не принадлежит указанному домену");

        verifyNoInteractions(componentRepository);
    }

    @Test
    void getById_shouldReturnComponentWhenItExists() {
        Component component = Component.builder().id(1L).name("Component").build();

        when(componentRepository.getById(1L)).thenReturn(Optional.of(component));

        Component result = componentService.getById(1L);

        assertThat(result).isSameAs(component);
        verify(componentRepository).getById(1L);
    }

    @Test
    void getById_shouldThrowNotFoundExceptionWhenComponentDoesNotExist() {
        when(componentRepository.getById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> componentService.getById(1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Component with id 1 not found");

        verify(componentRepository).getById(1L);
    }

    @Test
    void unimplementedMethods_shouldReturnNullOrDoNothing() {
        assertThat(componentService.update(1L, new Component())).isNull();
        componentService.deleteById(1L);
        assertThat(componentService.getPage(0, 10)).isNull();
    }
}
