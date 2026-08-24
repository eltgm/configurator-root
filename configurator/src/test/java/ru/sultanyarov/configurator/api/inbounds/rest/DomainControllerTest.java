package ru.sultanyarov.configurator.api.inbounds.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.sultanyarov.configurator.api.inbounds.rest.controller.DomainController;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateDomainRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Domain;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.DomainPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateDomainRequest;
import ru.sultanyarov.configurator.application.facade.DomainFacade;

@ExtendWith(MockitoExtension.class)
class DomainControllerTest {
  @Mock private DomainFacade domainFacade;

  @InjectMocks private DomainController domainController;

  @Test
  void postDomainsDemo_shouldReturnCreatedDomain() {
    Domain expectedDomain = new Domain();
    expectedDomain.setId(1L);
    expectedDomain.setName("Сборка ПК");
    expectedDomain.setCreatedAt(LocalDateTime.now());
    when(domainFacade.createDemoDomain()).thenReturn(expectedDomain);

    ResponseEntity<Domain> response = domainController.postDomainsDemo();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isSameAs(expectedDomain);
    verify(domainFacade).createDemoDomain();
  }

  @Test
  void getDomains_shouldReturnDomainPage() {
    // Arrange
    int page = 0;
    int size = 10;

    Domain domainDto1 = new Domain();
    domainDto1.setId(1L);
    domainDto1.setName("Test Domain 1");
    domainDto1.setDescription("Test Description 1");
    domainDto1.setCreatedAt(LocalDateTime.now());

    Domain domainDto2 = new Domain();
    domainDto2.setId(2L);
    domainDto2.setName("Test Domain 2");
    domainDto2.setDescription("Test Description 2");
    domainDto2.setCreatedAt(LocalDateTime.now());

    DomainPage expectedPage = new DomainPage();
    expectedPage.setItems(List.of(domainDto1, domainDto2));
    expectedPage.setPage(0);
    expectedPage.setSize(10);
    expectedPage.setTotalItems(2);

    when(domainFacade.getDomains(anyInt(), anyInt())).thenReturn(expectedPage);

    // Act
    ResponseEntity<DomainPage> response = domainController.getDomains(page, size);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expectedPage);
    verify(domainFacade).getDomains(page, size);
  }

  @Test
  void deleteDomainsById_shouldReturnNoContent() {
    // Arrange
    Long id = 1L;

    // Act
    ResponseEntity<Void> response = domainController.deleteDomainsById(id);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    verify(domainFacade).deleteDomainById(id);
  }

  @Test
  void getDomainsById_shouldReturnDomain() {
    // Arrange
    Long id = 1L;

    Domain expectedDomain = new Domain();
    expectedDomain.setId(1L);
    expectedDomain.setName("Test Domain");
    expectedDomain.setDescription("Test Description");
    expectedDomain.setCreatedAt(LocalDateTime.now());

    when(domainFacade.getDomainById(id)).thenReturn(expectedDomain);

    // Act
    ResponseEntity<Domain> response = domainController.getDomainsById(id);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expectedDomain);
    verify(domainFacade).getDomainById(id);
  }

  @Test
  void putDomainsById_shouldReturnUpdatedDomain() {
    // Arrange
    Long id = 1L;
    UpdateDomainRequest request = new UpdateDomainRequest();
    request.setName("Updated Domain");
    request.setDescription("Updated Description");

    Domain expectedDomain = new Domain();
    expectedDomain.setId(1L);
    expectedDomain.setName("Updated Domain");
    expectedDomain.setDescription("Updated Description");
    expectedDomain.setCreatedAt(LocalDateTime.now());

    when(domainFacade.updateDomain(anyLong(), any(UpdateDomainRequest.class)))
        .thenReturn(expectedDomain);

    // Act
    ResponseEntity<Domain> response = domainController.putDomainsById(id, request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expectedDomain);
    verify(domainFacade).updateDomain(id, request);
  }

  @Test
  void postDomains_shouldReturnCreatedDomain() {
    // Arrange
    CreateDomainRequest request = new CreateDomainRequest();
    request.setName("New Domain");
    request.setDescription("New Description");

    Domain expectedDomain = new Domain();
    expectedDomain.setId(1L);
    expectedDomain.setName("New Domain");
    expectedDomain.setDescription("New Description");
    expectedDomain.setCreatedAt(LocalDateTime.now());

    when(domainFacade.createDomain(any(CreateDomainRequest.class))).thenReturn(expectedDomain);

    // Act
    ResponseEntity<Domain> response = domainController.postDomains(request);

    // Assert
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).usingRecursiveComparison().isEqualTo(expectedDomain);
    verify(domainFacade).createDomain(request);
  }
}
