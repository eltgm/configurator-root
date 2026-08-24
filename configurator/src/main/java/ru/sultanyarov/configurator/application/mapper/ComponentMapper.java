package ru.sultanyarov.configurator.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.AttributeValueInput;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateComponentRequest;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.Page;

@Mapper(componentModel = "spring")
public interface ComponentMapper {

  @Mapping(target = "images", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "archived", ignore = true)
  Component toEntity(CreateComponentRequest createComponentRequest);

  @Mapping(target = "images", ignore = true)
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "archived", ignore = true)
  Component toEntity(UpdateComponentRequest updateComponentRequest);

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "name", ignore = true)
  @Mapping(target = "label", ignore = true)
  @Mapping(target = "dataType", ignore = true)
  ru.sultanyarov.configurator.domain.model.AttributeValue toEntity(
      AttributeValueInput attributeValueInput);

  ru.sultanyarov.configurator.api.inbounds.rest.dto.Component toDto(Component component);

  @Mapping(target = "url", expression = "java(componentImageContentUrl(componentImage.id()))")
  ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentImage toDto(
      ru.sultanyarov.configurator.domain.model.ComponentImage componentImage);

  default String componentImageContentUrl(Long imageId) {
    return imageId == null ? null : "/component-images/%d/content".formatted(imageId);
  }

  /**
   * Converts a domain page of components to its transport-layer representation.
   *
   * @param byPageByDomainId the page of domain components to convert
   * @return the component page DTO containing mapped components and pagination information
   */
  ComponentPage toComponentPageDto(Page<Component> byPageByDomainId);
}
