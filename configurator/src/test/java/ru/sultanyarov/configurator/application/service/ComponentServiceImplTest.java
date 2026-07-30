package ru.sultanyarov.configurator.application.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.mockito.*;
import org.mockito.junit.jupiter.*;
import ru.sultanyarov.configurator.application.port.out.*;
import ru.sultanyarov.configurator.application.validator.*;
import ru.sultanyarov.configurator.domain.exception.*;
import ru.sultanyarov.configurator.domain.model.*;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    void update_shouldReplaceEditableStateAndAttributesWhilePreservingImages() {
        ComponentImage image = ComponentImage.builder().id(30L).componentId(7L).url("/image.jpg").orderIndex(1).build();
        Component existingComponent = Component.builder()
                .id(7L)
                .componentTypeId(5L)
                .name("Existing")
                .archived(false)
                .images(List.of(image))
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
                .components(List.of(existingComponent))
                .build();
        Component componentToUpdate = Component.builder()
                .componentTypeId(5L)
                .name(" Updated ")
                .brand("Brand")
                .description("Description")
                .attributes(List.of(AttributeValue.builder().attributeDefinitionId(11L).value("55").build()))
                .build();
        Component persistedComponent = Component.builder()
                .id(7L)
                .componentTypeId(5L)
                .name("Updated")
                .brand("Brand")
                .description("Description")
                .archived(false)
                .build();
        List<AttributeValue> persistedAttributes = List.of(
                AttributeValue.builder()
                        .id(41L)
                        .attributeDefinitionId(11L)
                        .name("force")
                        .label("Force")
                        .dataType(DataType.NUMBER)
                        .value("55")
                        .build()
        );

        when(componentRepository.getById(7L)).thenReturn(Optional.of(existingComponent));
        when(componentTypeService.getById(5L)).thenReturn(componentType);
        when(componentRepository.updateComponent(7L, componentToUpdate)).thenReturn(Optional.of(persistedComponent));
        when(attributeValueService.replaceAttributeValues(anyList(), eq(7L))).thenReturn(persistedAttributes);

        Component result = componentService.update(7L, componentToUpdate);

        assertThat(componentToUpdate.getName()).isEqualTo("Updated");
        assertThat(result).isSameAs(persistedComponent);
        assertThat(result.getAttributes()).isEqualTo(persistedAttributes);
        assertThat(result.getImages()).containsExactly(image);
        verify(componentValidator).validateUpdate(
                eq(componentToUpdate),
                eq(existingComponent),
                eq(componentType),
                eq(Map.of(11L, attributeDefinition))
        );
        verify(componentRepository).updateComponent(7L, componentToUpdate);
        verify(attributeValueService).replaceAttributeValues(
                eq(List.of(AttributeValue.builder()
                        .attributeDefinitionId(11L)
                        .name("force")
                        .label("Force")
                        .dataType(DataType.NUMBER)
                        .value("55")
                        .build())),
                eq(7L)
        );
    }

    @Test
    void update_shouldRejectInvalidComponentBeforePersistence() {
        Component existingComponent = Component.builder().id(7L).componentTypeId(5L).name("Existing").build();
        Component componentToUpdate = Component.builder().componentTypeId(6L).name("Updated").attributes(List.of()).build();
        ComponentType componentType = ComponentType.builder().id(5L).attributeDefinitions(List.of()).build();

        when(componentRepository.getById(7L)).thenReturn(Optional.of(existingComponent));
        when(componentTypeService.getById(5L)).thenReturn(componentType);
        doThrow(new ValidationException("Changing component type is not supported"))
                .when(componentValidator)
                .validateUpdate(componentToUpdate, existingComponent, componentType, Map.of());

        assertThatThrownBy(() -> componentService.update(7L, componentToUpdate))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Changing component type");

        verify(componentRepository, never()).updateComponent(any(), any());
        verifyNoInteractions(attributeValueService);
    }

    @Test
    void update_shouldThrowBusinessExceptionWhenRepositoryDidNotUpdateComponent() {
        Component existingComponent = Component.builder().id(7L).componentTypeId(5L).name("Existing").build();
        Component componentToUpdate = Component.builder().componentTypeId(5L).name("Updated").attributes(List.of()).build();
        ComponentType componentType = ComponentType.builder().id(5L).attributeDefinitions(List.of()).build();

        when(componentRepository.getById(7L)).thenReturn(Optional.of(existingComponent));
        when(componentTypeService.getById(5L)).thenReturn(componentType);
        when(componentRepository.updateComponent(7L, componentToUpdate)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> componentService.update(7L, componentToUpdate))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Failed to update component");

        verifyNoInteractions(attributeValueService);
    }

    @Test
    void getById_shouldReturnComponentFromRepository() {
        Component component = Component.builder().id(7L).build();
        when(componentRepository.getById(7L)).thenReturn(Optional.of(component));

        assertThat(componentService.getById(7L)).isSameAs(component);
    }

    @Test
    void getById_shouldThrowNotFoundExceptionWhenComponentDoesNotExist() {
        when(componentRepository.getById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> componentService.getById(7L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("7");
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
    void unimplementedMethods_shouldReturnNullOrDoNothing() {
        componentService.deleteById(1L);
        assertThat(componentService.getPage(0, 10)).isNull();
    }
}
