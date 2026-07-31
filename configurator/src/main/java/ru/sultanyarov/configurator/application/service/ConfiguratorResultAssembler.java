package ru.sultanyarov.configurator.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import ru.sultanyarov.configurator.domain.model.BaseComponentCompatibility;
import ru.sultanyarov.configurator.domain.model.CompatibleComponent;
import ru.sultanyarov.configurator.domain.model.CompatibleComponentGroup;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.IntersectionCompatibleComponent;
import ru.sultanyarov.configurator.domain.model.IntersectionCompatibleComponentGroup;

@UtilityClass
class ConfiguratorResultAssembler {
  static List<CompatibleComponentGroup> toOrderedGroups(
      Domain domain, Map<Long, List<CompatibleComponent>> compatibleByType) {
    return componentTypes(domain).stream()
        .filter(componentType -> compatibleByType.containsKey(componentType.id()))
        .map(
            componentType ->
                CompatibleComponentGroup.builder()
                    .componentTypeId(componentType.id())
                    .componentTypeName(componentType.name())
                    .components(List.copyOf(compatibleByType.get(componentType.id())))
                    .build())
        .toList();
  }

  static List<IntersectionCompatibleComponentGroup> toOrderedIntersectionGroups(
      Domain domain,
      List<Component> candidates,
      Map<Long, List<BaseComponentCompatibility>> compatibilityByCandidate,
      int expectedBaseCount) {
    Map<Long, List<IntersectionCompatibleComponent>> compatibleByType =
        groupIntersection(candidates, compatibilityByCandidate, expectedBaseCount);
    return componentTypes(domain).stream()
        .filter(componentType -> compatibleByType.containsKey(componentType.id()))
        .map(
            componentType ->
                IntersectionCompatibleComponentGroup.builder()
                    .componentTypeId(componentType.id())
                    .componentTypeName(componentType.name())
                    .components(List.copyOf(compatibleByType.get(componentType.id())))
                    .build())
        .toList();
  }

  private static Map<Long, List<IntersectionCompatibleComponent>> groupIntersection(
      List<Component> candidates,
      Map<Long, List<BaseComponentCompatibility>> compatibilityByCandidate,
      int expectedBaseCount) {
    Map<Long, List<IntersectionCompatibleComponent>> result = new HashMap<>();
    for (Component candidate : candidates) {
      List<BaseComponentCompatibility> compatibilityByBase =
          compatibilityByCandidate.get(candidate.getId());
      if (compatibilityByBase != null && compatibilityByBase.size() == expectedBaseCount) {
        result
            .computeIfAbsent(candidate.getComponentTypeId(), ignored -> new ArrayList<>())
            .add(toIntersectionCompatibleComponent(candidate, compatibilityByBase));
      }
    }
    return result;
  }

  private static List<ComponentType> componentTypes(Domain domain) {
    return domain.componentTypes() == null ? List.of() : domain.componentTypes();
  }

  private static IntersectionCompatibleComponent toIntersectionCompatibleComponent(
      Component component, List<BaseComponentCompatibility> compatibilityByBase) {
    return IntersectionCompatibleComponent.builder()
        .id(component.getId())
        .name(component.getName())
        .brand(component.getBrand())
        .componentTypeId(component.getComponentTypeId())
        .compatibilityByBase(List.copyOf(compatibilityByBase))
        .build();
  }
}
