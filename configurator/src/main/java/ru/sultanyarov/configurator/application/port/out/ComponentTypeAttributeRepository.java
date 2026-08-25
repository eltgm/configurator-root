package ru.sultanyarov.configurator.application.port.out;

import java.util.List;
import java.util.Optional;
import ru.sultanyarov.configurator.domain.model.ComponentTypeAttribute;

/** Outbound port for per-component-type attribute links and settings. */
public interface ComponentTypeAttributeRepository {

  Optional<ComponentTypeAttribute> save(ComponentTypeAttribute link);

  Optional<ComponentTypeAttribute> get(Long componentTypeId, Long attributeDefinitionId);

  boolean exists(Long componentTypeId, Long attributeDefinitionId);

  boolean delete(Long componentTypeId, Long attributeDefinitionId);

  List<Long> getComponentTypeIdsByAttributeDefinitionId(Long attributeDefinitionId);
}
