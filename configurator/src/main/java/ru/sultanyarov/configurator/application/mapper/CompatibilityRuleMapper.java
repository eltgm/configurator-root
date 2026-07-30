package ru.sultanyarov.configurator.application.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleConditionInput;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.SaveCompatibilityRuleSetRequest;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleCondition;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CompatibilityRuleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    CompatibilityRuleSet toEntity(Long domainId, SaveCompatibilityRuleSetRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ruleSetId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    CompatibilityRuleCondition toEntity(CompatibilityRuleConditionInput condition);

    ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleSet toDto(
            CompatibilityRuleSet ruleSet
    );

    List<ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleSet> toDtos(
            List<CompatibilityRuleSet> ruleSets
    );
}
