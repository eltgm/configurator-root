package ru.sultanyarov.configurator.application.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleSet;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.SaveCompatibilityRuleSetRequest;
import ru.sultanyarov.configurator.application.mapper.CompatibilityRuleMapper;
import ru.sultanyarov.configurator.application.service.CompatibilityRuleService;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompatibilityRuleFacadeImpl implements CompatibilityRuleFacade {
    private final CompatibilityRuleService compatibilityRuleService;
    private final CompatibilityRuleMapper compatibilityRuleMapper;

    @Override
    public CompatibilityRuleSet create(Long domainId, SaveCompatibilityRuleSetRequest request) {
        log.info("Creating compatibility rule set in domain with id {}", domainId);
        return compatibilityRuleMapper.toDto(
                compatibilityRuleService.create(compatibilityRuleMapper.toEntity(domainId, request))
        );
    }

    @Override
    public List<CompatibilityRuleSet> getAllByDomainId(Long domainId) {
        log.info("Getting compatibility rule sets from domain with id {}", domainId);
        return compatibilityRuleMapper.toDtos(
                compatibilityRuleService.getAllByDomainId(domainId)
        );
    }

    @Override
    public CompatibilityRuleSet getByIdAndDomainId(Long ruleSetId, Long domainId) {
        log.info("Getting compatibility rule set with id {} from domain with id {}", ruleSetId, domainId);
        return compatibilityRuleMapper.toDto(
                compatibilityRuleService.getByIdAndDomainId(ruleSetId, domainId)
        );
    }

    @Override
    public CompatibilityRuleSet updateByIdAndDomainId(
            Long ruleSetId,
            Long domainId,
            SaveCompatibilityRuleSetRequest request
    ) {
        log.info("Updating compatibility rule set with id {} in domain with id {}", ruleSetId, domainId);
        return compatibilityRuleMapper.toDto(
                compatibilityRuleService.updateByIdAndDomainId(
                        ruleSetId,
                        domainId,
                        compatibilityRuleMapper.toEntity(domainId, request)
                )
        );
    }

    @Override
    public void deleteByIdAndDomainId(Long ruleSetId, Long domainId) {
        log.info("Deleting compatibility rule set with id {} from domain with id {}", ruleSetId, domainId);
        compatibilityRuleService.deleteByIdAndDomainId(ruleSetId, domainId);
    }
}
