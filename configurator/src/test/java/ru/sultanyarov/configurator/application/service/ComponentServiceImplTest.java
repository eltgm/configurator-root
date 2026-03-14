package ru.sultanyarov.configurator.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.port.out.ComponentRepository;
import ru.sultanyarov.configurator.application.validator.ComponentValidator;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.model.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
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
    void unimplementedMethods_shouldReturnNullOrDoNothing() {
        assertThat(componentService.update(1L, new Component())).isNull();
        componentService.deleteById(1L);
        assertThat(componentService.getById(1L)).isNull();
        assertThat(componentService.getPage(0, 10)).isNull();
    }
}
