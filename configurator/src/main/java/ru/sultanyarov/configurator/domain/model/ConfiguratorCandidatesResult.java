package ru.sultanyarov.configurator.domain.model;

import java.util.List;
import lombok.Builder;

@Builder
public record ConfiguratorCandidatesResult(
    List<Long> componentIds,
    List<ConfiguratorCandidateTypeGroup> candidatesByType,
    ConfiguratorAssemblyStatus assemblyStatus,
    List<ConfiguratorAssemblyPairDecision> assemblyDecisions) {}
