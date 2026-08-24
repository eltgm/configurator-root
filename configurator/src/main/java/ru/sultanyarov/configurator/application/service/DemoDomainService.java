package ru.sultanyarov.configurator.application.service;

import ru.sultanyarov.configurator.domain.model.Domain;

/** Creates the deterministic demo catalog used by the first-run experience. */
public interface DemoDomainService {
  Domain createDemoDomain();
}
