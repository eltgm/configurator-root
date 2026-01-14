package ru.sultanyarov.configurator.service.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;
import ru.sultanyarov.configurator.domain.repository.DomainRepository;

import java.util.Objects;

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
        return domainRepository.getDomainById(id)
                .orElseThrow(() -> new NotFoundException("Domain with id {} not found", id));
    }

    @Override
    public void deleteById(Long id) {
        log.debug("delete domain by id {}", id);
        Domain domain = getById(id);
        ensureNoRelatedEntities(domain);

        domainRepository.deleteDomainById(id);
    }

    @Override
    @Transactional
    public Domain create(Domain newDomain) {
        log.debug("create domain {}", newDomain);
        ensureNotExistsByName(newDomain);

        return domainRepository.createDomain(newDomain)
                .orElseThrow(() -> new BusinessException("Failed to create domain"));
    }

    @Override
    @Transactional
    public Domain update(Long id, Domain updatedDomain) {
        log.debug("update domain {} with id {}", updatedDomain, id);
        Domain existedDomain = getById(id);
        if (!Objects.equals(existedDomain.name(), updatedDomain.name())) {
            ensureNotExistsByName(updatedDomain);
        }

        return domainRepository.updateDomain(id, updatedDomain)
                .orElseThrow(() -> new BusinessException("Failed to update domain with id {}", id));
    }

    private void ensureNotExistsByName(Domain domain) {
        if (domainRepository.existsByName(domain.name())) {
            throw new EntityAlreadyExistsException("Domain with name {} already exists", domain.name());
        }
    }

    private void ensureNoRelatedEntities(Domain domain) {
        //TODO after resolving con1-36
    }
}
