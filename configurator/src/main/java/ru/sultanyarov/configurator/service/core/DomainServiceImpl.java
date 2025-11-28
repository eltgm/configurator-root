package ru.sultanyarov.configurator.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;
import ru.sultanyarov.configurator.domain.repository.DomainRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainServiceImpl implements DomainService {
    private final DomainRepository domainRepository;

    @Override
    public Page<Domain> getPage(int page, int pageSize) {
        log.debug("get domains page {} with page size {}", page, pageSize);
        return domainRepository.getDomains(page, pageSize);
    }

    @Override
    public Domain getById(Long id) {
        log.debug("get domain by id {}", id);
        validateExistenceById(id);

        return domainRepository.getDomainById(id);
    }

    @Override
    public void deleteById(Long id) {
        log.debug("delete domain by id {}", id);
        validateExistenceById(id);
        validateDomainWithoutComponents(id);

        domainRepository.deleteDomainById(id);
    }

    @Override
    @Transactional
    public Domain create(Domain domain) {
        log.debug("create domain {}", domain);
        validateExistenceByName(domain);

        return domainRepository.createDomain(domain);
    }

    @Override
    @Transactional
    public Domain update(Long id, Domain domain) {
        log.debug("update domain {} with id {}", domain, id);
        validateExistenceById(id);
        validateExistenceByName(domain);

        return domainRepository.updateDomain(id, domain);
    }

    private void validateExistenceById(Long id) {
        if (!domainRepository.existsById(id)) {
            throw new NotFoundException("Domain with id {} not found", id);
        }
    }

    private void validateExistenceByName(Domain domain) {
        if (domainRepository.existsByName(domain.name())) {
            throw new EntityAlreadyExistsException("Domain with name {} already exists", domain.name());
        }
    }

    private void validateDomainWithoutComponents(Long id) {
        //TODO after resolving con1-36
    }
}
