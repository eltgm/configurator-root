package ru.sultanyarov.configurator.api.inbounds.rest.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.api.inbounds.rest.DomainsApi;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateDomainRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Domain;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.DomainPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.UpdateDomainRequest;
import ru.sultanyarov.configurator.application.facade.DomainFacade;

@RestController
@RequiredArgsConstructor
public class DomainController implements DomainsApi {
  private final DomainFacade domainFacade;

  @Override
  public ResponseEntity<Domain> domainsDemoPost() {
    return ResponseEntity.status(CREATED).body(domainFacade.createDemoDomain());
  }

  @Override
  public ResponseEntity<DomainPage> domainsGet(Integer page, Integer size) {
    return ResponseEntity.ok(domainFacade.getDomains(page, size));
  }

  @Override
  public ResponseEntity<Void> domainsIdDelete(Long id) {
    domainFacade.deleteDomainById(id);
    return ResponseEntity.status(NO_CONTENT).build();
  }

  @Override
  public ResponseEntity<Domain> domainsIdGet(Long id) {
    return ResponseEntity.ok(domainFacade.getDomainById(id));
  }

  @Override
  public ResponseEntity<Domain> domainsIdPut(Long id, UpdateDomainRequest updateDomainRequest) {
    return ResponseEntity.ok(domainFacade.updateDomain(id, updateDomainRequest));
  }

  @Override
  public ResponseEntity<Domain> domainsPost(CreateDomainRequest createDomainRequest) {
    return ResponseEntity.status(CREATED).body(domainFacade.createDomain(createDomainRequest));
  }
}
