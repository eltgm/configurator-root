package ru.sultanyarov.configurator.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import ru.sultanyarov.configurator.domain.model.Configuration;
import ru.sultanyarov.configurator.domain.model.Page;

public interface ConfigurationRepository {
  /** Includes every configuration in the domain, irrespective of its owner or component state. */
  boolean existsByDomainId(Long domainId);

  Optional<Configuration> create(Configuration configuration);

  Optional<Configuration> update(Long id, Long userId, Configuration configuration);

  boolean deleteByIdAndUserId(Long id, Long userId);

  Optional<Configuration> findByIdAndUserId(Long id, Long userId);

  /**
   * Returns an owned configuration while holding its row lock until the current transaction ends.
   */
  Optional<Configuration> findByIdAndUserIdForUpdate(Long id, Long userId);

  /**
   * Returns the quantities currently occupied by inventory-tracked configurations for each
   * requested component. Callers hold the corresponding component row locks before using it.
   */
  Map<Long, Integer> findAllocatedQuantitiesByComponentIds(Collection<Long> componentIds);

  Page<Configuration> findPageByDomainIdAndUserId(
      Long domainId, Long userId, Boolean trackInventory, int page, int size);
}
