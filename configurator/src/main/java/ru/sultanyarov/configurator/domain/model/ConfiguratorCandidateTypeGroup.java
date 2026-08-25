package ru.sultanyarov.configurator.domain.model;

import java.util.List;
import lombok.Builder;

@Builder
public record ConfiguratorCandidateTypeGroup(
    Long componentTypeId,
    String componentTypeName,
    List<ConfiguratorAssemblyCandidate> components) {}
