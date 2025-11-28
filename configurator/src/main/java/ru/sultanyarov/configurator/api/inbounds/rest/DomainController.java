package ru.sultanyarov.configurator.api.inbounds.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.domain.dto.CreateDomainRequest;
import ru.sultanyarov.configurator.domain.dto.Domain;
import ru.sultanyarov.configurator.domain.dto.DomainPage;
import ru.sultanyarov.configurator.domain.dto.UpdateDomainRequest;
import ru.sultanyarov.configurator.service.facade.DomainFacade;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;

@RestController
@RequiredArgsConstructor
public class DomainController implements DomainsApi {
    private final DomainFacade domainFacade;

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
        return ResponseEntity.status(CREATED)
                .body(domainFacade.createDomain(createDomainRequest));
    }
}
