package ru.sultanyarov.configurator.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeValueInput;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateComponentRequest;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.Page;

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
  void toEntity_shouldMapUpdateRequestToDomainEntity() {
    UpdateComponentRequest request =
        new UpdateComponentRequest(10L, "Switch Pro", List.of(new AttributeValueInput(1L, "55")));
    request.setBrand("Gateron");
    request.setDescription("Updated");

    Component result = mapper.toEntity(request);

    assertThat(result.getId()).isNull();
    assertThat(result.getComponentTypeId()).isEqualTo(10L);
    assertThat(result.getName()).isEqualTo("Switch Pro");
    assertThat(result.getBrand()).isEqualTo("Gateron");
    assertThat(result.getDescription()).isEqualTo("Updated");
    assertThat(result.getArchived()).isNull();
    assertThat(result.getCreatedAt()).isNull();
    assertThat(result.getImages()).isNull();
    assertThat(result.getAttributes())
        .singleElement()
        .satisfies(
            attribute -> {
              assertThat(attribute.attributeDefinitionId()).isEqualTo(1L);
              assertThat(attribute.value()).isEqualTo("55");
            });
  }

  @Test
  void toDto_shouldMapDomainEntityToDto() {
    Component component =
        Component.builder()
            .id(1L)
            .componentTypeId(10L)
            .name("Switch")
            .brand("Gateron")
            .description("Yellow")
            .archived(false)
            .createdAt(LocalDateTime.now())
            .attributes(
                List.of(
                    ru.sultanyarov.configurator.domain.model.AttributeValue.builder()
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

  @Test
  void toDto_shouldExposeBackendContentUrlForComponentImage() {
    ru.sultanyarov.configurator.domain.model.ComponentImage image =
        ru.sultanyarov.configurator.domain.model.ComponentImage.builder()
            .id(42L)
            .componentId(7L)
            .objectKey("components/7/image.png")
            .orderIndex(3)
            .build();

    ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage result = mapper.toDto(image);

    assertThat(result.getId()).isEqualTo(42L);
    assertThat(result.getUrl()).isEqualTo("/component-images/42/content");
    assertThat(result.getOrderIndex()).isEqualTo(3);
  }

  @Test
  void toComponentPageDto_shouldMapComponentsAndPaginationInformation() {
    Component component =
        Component.builder()
            .id(1L)
            .componentTypeId(10L)
            .name("Switch")
            .archived(false)
            .createdAt(LocalDateTime.now())
            .build();
    Page<Component> page = new Page<>(List.of(component), 2, 5, 11);

    ComponentPage result = mapper.toComponentPageDto(page);

    assertThat(result.getPage()).isEqualTo(2);
    assertThat(result.getSize()).isEqualTo(5);
    assertThat(result.getTotalItems()).isEqualTo(11);
    assertThat(result.getItems())
        .singleElement()
        .satisfies(
            item -> {
              assertThat(item.getId()).isEqualTo(1L);
              assertThat(item.getComponentTypeId()).isEqualTo(10L);
              assertThat(item.getName()).isEqualTo("Switch");
            });
  }
}
