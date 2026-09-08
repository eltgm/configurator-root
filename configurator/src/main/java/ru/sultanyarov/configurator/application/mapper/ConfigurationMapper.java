package ru.sultanyarov.configurator.application.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationComponentInput;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateConfigurationRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.SavedConfiguration;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateConfigurationRequest;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.ConfigurationComponentItem;
import ru.sultanyarov.configurator.domain.model.ConfigurationDraft;
import ru.sultanyarov.configurator.domain.model.Page;

@Mapper(componentModel = "spring")
public interface ConfigurationMapper {
  default ConfigurationDraft toDomain(CreateConfigurationRequest request) {
    return toDraft(
        request.getName(),
        request.getDescription(),
        request.getTrackInventory(),
        request.getComponents(),
        request.getComponentIds());
  }

  default ConfigurationDraft toDomain(UpdateConfigurationRequest request) {
    return toDraft(
        request.getName(),
        request.getDescription(),
        request.getTrackInventory(),
        request.getComponents(),
        request.getComponentIds());
  }

  SavedConfiguration toDto(ru.sultanyarov.configurator.domain.model.Configuration configuration);

  ConfigurationPage toDto(
      Page<ru.sultanyarov.configurator.domain.model.Configuration> configurations);

  @Mapping(target = "_configuration", source = "configuration")
  ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfigurationExport toDto(
      ru.sultanyarov.configurator.domain.model.ConfigurationExport export);

  private static ConfigurationDraft toDraft(
      String name,
      String description,
      Boolean trackInventory,
      List<ConfigurationComponentInput> components,
      List<Long> componentIds) {
    List<ConfigurationComponentInput> requestedComponents =
        components == null || components.isEmpty() ? null : components;
    List<Long> deprecatedComponentIds =
        componentIds == null || componentIds.isEmpty() ? null : componentIds;
    if (requestedComponents != null && deprecatedComponentIds != null) {
      throw new ValidationException("Use either components or deprecated componentIds, not both");
    }
    List<ConfigurationComponentItem> items =
        requestedComponents != null
            ? requestedComponents.stream()
                .map(
                    item ->
                        new ConfigurationComponentItem(item.getComponentId(), item.getQuantity()))
                .toList()
            : deprecatedComponentIds == null
                ? null
                : deprecatedComponentIds.stream()
                    .map(id -> new ConfigurationComponentItem(id, 1))
                    .toList();
    return new ConfigurationDraft(name, description, trackInventory, items);
  }
}
