package ru.sultanyarov.configurator.application.validator;

import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.DataType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ComponentValidatorImplTest {

    private final ComponentValidatorImpl validator = new ComponentValidatorImpl();

    @Test
    void validateCreation_shouldPassForValidComponent() {
        ComponentType componentType = componentType(
                List.of(
                        attributeDefinition(1L, "name", DataType.STRING, null, true),
                        attributeDefinition(2L, "force", DataType.NUMBER, null, false),
                        attributeDefinition(3L, "clicky", DataType.BOOLEAN, null, false),
                        attributeDefinition(4L, "stem", DataType.ENUM, Set.of("MX", "ALPS"), false)
                ),
                List.of(component("Existing", List.of()))
        );

        Component componentToCreate = component("New", List.of(
                attributeValue(1L, DataType.STRING, "value"),
                attributeValue(2L, DataType.NUMBER, "12.5"),
                attributeValue(3L, DataType.BOOLEAN, "true"),
                attributeValue(4L, DataType.ENUM, "MX")
        ));

        assertThatCode(() -> validator.validateCreation(componentToCreate, componentType, attributeMap(componentType.attributeDefinitions())))
                .doesNotThrowAnyException();
    }

    @Test
    void validateCreation_shouldRejectDuplicateComponentName() {
        ComponentType componentType = componentType(List.of(), List.of(component("Existing", List.of())));

        assertThatThrownBy(() -> validator.validateCreation(component("Existing", List.of()), componentType, attributeMap(componentType.attributeDefinitions())))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessageContaining("unique");
    }

    @Test
    void validateCreation_shouldRejectDuplicateAttributeIds() {
        ComponentType componentType = componentType(List.of(attributeDefinition(1L, "name", DataType.STRING, null, false)), List.of());
        Component componentToCreate = component("New", List.of(
                attributeValue(1L, DataType.STRING, "a"),
                attributeValue(1L, DataType.STRING, "b")
        ));

        assertThatThrownBy(() -> validator.validateCreation(componentToCreate, componentType, attributeMap(componentType.attributeDefinitions())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Repeated attribute ids");
    }

    @Test
    void validateCreation_shouldRejectInvalidAttributeIdForComponentType() {
        ComponentType componentType = componentType(List.of(attributeDefinition(1L, "name", DataType.STRING, null, false)), List.of());
        Component componentToCreate = component("New", List.of(attributeValue(999L, DataType.STRING, "a")));

        assertThatThrownBy(() -> validator.validateCreation(componentToCreate, componentType, attributeMap(componentType.attributeDefinitions())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Incorrect attributes");
    }

    @Test
    void validateCreation_shouldRejectMissingRequiredAttribute() {
        ComponentType componentType = componentType(List.of(attributeDefinition(1L, "name", DataType.STRING, null, true)), List.of());
        Component componentToCreate = component("New", List.of());

        assertThatThrownBy(() -> validator.validateCreation(componentToCreate, componentType, attributeMap(componentType.attributeDefinitions())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Required attribute");
    }

    @Test
    void validateCreation_shouldRejectNullStringValue() {
        ComponentType componentType = componentType(List.of(attributeDefinition(1L, "name", DataType.STRING, null, false)), List.of());

        assertThatThrownBy(() -> validator.validateCreation(component("New", List.of(attributeValue(1L, DataType.STRING, null))), componentType, attributeMap(componentType.attributeDefinitions())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Incorrect string value");
    }

    @Test
    void validateCreation_shouldRejectInvalidNumberValue() {
        ComponentType componentType = componentType(List.of(attributeDefinition(1L, "force", DataType.NUMBER, null, false)), List.of());

        assertThatThrownBy(() -> validator.validateCreation(component("New", List.of(attributeValue(1L, DataType.NUMBER, "not-a-number"))), componentType, attributeMap(componentType.attributeDefinitions())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Incorrect number value");
    }

    @Test
    void validateCreation_shouldRejectInvalidBooleanValue() {
        ComponentType componentType = componentType(List.of(attributeDefinition(1L, "clicky", DataType.BOOLEAN, null, false)), List.of());

        assertThatThrownBy(() -> validator.validateCreation(component("New", List.of(attributeValue(1L, DataType.BOOLEAN, "yes"))), componentType, attributeMap(componentType.attributeDefinitions())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Incorrect boolean value");
    }

    @Test
    void validateCreation_shouldRejectInvalidEnumValue() {
        ComponentType componentType = componentType(List.of(attributeDefinition(1L, "stem", DataType.ENUM, Set.of("MX"), false)), List.of());

        assertThatThrownBy(() -> validator.validateCreation(component("New", List.of(attributeValue(1L, DataType.ENUM, "ALPS"))), componentType, attributeMap(componentType.attributeDefinitions())))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Incorrect enum value");
    }

    private static Component component(String name, List<AttributeValue> attributes) {
        return Component.builder()
                .componentTypeId(1L)
                .name(name)
                .attributes(attributes)
                .build();
    }

    private static ComponentType componentType(List<AttributeDefinition> definitions, List<Component> components) {
        return ComponentType.builder()
                .id(1L)
                .attributeDefinitions(definitions)
                .components(components)
                .build();
    }

    private static AttributeDefinition attributeDefinition(Long id, String name, DataType dataType, Set<String> enumValues, boolean required) {
        return AttributeDefinition.builder()
                .id(id)
                .name(name)
                .dataType(dataType)
                .enumValues(enumValues)
                .isRequired(required)
                .build();
    }

    private static AttributeValue attributeValue(Long attributeDefinitionId, DataType dataType, String value) {
        return AttributeValue.builder()
                .attributeDefinitionId(attributeDefinitionId)
                .dataType(dataType)
                .value(value)
                .build();
    }

    private static Map<Long, AttributeDefinition> attributeMap(List<AttributeDefinition> definitions) {
        return definitions.stream().collect(java.util.stream.Collectors.toMap(AttributeDefinition::id, definition -> definition));
    }
}
