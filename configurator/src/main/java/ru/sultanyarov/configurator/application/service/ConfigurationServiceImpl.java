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
import ru.sultanyarov.configurator.application.port.out.ComponentRepository;
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
import ru.sultanyarov.configurator.domain.model.ConfigurationComponentItem;
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
  private static final int MAX_COMPONENT_QUANTITY = 999_999;
  private static final int EXPORT_SCHEMA_VERSION = 2;

  private final DomainService domainService;
  private final ComponentService componentService;
  private final ComponentRepository componentRepository;
  private final ConfiguratorRepository configuratorRepository;
  private final ConfigurationRepository configurationRepository;
  private final CurrentUserProvider currentUserProvider;
  private final ConfigurationCompatibilityValidator compatibilityValidator;

  @Override
  @Transactional
  public Configuration create(Long domainId, ConfigurationDraft draft) {
    log.info("Creating configuration in domain {}", domainId);
    NormalizedDraft normalizedDraft = normalizeAndValidateDraft(draft, false);
    Domain domain = domainService.getById(domainId);
    lockComponentIds(normalizedDraft.componentIds());
    ValidatedConfiguration validated = validateConfiguration(domain, normalizedDraft);
    validateInventoryAvailability(validated, null);
    Long currentUserId = currentUserProvider.getCurrentUserId();
    Configuration configuration =
        Configuration.builder()
            .domainId(domainId)
            .name(validated.name())
            .description(validated.description())
            .createdByUserId(currentUserId)
            .trackInventory(validated.trackInventory())
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
    Configuration existing = findOwnedConfigurationForUpdate(id, currentUserId);
    NormalizedDraft normalizedDraft = normalizeAndValidateDraft(draft, existing.trackInventory());
    lockComponentIds(unionComponentIds(existing.components(), normalizedDraft.components()));
    Domain domain = domainService.getById(existing.domainId());
    ValidatedConfiguration validated = validateConfiguration(domain, normalizedDraft);
    validateInventoryAvailability(validated, existing);
    Configuration configuration =
        Configuration.builder()
            .id(existing.id())
            .domainId(existing.domainId())
            .name(validated.name())
            .description(validated.description())
            .createdByUserId(existing.createdByUserId())
            .createdAt(existing.createdAt())
            .trackInventory(validated.trackInventory())
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
    Configuration existing = findOwnedConfigurationForUpdate(id, currentUserId);
    lockComponentIds(existing.components().stream().map(ConfigurationComponent::id).toList());
    if (!configurationRepository.deleteByIdAndUserId(id, currentUserId)) {
      throw new BusinessException("Failed to delete configuration with id {}", id);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public Page<Configuration> getPage(
      Long domainId, Integer page, Integer size, Boolean trackInventory) {
    int resolvedPage = page == null ? DEFAULT_PAGE : page;
    int resolvedSize = size == null ? DEFAULT_SIZE : size;
    validatePagination(resolvedPage, resolvedSize);
    domainService.getById(domainId);
    return configurationRepository.findPageByDomainIdAndUserId(
        domainId,
        currentUserProvider.getCurrentUserId(),
        trackInventory,
        resolvedPage,
        resolvedSize);
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

  private Configuration findOwnedConfigurationForUpdate(Long id, Long currentUserId) {
    return configurationRepository
        .findByIdAndUserIdForUpdate(id, currentUserId)
        .orElseThrow(() -> new NotFoundException("Configuration with id {} not found", id));
  }

  private ValidatedConfiguration validateConfiguration(Domain domain, NormalizedDraft draft) {
    List<Component> selectedComponents =
        resolveActiveDomainComponents(domain, draft.componentIds());
    compatibilityValidator.validateAssemblyCompatibility(domain.id(), selectedComponents);
    return new ValidatedConfiguration(
        draft.name(),
        draft.description(),
        draft.trackInventory(),
        toConfigurationComponents(domain, selectedComponents, draft.components()),
        draft.components());
  }

  private void validateInventoryAvailability(
      ValidatedConfiguration target, Configuration existingConfiguration) {
    if (!target.trackInventory()) {
      return;
    }
    Map<Long, Integer> allocatedByComponent =
        configurationRepository.findAllocatedQuantitiesByComponentIds(target.componentIds());
    Map<Long, Integer> occupiedByExisting =
        existingConfiguration != null && existingConfiguration.trackInventory()
            ? quantitiesByComponent(existingConfiguration.components())
            : Map.of();

    for (ConfigurationComponentItem item : target.items()) {
      Component component =
          componentRepository
              .getById(item.componentId())
              .orElseThrow(
                  () ->
                      new NotFoundException("Component with id {} not found", item.componentId()));
      int total = component.getTotalQuantity() == null ? 0 : component.getTotalQuantity();
      int allocated = allocatedByComponent.getOrDefault(item.componentId(), 0);
      int occupiedBySelf = occupiedByExisting.getOrDefault(item.componentId(), 0);
      int availableForTarget = total - allocated + occupiedBySelf;
      if (item.quantity() > availableForTarget) {
        throw new ConfigurationConflictException(
            "Component with id {} requires {} instances, but only {} are available",
            item.componentId(),
            item.quantity(),
            Math.max(availableForTarget, 0));
      }
    }
  }

  private static Map<Long, Integer> quantitiesByComponent(List<ConfigurationComponent> components) {
    Map<Long, Integer> quantities = new HashMap<>();
    for (ConfigurationComponent component : components) {
      quantities.put(component.id(), component.quantity());
    }
    return quantities;
  }

  private static NormalizedDraft normalizeAndValidateDraft(
      ConfigurationDraft draft, boolean defaultTrackInventory) {
    if (draft == null) {
      throw new ValidationException("Configuration request is required");
    }
    String name = draft.name() == null ? null : draft.name().trim();
    if (name == null || name.isEmpty()) {
      throw new ValidationException("Configuration name must not be blank");
    }
    String description = normalizeDescription(draft.description());
    List<ConfigurationComponentItem> components = validateAndCopyComponents(draft.components());
    return new NormalizedDraft(
        name,
        description,
        draft.trackInventory() == null ? defaultTrackInventory : draft.trackInventory(),
        components);
  }

  private static String normalizeDescription(String description) {
    if (description == null || description.isBlank()) {
      return null;
    }
    return description.trim();
  }

  private static List<ConfigurationComponentItem> validateAndCopyComponents(
      List<ConfigurationComponentItem> components) {
    if (components == null || components.isEmpty()) {
      throw new ValidationException("At least one component is required");
    }
    if (components.size() > MAX_COMPONENTS) {
      throw new ValidationException("No more than {} component models are allowed", MAX_COMPONENTS);
    }
    Set<Long> componentIds = new HashSet<>();
    List<ConfigurationComponentItem> copied = new ArrayList<>(components.size());
    for (ConfigurationComponentItem item : components) {
      if (item == null || item.componentId() == null || item.componentId() <= 0) {
        throw new ValidationException("Component identifiers must be positive");
      }
      if (item.quantity() == null
          || item.quantity() < 1
          || item.quantity() > MAX_COMPONENT_QUANTITY) {
        throw new ValidationException(
            "Component quantity must be between 1 and {}", MAX_COMPONENT_QUANTITY);
      }
      if (!componentIds.add(item.componentId())) {
        throw new ValidationException("Component identifiers must be unique");
      }
      copied.add(new ConfigurationComponentItem(item.componentId(), item.quantity()));
    }
    return List.copyOf(copied);
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

  private static List<ConfigurationComponent> toConfigurationComponents(
      Domain domain,
      List<Component> components,
      List<ConfigurationComponentItem> requestedComponents) {
    Map<Long, String> typeNames = new LinkedHashMap<>();
    if (domain.componentTypes() != null) {
      for (ComponentType type : domain.componentTypes()) {
        typeNames.put(type.id(), type.name());
      }
    }
    Map<Long, Component> componentsById = new HashMap<>();
    for (Component component : components) {
      componentsById.put(component.getId(), component);
    }
    return requestedComponents.stream()
        .map(
            item -> {
              Component component = componentsById.get(item.componentId());
              return ConfigurationComponent.builder()
                  .id(component.getId())
                  .name(component.getName())
                  .brand(component.getBrand())
                  .componentTypeId(component.getComponentTypeId())
                  .componentTypeName(typeNames.get(component.getComponentTypeId()))
                  .archived(Boolean.TRUE.equals(component.getArchived()))
                  .quantity(item.quantity())
                  .build();
            })
        .toList();
  }

  private void lockComponentIds(List<Long> componentIds) {
    componentRepository.lockByIds(componentIds.stream().distinct().sorted().toList());
  }

  private static List<Long> unionComponentIds(
      List<ConfigurationComponent> existing, List<ConfigurationComponentItem> target) {
    Set<Long> componentIds = new HashSet<>();
    existing.forEach(component -> componentIds.add(component.id()));
    target.forEach(component -> componentIds.add(component.componentId()));
    return componentIds.stream().sorted().toList();
  }

  private static void validatePagination(int page, int size) {
    if (page < 0) {
      throw new ValidationException("Page must be greater than or equal to 0");
    }
    if (size < 1 || size > MAX_SIZE) {
      throw new ValidationException("Size must be between 1 and {}", MAX_SIZE);
    }
  }

  private record NormalizedDraft(
      String name,
      String description,
      boolean trackInventory,
      List<ConfigurationComponentItem> components) {
    List<Long> componentIds() {
      return components.stream().map(ConfigurationComponentItem::componentId).toList();
    }
  }

  private record ValidatedConfiguration(
      String name,
      String description,
      boolean trackInventory,
      List<ConfigurationComponent> components,
      List<ConfigurationComponentItem> items) {
    Set<Long> componentIds() {
      return items.stream()
          .map(ConfigurationComponentItem::componentId)
          .collect(java.util.stream.Collectors.toSet());
    }
  }
}
