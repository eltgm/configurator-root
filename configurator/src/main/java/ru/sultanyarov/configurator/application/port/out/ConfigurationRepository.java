package ru.sultanyarov.configurator.application.port.out;

import java.util.Optional;
import ru.sultanyarov.configurator.domain.model.Configuration;
import ru.sultanyarov.configurator.domain.model.Page;

public interface ConfigurationRepository {
  Optional<Configuration> create(Configuration configuration);

  Optional<Configuration> update(Long id, Long userId, Configuration configuration);

  Optional<Configuration> findByIdAndUserId(Long id, Long userId);

  Page<Configuration> findPageByDomainIdAndUserId(Long domainId, Long userId, int page, int size);
}
