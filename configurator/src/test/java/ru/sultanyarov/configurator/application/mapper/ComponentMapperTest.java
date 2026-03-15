package ru.sultanyarov.configurator.application.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeValueInput;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.domain.model.Component;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentMapperTest {

    private final ComponentMapper mapper = Mappers.getMapper(ComponentMapper.class);

    @Test
    void toEntity_shouldMapCreateRequestToDomainEntity() {
        CreateComponentRequest request = new CreateComponentRequest();
        request.setComponentTypeId(10L);
        request.setName("Switch");
        request.setBrand("Gateron");
        request.setDescription("Yellow");
        request.setAttributes(List.of(new AttributeValueInput(1L, "42")));

        Component result = mapper.toEntity(request);

        assertThat(result.getId()).isNull();
        assertThat(result.getComponentTypeId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Switch");
        assertThat(result.getBrand()).isEqualTo("Gateron");
        assertThat(result.getDescription()).isEqualTo("Yellow");
        assertThat(result.getArchived()).isNull();
        assertThat(result.getCreatedAt()).isNull();
        assertThat(result.getImages()).isNull();
        assertThat(result.getAttributes()).hasSize(1);
        assertThat(result.getAttributes().get(0).attributeDefinitionId()).isEqualTo(1L);
        assertThat(result.getAttributes().get(0).value()).isEqualTo("42");
    }

    @Test
    void toDto_shouldMapDomainEntityToDto() {
        Component component = Component.builder()
                .id(1L)
                .componentTypeId(10L)
                .name("Switch")
                .brand("Gateron")
                .description("Yellow")
                .archived(false)
                .createdAt(LocalDateTime.now())
                .attributes(List.of(ru.sultanyarov.configurator.domain.model.AttributeValue.builder()
                        .id(100L)
                        .attributeDefinitionId(1L)
                        .name("force")
                        .label("Force")
                        .value("42")
                        .build()))
                .images(List.of())
                .build();

        ru.sultanyarov.configurator.api.inbounds.rest.dto.Component dto = mapper.toDto(component);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getComponentTypeId()).isEqualTo(10L);
        assertThat(dto.getName()).isEqualTo("Switch");
        assertThat(dto.getBrand()).isEqualTo("Gateron");
        assertThat(dto.getDescription()).isEqualTo("Yellow");
        assertThat(dto.getArchived()).isFalse();
        assertThat(dto.getCreatedAt()).isEqualTo(component.getCreatedAt());
        assertThat(dto.getAttributes()).hasSize(1);
        assertThat(dto.getAttributes().get(0).getAttributeDefinitionId()).isEqualTo(1L);
        assertThat(dto.getAttributes().get(0).getName()).isEqualTo("force");
        assertThat(dto.getAttributes().get(0).getLabel()).isEqualTo("Force");
        assertThat(dto.getAttributes().get(0).getValue()).isEqualTo("42");
    }
}
