package ru.sultanyarov.configurator.application.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sultanyarov.configurator.application.port.out.ConfigurationRepository;
import ru.sultanyarov.configurator.application.port.out.ConfiguratorRepository;
import ru.sultanyarov.configurator.application.port.out.CurrentUserProvider;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.ConfigurationConflictException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Configuration;
import ru.sultanyarov.configurator.domain.model.ConfigurationComponent;
import ru.sultanyarov.configurator.domain.model.ConfigurationDraft;
import ru.sultanyarov.configurator.domain.model.ConfigurationExport;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigurationServiceImpl implements ConfigurationService {
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 10;
  private static final int MAX_SIZE = 100;
  private static final int MAX_COMPONENTS = 50;
  private static final int EXPORT_SCHEMA_VERSION = 1;

  private final DomainService domainService;
  private final ComponentService componentService;
  private final ConfiguratorRepository configuratorRepository;
  private final ConfigurationRepository configurationRepository;
  private final CurrentUserProvider currentUserProvider;
  private final ConfigurationCompatibilityValidator compatibilityValidator;

  @Override
  @Transactional
  public Configuration create(Long domainId, ConfigurationDraft draft) {
    log.info("Creating configuration in domain {}", domainId);
    ValidatedConfiguration validated = validateConfiguration(domainId, draft);
    Long currentUserId = currentUserProvider.getCurrentUserId();
    Configuration configuration =
        Configuration.builder()
            .domainId(domainId)
            .name(validated.name())
            .description(validated.description())
            .createdByUserId(currentUserId)
            .components(validated.components())
            .build();
    return configurationRepository
        .create(configuration)
        .orElseThrow(() -> new BusinessException("Failed to create configuration"));
  }

  @Override
  @Transactional
  public Configuration update(Long id, ConfigurationDraft draft) {
    log.info("Updating configuration {}", id);
    Long currentUserId = currentUserProvider.getCurrentUserId();
    Configuration existing = findOwnedConfiguration(id, currentUserId);
    ValidatedConfiguration validated = validateConfiguration(existing.domainId(), draft);
    Configuration configuration =
        Configuration.builder()
            .id(existing.id())
            .domainId(existing.domainId())
            .name(validated.name())
            .description(validated.description())
            .createdByUserId(existing.createdByUserId())
            .createdAt(existing.createdAt())
            .components(validated.components())
            .build();
    return configurationRepository
        .update(id, currentUserId, configuration)
        .orElseThrow(() -> new BusinessException("Failed to update configuration with id {}", id));
  }

  @Override
  @Transactional
  public void delete(Long id) {
    log.info("Deleting configuration {}", id);
    Long currentUserId = currentUserProvider.getCurrentUserId();
    if (!configurationRepository.deleteByIdAndUserId(id, currentUserId)) {
      throw new NotFoundException("Configuration with id {} not found", id);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Configuration> getPage(Long domainId, Integer page, Integer size) {
    int resolvedPage = page == null ? DEFAULT_PAGE : page;
    int resolvedSize = size == null ? DEFAULT_SIZE : size;
    validatePagination(resolvedPage, resolvedSize);
    domainService.getById(domainId);
    return configurationRepository.findPageByDomainIdAndUserId(
        domainId, currentUserProvider.getCurrentUserId(), resolvedPage, resolvedSize);
  }

  @Override
  @Transactional(readOnly = true)
  public Configuration getById(Long id) {
    return findOwnedConfiguration(id, currentUserProvider.getCurrentUserId());
  }

  @Override
  @Transactional(readOnly = true)
  public ConfigurationExport export(Long id) {
    return new ConfigurationExport(
        EXPORT_SCHEMA_VERSION, LocalDateTime.now(ZoneOffset.UTC), getById(id));
  }

  private Configuration findOwnedConfiguration(Long id, Long currentUserId) {
    return configurationRepository
        .findByIdAndUserId(id, currentUserId)
        .orElseThrow(() -> new NotFoundException("Configuration with id {} not found", id));
  }

  private ValidatedConfiguration validateConfiguration(Long domainId, ConfigurationDraft draft) {
    ConfigurationDraft normalizedDraft = normalizeAndValidateDraft(draft);
    Domain domain = domainService.getById(domainId);
    List<Component> selectedComponents =
        resolveActiveDomainComponents(domain, normalizedDraft.componentIds());
    validateDistinctComponentTypes(selectedComponents);
    compatibilityValidator.validatePairwiseDirectCompatibility(domainId, selectedComponents);
    return new ValidatedConfiguration(
        normalizedDraft.name(),
        normalizedDraft.description(),
        toConfigurationComponents(domain, selectedComponents));
  }

  private static ConfigurationDraft normalizeAndValidateDraft(ConfigurationDraft draft) {
    if (draft == null) {
      throw new ValidationException("Configuration request is required");
    }
    String name = draft.name() == null ? null : draft.name().trim();
    if (name == null || name.isEmpty()) {
      throw new ValidationException("Configuration name must not be blank");
    }
    String description = normalizeDescription(draft.description());
    validateComponentIds(draft.componentIds());
    return new ConfigurationDraft(name, description, List.copyOf(draft.componentIds()));
  }

  private static String normalizeDescription(String description) {
    if (description == null || description.isBlank()) {
      return null;
    }
    return description.trim();
  }

  private static void validateComponentIds(List<Long> componentIds) {
    if (componentIds == null || componentIds.isEmpty()) {
      throw new ValidationException("At least one component id is required");
    }
    if (componentIds.size() > MAX_COMPONENTS) {
      throw new ValidationException("No more than {} component ids are allowed", MAX_COMPONENTS);
    }
    if (componentIds.stream().anyMatch(id -> id == null || id <= 0)) {
      throw new ValidationException("Component identifiers must be positive");
    }
    if (new HashSet<>(componentIds).size() != componentIds.size()) {
      throw new ValidationException("Component identifiers must be unique");
    }
  }

  private List<Component> resolveActiveDomainComponents(Domain domain, List<Long> componentIds) {
    Map<Long, Component> activeById = new HashMap<>();
    for (Component component : configuratorRepository.getActiveComponents(domain.id())) {
      activeById.put(component.getId(), component);
    }
    Set<Long> domainTypeIds = new HashSet<>();
    if (domain.componentTypes() != null) {
      domain.componentTypes().stream().map(ComponentType::id).forEach(domainTypeIds::add);
    }

    List<Component> selected = new ArrayList<>(componentIds.size());
    for (Long componentId : componentIds) {
      Component component = activeById.get(componentId);
      if (component != null) {
        selected.add(component);
        continue;
      }
      Component unavailableComponent = componentService.getById(componentId);
      if (!domainTypeIds.contains(unavailableComponent.getComponentTypeId())) {
        throw new ValidationException(
            "Component with id {} does not belong to domain with id {}", componentId, domain.id());
      }
      if (Boolean.TRUE.equals(unavailableComponent.getArchived())) {
        throw new ConfigurationConflictException(
            "Archived component with id {} cannot be added to a configuration", componentId);
      }
      throw new ValidationException(
          "Component with id {} is unavailable for configuration", componentId);
    }
    return selected;
  }

  private static void validateDistinctComponentTypes(List<Component> components) {
    Set<Long> componentTypeIds = new HashSet<>();
    for (Component component : components) {
      if (!componentTypeIds.add(component.getComponentTypeId())) {
        throw new ConfigurationConflictException(
            "Only one component of type {} can be added to a configuration",
            component.getComponentTypeId());
      }
    }
  }

  private static List<ConfigurationComponent> toConfigurationComponents(
      Domain domain, List<Component> components) {
    Map<Long, String> typeNames = new LinkedHashMap<>();
    if (domain.componentTypes() != null) {
      for (ComponentType type : domain.componentTypes()) {
        typeNames.put(type.id(), type.name());
      }
    }
    return components.stream()
        .map(
            component ->
                ConfigurationComponent.builder()
                    .id(component.getId())
                    .name(component.getName())
                    .brand(component.getBrand())
                    .componentTypeId(component.getComponentTypeId())
                    .componentTypeName(typeNames.get(component.getComponentTypeId()))
                    .archived(Boolean.TRUE.equals(component.getArchived()))
                    .build())
        .toList();
  }

  private static void validatePagination(int page, int size) {
    if (page < 0) {
      throw new ValidationException("Page must be greater than or equal to 0");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new ValidationException("Size must be between 1 and {}", MAX_SIZE);
    }
  }

  private record ValidatedConfiguration(
      String name, String description, List<ConfigurationComponent> components) {}
}
