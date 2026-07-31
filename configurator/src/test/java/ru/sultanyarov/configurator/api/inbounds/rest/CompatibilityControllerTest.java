package ru.sultanyarov.configurator.api.inbounds.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.sultanyarov.configurator.api.inbounds.rest.controller.CompatibilityController;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityLink;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleSet;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateCompatibilityLinkRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.GraphResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.SaveCompatibilityRuleSetRequest;
import ru.sultanyarov.configurator.application.facade.CompatibilityFacade;
import ru.sultanyarov.configurator.application.facade.CompatibilityRuleFacade;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompatibilityControllerTest {

    @Mock
    private CompatibilityFacade compatibilityFacade;

    @Mock
    private CompatibilityRuleFacade compatibilityRuleFacade;

    @InjectMocks
    private CompatibilityController compatibilityController;

    @Test
    void domainsIdCompatibilityGraphGet_shouldDelegateRetrievalToFacade() {
        GraphResponse graphResponse = new GraphResponse(List.of(), List.of());
        when(compatibilityFacade.getCompatibilityGraph(1L)).thenReturn(graphResponse);

        ResponseEntity<GraphResponse> response =
                compatibilityController.domainsIdCompatibilityGraphGet(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(graphResponse);
        verify(compatibilityFacade).getCompatibilityGraph(1L);
    }

    @Test
    void domainsIdCompatibilityPost_shouldDelegateCreationToFacade() {
        CreateCompatibilityLinkRequest request = new CreateCompatibilityLinkRequest(9L, 3L);
        CompatibilityLink createdLink = new CompatibilityLink(11L, 1L, 3L, 9L)
                .comment("Compatible");
        when(compatibilityFacade.createCompatibilityLink(1L, request)).thenReturn(createdLink);

        ResponseEntity<CompatibilityLink> response =
                compatibilityController.domainsIdCompatibilityPost(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(createdLink);
        verify(compatibilityFacade).createCompatibilityLink(1L, request);
    }

    @Test
    void domainsIdCompatibilityLinkIdDelete_shouldDelegateDeletionToFacade() {
        ResponseEntity<Void> response =
                compatibilityController.domainsIdCompatibilityLinkIdDelete(1L, 11L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(compatibilityFacade).deleteCompatibilityLink(1L, 11L);
    }

    @Test
    void domainsIdCompatibilityRulesPost_shouldReturnCreatedRuleSet() {
        SaveCompatibilityRuleSetRequest request = ruleRequest();
        CompatibilityRuleSet ruleSet = ruleDto(7L);
        when(compatibilityRuleFacade.create(1L, request)).thenReturn(ruleSet);

        ResponseEntity<CompatibilityRuleSet> response =
                compatibilityController.domainsIdCompatibilityRulesPost(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(ruleSet);
        verify(compatibilityRuleFacade).create(1L, request);
    }

    @Test
    void domainsIdCompatibilityRulesGet_shouldReturnRuleSets() {
        List<CompatibilityRuleSet> ruleSets = List.of(ruleDto(7L));
        when(compatibilityRuleFacade.getAllByDomainId(1L)).thenReturn(ruleSets);

        ResponseEntity<List<CompatibilityRuleSet>> response =
                compatibilityController.domainsIdCompatibilityRulesGet(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(ruleSets);
        verify(compatibilityRuleFacade).getAllByDomainId(1L);
    }

    @Test
    void domainsIdCompatibilityRulesRuleIdGet_shouldReturnScopedRuleSet() {
        CompatibilityRuleSet ruleSet = ruleDto(7L);
        when(compatibilityRuleFacade.getByIdAndDomainId(7L, 1L)).thenReturn(ruleSet);

        ResponseEntity<CompatibilityRuleSet> response =
                compatibilityController.domainsIdCompatibilityRulesRuleIdGet(1L, 7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(ruleSet);
        verify(compatibilityRuleFacade).getByIdAndDomainId(7L, 1L);
    }

    @Test
    void domainsIdCompatibilityRulesRuleIdPut_shouldReturnUpdatedRuleSet() {
        SaveCompatibilityRuleSetRequest request = ruleRequest();
        CompatibilityRuleSet ruleSet = ruleDto(7L);
        when(compatibilityRuleFacade.updateByIdAndDomainId(7L, 1L, request)).thenReturn(ruleSet);

        ResponseEntity<CompatibilityRuleSet> response =
                compatibilityController.domainsIdCompatibilityRulesRuleIdPut(1L, 7L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(ruleSet);
        verify(compatibilityRuleFacade).updateByIdAndDomainId(7L, 1L, request);
    }

    @Test
    void domainsIdCompatibilityRulesRuleIdDelete_shouldReturnNoContent() {
        ResponseEntity<Void> response =
                compatibilityController.domainsIdCompatibilityRulesRuleIdDelete(1L, 7L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(compatibilityRuleFacade).deleteByIdAndDomainId(7L, 1L);
    }

    private static SaveCompatibilityRuleSetRequest ruleRequest() {
        return new SaveCompatibilityRuleSetRequest("Rule", 10L, 20L, true, List.of());
    }

    private static CompatibilityRuleSet ruleDto(Long id) {
        return new CompatibilityRuleSet(
                id,
                1L,
                "Rule",
                10L,
                20L,
                true,
                List.of(),
                LocalDateTime.now()
        );
    }
}
