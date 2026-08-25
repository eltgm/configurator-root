package ru.sultanyarov.configurator.application.facade;

import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorBatchSearchRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorBatchSearchResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorCandidatesRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorCandidatesResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorIntersectionRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorIntersectionResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorResponse;

/** REST boundary for configurator operations. */
public interface ConfiguratorFacade {
  ConfiguratorResponse getCompatibleComponents(
      Long domainId, Long baseComponentId, boolean includeTransitive);

  ConfiguratorBatchSearchResponse searchCompatibleComponents(
      Long domainId, ConfiguratorBatchSearchRequest request);

  ConfiguratorIntersectionResponse intersectCompatibleComponents(
      Long domainId, ConfiguratorIntersectionRequest request);

  ConfiguratorCandidatesResponse classifyCandidates(
      Long domainId, ConfiguratorCandidatesRequest request);
}
