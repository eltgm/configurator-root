package ru.sultanyarov.configurator.api.inbounds.rest.controller;

import static org.springframework.http.HttpStatus.CREATED;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.api.inbounds.rest.CompatibilityApi;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityLink;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleSet;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateCompatibilityLinkRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.GraphResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.SaveCompatibilityRuleSetRequest;
import ru.sultanyarov.configurator.application.facade.CompatibilityFacade;
import ru.sultanyarov.configurator.application.facade.CompatibilityRuleFacade;

@RestController
@RequiredArgsConstructor
public class CompatibilityController implements CompatibilityApi {
  private final CompatibilityFacade compatibilityFacade;
  private final CompatibilityRuleFacade compatibilityRuleFacade;

  @Override
  public ResponseEntity<GraphResponse> getDomainsByIdCompatibilityGraph(Long id) {
    return ResponseEntity.ok(compatibilityFacade.getCompatibilityGraph(id));
  }

  @Override
  public ResponseEntity<Void> deleteDomainsByIdCompatibilityByLinkId(Long id, Long linkId) {
    compatibilityFacade.deleteCompatibilityLink(id, linkId);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<CompatibilityLink> postDomainsByIdCompatibility(
      Long id, CreateCompatibilityLinkRequest createCompatibilityLinkRequest) {
    return ResponseEntity.status(CREATED)
        .body(compatibilityFacade.createCompatibilityLink(id, createCompatibilityLinkRequest));
  }

  @Override
  public ResponseEntity<List<CompatibilityRuleSet>> getDomainsByIdCompatibilityRules(Long id) {
    return ResponseEntity.ok(compatibilityRuleFacade.getAllByDomainId(id));
  }

  @Override
  public ResponseEntity<CompatibilityRuleSet> postDomainsByIdCompatibilityRules(
      Long id, SaveCompatibilityRuleSetRequest saveCompatibilityRuleSetRequest) {
    return ResponseEntity.status(CREATED)
        .body(compatibilityRuleFacade.create(id, saveCompatibilityRuleSetRequest));
  }

  @Override
  public ResponseEntity<CompatibilityRuleSet> getDomainsByIdCompatibilityRulesByRuleId(
      Long id, Long ruleId) {
    return ResponseEntity.ok(compatibilityRuleFacade.getByIdAndDomainId(ruleId, id));
  }

  @Override
  public ResponseEntity<CompatibilityRuleSet> putDomainsByIdCompatibilityRulesByRuleId(
      Long id, Long ruleId, SaveCompatibilityRuleSetRequest saveCompatibilityRuleSetRequest) {
    return ResponseEntity.ok(
        compatibilityRuleFacade.updateByIdAndDomainId(ruleId, id, saveCompatibilityRuleSetRequest));
  }

  @Override
  public ResponseEntity<Void> deleteDomainsByIdCompatibilityRulesByRuleId(Long id, Long ruleId) {
    compatibilityRuleFacade.deleteByIdAndDomainId(ruleId, id);
    return ResponseEntity.noContent().build();
  }
}
