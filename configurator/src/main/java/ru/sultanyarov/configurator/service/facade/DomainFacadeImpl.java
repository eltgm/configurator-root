package ru.sultanyarov.configurator.service.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.domain.dto.CreateDomainRequest;
import ru.sultanyarov.configurator.domain.dto.Domain;
import ru.sultanyarov.configurator.domain.dto.DomainPage;
import ru.sultanyarov.configurator.domain.dto.UpdateDomainRequest;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.service.core.DomainService;
import ru.sultanyarov.configurator.service.mapper.DomainMapper;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Service
@RequiredArgsConstructor
public class DomainFacadeImpl implements DomainFacade {
    private final DomainService domainService;
    private final DomainMapper domainMapper;

    @Override
    public Domain createDomain(CreateDomainRequest createDomainRequest) {
        log.info("Start create domain");
        validateCreateDomainRequest(createDomainRequest);

        return domainMapper.toDomainDto(
                domainService.create(
                        domainMapper.toDomain(createDomainRequest)
                )
        );
    }

    private void validateCreateDomainRequest(CreateDomainRequest createDomainRequest) {
        String name = createDomainRequest.getName();
        validateName(name);
    }

    private static void validateName(String name) {
        if (!hasText(name)) {
            throw new ValidationException("Name is required");
        }
    }

    @Override
    public Domain getDomainById(Long id) {
        return domainMapper.toDomainDto(
                domainService.getById(id)
        );
    }

    @Override
    public void deleteDomainById(Long id) {
        log.info("Start delete domain by id: {}", id);
        domainService.deleteById(id);
    }

    @Override
    public Domain updateDomain(Long id, UpdateDomainRequest updateDomainRequest) {
        log.info("Start update domain by id: {}", id);
        validateUpdateDomainRequest(updateDomainRequest);

        return domainMapper.toDomainDto(
                domainService.update(id, domainMapper.toDomain(updateDomainRequest))
        );
    }

    private void validateUpdateDomainRequest(UpdateDomainRequest updateDomainRequest) {
        String name = updateDomainRequest.getName();
        validateName(name);
    }

    @Override
    public DomainPage getDomains(int page, int pageSize) {
        log.info("Start get domains");
        return domainMapper.toDomainPageDto(
                domainService.getPage(page, pageSize)
        );
    }
}
