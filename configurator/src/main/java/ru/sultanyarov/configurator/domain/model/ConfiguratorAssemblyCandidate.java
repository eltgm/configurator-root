package ru.sultanyarov.configurator.domain.model;

import java.util.List;
import lombok.Builder;

@Builder
public record ConfiguratorAssemblyCandidate(
    Long id,
    String name,
    String brand,
    Long componentTypeId,
    ConfiguratorCandidateStatus status,
    List<ConfiguratorCandidateBaseDecision> compatibilityByBase) {}
