package ru.sultanyarov.configurator.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanation;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanationSource;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;
import ru.sultanyarov.configurator.domain.model.Component;

@UtilityClass
class CompatibilityGraphBuilder {
  static CompatibilityGraphContext build(
      List<Component> activeComponents,
      List<CompatibilityLink> manualLinks,
      List<CompatibilityRuleSet> automaticRules,
      CompatibilityRuleEvaluator ruleEvaluator) {
    IndexedComponents indexedComponents = indexComponents(activeComponents);
    Map<Long, Map<Long, List<CompatibilityExplanation>>> graph = new HashMap<>();
    addManualEdges(graph, manualLinks, indexedComponents.byId().keySet());
    addAutomaticEdges(graph, automaticRules, indexedComponents.byType(), ruleEvaluator);
    return new CompatibilityGraphContext(graph, indexedComponents.order());
  }

  private static IndexedComponents indexComponents(List<Component> components) {
    Map<Long, Component> componentsById = new HashMap<>();
    Map<Long, List<Component>> componentsByType = new HashMap<>();
    Map<Long, Integer> componentOrder = new HashMap<>();
    for (int index = 0; index < components.size(); index++) {
      Component component = components.get(index);
      componentsById.put(component.getId(), component);
      componentsByType
          .computeIfAbsent(component.getComponentTypeId(), ignored -> new ArrayList<>())
          .add(component);
      componentOrder.put(component.getId(), index);
    }
    return new IndexedComponents(componentsById, componentsByType, componentOrder);
  }

  private static void addManualEdges(
      Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
      List<CompatibilityLink> links,
      Set<Long> activeComponentIds) {
    for (CompatibilityLink link : links) {
      if (!activeComponentIds.contains(link.componentAId())
          || !activeComponentIds.contains(link.componentBId())) {
        continue;
      }
      addUndirectedEdge(
          graph,
          link.componentAId(),
          link.componentBId(),
          CompatibilityExplanation.builder()
              .source(CompatibilityExplanationSource.MANUAL)
              .linkId(link.id())
              .comment(link.comment())
              .build());
    }
  }

  private static void addAutomaticEdges(
      Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
      List<CompatibilityRuleSet> rules,
      Map<Long, List<Component>> componentsByType,
      CompatibilityRuleEvaluator ruleEvaluator) {
    for (CompatibilityRuleSet rule : rules) {
      List<Component> componentsA =
          componentsByType.getOrDefault(rule.componentTypeAId(), List.of());
      List<Component> componentsB =
          componentsByType.getOrDefault(rule.componentTypeBId(), List.of());
      connectMatchingComponents(graph, rule, componentsA, componentsB, ruleEvaluator);
    }
  }

  private static void connectMatchingComponents(
      Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
      CompatibilityRuleSet rule,
      List<Component> componentsA,
      List<Component> componentsB,
      CompatibilityRuleEvaluator ruleEvaluator) {
    for (Component componentA : componentsA) {
      for (Component componentB : componentsB) {
        ruleEvaluator
            .evaluate(rule, componentA, componentB)
            .map(CompatibilityExplanations::automatic)
            .ifPresent(
                explanation ->
                    addUndirectedEdge(graph, componentA.getId(), componentB.getId(), explanation));
      }
    }
  }

  private static void addUndirectedEdge(
      Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
      Long componentAId,
      Long componentBId,
      CompatibilityExplanation explanation) {
    addDirectedEdge(graph, componentAId, componentBId, explanation);
    addDirectedEdge(graph, componentBId, componentAId, explanation);
  }

  private static void addDirectedEdge(
      Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
      Long sourceId,
      Long targetId,
      CompatibilityExplanation explanation) {
    graph
        .computeIfAbsent(sourceId, ignored -> new HashMap<>())
        .computeIfAbsent(targetId, ignored -> new ArrayList<>())
        .add(explanation);
  }

  private record IndexedComponents(
      Map<Long, Component> byId, Map<Long, List<Component>> byType, Map<Long, Integer> order) {}
}
