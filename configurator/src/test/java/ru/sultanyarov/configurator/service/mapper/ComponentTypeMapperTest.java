package ru.sultanyarov.configurator.service.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sultanyarov.configurator.application.mapper.ComponentTypeMapper;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentTypeRequest;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.test.data.ComponentTypeTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentTypeMapperTest {

    private final ComponentTypeMapper componentTypeMapper = Mappers.getMapper(ComponentTypeMapper.class);

    @Test
    void toDto_shouldMapCorrectly_whenComponentTypeIsNotNull() {
        // Arrange
        ComponentType componentType = ComponentTypeTestData.componentTypeWithId(1L);

        // Act
        ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentType result = componentTypeMapper.toDto(componentType);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(ct -> {
                    assertThat(ct.getId()).isEqualTo(componentType.id());
                    assertThat(ct.getDomainId()).isEqualTo(componentType.domainId());
                    assertThat(ct.getName()).isEqualTo(componentType.name());
                    assertThat(ct.getCode()).isEqualTo(componentType.code());
                    assertThat(ct.getDescription()).isEqualTo(componentType.description());
                    assertThat(ct.getOrderIndex()).isEqualTo(componentType.orderIndex());
                });
    }

    @Test
    void toDto_shouldReturnNull_whenComponentTypeIsNull() {
        // Arrange
        ComponentType componentType = null;

        // Act
        ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentType result = componentTypeMapper.toDto(componentType);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void toDtoList_shouldMapCorrectly_whenComponentTypeListIsNotEmpty() {
        // Arrange
        List<ComponentType> componentTypes = List.of(
                ComponentTypeTestData.componentTypeWithId(1L),
                ComponentTypeTestData.componentTypeWithId(2L)
        );

        // Act
        List<ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentType> result = componentTypeMapper.toDtoList(componentTypes);

        // Assert
        assertThat(result)
                .isNotNull()
                .hasSize(2)
                .satisfies(list -> {
                    assertThat(list.get(0).getId()).isEqualTo(componentTypes.get(0).id());
                    assertThat(list.get(0).getDomainId()).isEqualTo(componentTypes.get(0).domainId());
                    assertThat(list.get(0).getName()).isEqualTo(componentTypes.get(0).name());
                    assertThat(list.get(1).getId()).isEqualTo(componentTypes.get(1).id());
                    assertThat(list.get(1).getDomainId()).isEqualTo(componentTypes.get(1).domainId());
                    assertThat(list.get(1).getName()).isEqualTo(componentTypes.get(1).name());
                });
    }

    @Test
    void toDtoList_shouldReturnEmptyList_whenComponentTypeListIsEmpty() {
        // Arrange
        List<ComponentType> componentTypes = List.of();

        // Act
        List<ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentType> result = componentTypeMapper.toDtoList(componentTypes);

        // Assert
        assertThat(result)
                .isNotNull()
                .isEmpty();
    }

    @Test
    void toDtoList_shouldReturnNull_whenComponentTypeListIsNull() {
        // Arrange
        List<ComponentType> componentTypes = null;

        // Act
        List<ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentType> result = componentTypeMapper.toDtoList(componentTypes);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void toEntityWithDomain_shouldMapCorrectly_whenRequestIsNotNull() {
        // Arrange
        Long domainId = 1L;
        CreateComponentTypeRequest request = createCreateComponentTypeRequest("new-component-type", "NEW_CODE", "new description", 1);

        // Act
        ComponentType result = componentTypeMapper.toEntityWithDomain(domainId, request);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(ct -> {
                    assertThat(ct.id()).isNull();
                    assertThat(ct.domainId()).isEqualTo(domainId);
                    assertThat(ct.name()).isEqualTo(request.getName());
                    assertThat(ct.code()).isEqualTo(request.getCode());
                    assertThat(ct.description()).isEqualTo(request.getDescription());
                    assertThat(ct.orderIndex()).isEqualTo(request.getOrderIndex());
                    assertThat(ct.domain()).isNull();
                    assertThat(ct.createdAt()).isNotNull();
                });
    }

    @Test
    void toEntity_shouldMapCorrectly_whenRequestIsNotNull() {
        // Arrange
        Long id = 1L;
        CreateComponentTypeRequest request = createCreateComponentTypeRequest("updated-component-type", "UPDATED_CODE", "updated description", 2);

        // Act
        ComponentType result = componentTypeMapper.toEntity(id, request);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(ct -> {
                    assertThat(ct.id()).isEqualTo(id);
                    assertThat(ct.domainId()).isNull();
                    assertThat(ct.name()).isEqualTo(request.getName());
                    assertThat(ct.code()).isEqualTo(request.getCode());
                    assertThat(ct.description()).isEqualTo(request.getDescription());
                    assertThat(ct.orderIndex()).isEqualTo(request.getOrderIndex());
                    assertThat(ct.domain()).isNull();
                    assertThat(ct.createdAt()).isNotNull();
                });
    }

    private CreateComponentTypeRequest createCreateComponentTypeRequest(String name, String code, String description, Integer orderIndex) {
        CreateComponentTypeRequest request = new CreateComponentTypeRequest();
        request.setName(name);
        request.setCode(code);
        request.setDescription(description);
        request.setOrderIndex(orderIndex);
        return request;
    }
}
