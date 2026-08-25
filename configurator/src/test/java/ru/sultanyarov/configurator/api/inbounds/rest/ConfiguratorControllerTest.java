package ru.sultanyarov.configurator.api.inbounds.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import ru.sultanyarov.configurator.api.inbounds.rest.controller.ConfiguratorController;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorBatchSearchRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorBatchSearchResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorCandidatesRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorCandidatesResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorIntersectionRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorIntersectionResponse;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorResponse;
import ru.sultanyarov.configurator.application.facade.ConfiguratorFacade;

@ExtendWith(MockitoExtension.class)
class ConfiguratorControllerTest {
  @Mock private ConfiguratorFacade configuratorFacade;
  @InjectMocks private ConfiguratorController controller;

  @Test
  void getDomainsByIdConfiguratorCompatible_shouldDelegateToFacade() {
    ConfiguratorResponse responseBody = new ConfiguratorResponse(7L, List.of());
    when(configuratorFacade.getCompatibleComponents(1L, 7L, true)).thenReturn(responseBody);

    var response = controller.getDomainsByIdConfiguratorCompatible(1L, 7L, true);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(responseBody);
    verify(configuratorFacade).getCompatibleComponents(1L, 7L, true);
  }

  @Test
  void getDomainsByIdConfiguratorCompatible_shouldDefaultTransitiveFlagToFalse() {
    ConfiguratorResponse responseBody = new ConfiguratorResponse(7L, List.of());
    when(configuratorFacade.getCompatibleComponents(1L, 7L, false)).thenReturn(responseBody);

    var response = controller.getDomainsByIdConfiguratorCompatible(1L, 7L, null);

    assertThat(response.getBody()).isSameAs(responseBody);
    verify(configuratorFacade).getCompatibleComponents(1L, 7L, false);
  }

  @Test
  void postDomainsByIdConfiguratorCompatibleSearch_shouldDelegateToFacade() {
    ConfiguratorBatchSearchRequest request = new ConfiguratorBatchSearchRequest(List.of(7L, 8L));
    ConfiguratorBatchSearchResponse responseBody = new ConfiguratorBatchSearchResponse(List.of());
    when(configuratorFacade.searchCompatibleComponents(1L, request)).thenReturn(responseBody);

    var response = controller.postDomainsByIdConfiguratorCompatibleSearch(1L, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(responseBody);
    verify(configuratorFacade).searchCompatibleComponents(1L, request);
  }

  @Test
  void postDomainsByIdConfiguratorCompatibleIntersection_shouldDelegateToFacade() {
    ConfiguratorIntersectionRequest request = new ConfiguratorIntersectionRequest(List.of(7L, 8L));
    ConfiguratorIntersectionResponse responseBody =
        new ConfiguratorIntersectionResponse(List.of(7L, 8L), List.of());
    when(configuratorFacade.intersectCompatibleComponents(1L, request)).thenReturn(responseBody);

    var response = controller.postDomainsByIdConfiguratorCompatibleIntersection(1L, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(responseBody);
    verify(configuratorFacade).intersectCompatibleComponents(1L, request);
  }

  @Test
  void postDomainsByIdConfiguratorCandidates_shouldDelegateToFacade() {
    ConfiguratorCandidatesRequest request = new ConfiguratorCandidatesRequest(List.of(7L, 8L));
    ConfiguratorCandidatesResponse responseBody =
        new ConfiguratorCandidatesResponse(
            List.of(7L, 8L),
            List.of(),
            ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorAssemblyStatus.VALID,
            List.of());
    when(configuratorFacade.classifyCandidates(1L, request)).thenReturn(responseBody);

    var response = controller.postDomainsByIdConfiguratorCandidates(1L, request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isSameAs(responseBody);
    verify(configuratorFacade).classifyCandidates(1L, request);
  }
}
