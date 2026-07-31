package ru.sultanyarov.configurator.application.service;

import java.util.List;
import java.util.Map;
import ru.sultanyarov.configurator.domain.model.CompatibilityExplanation;

record CompatibilityGraphContext(
    Map<Long, Map<Long, List<CompatibilityExplanation>>> graph,
    Map<Long, Integer> componentOrder) {}
