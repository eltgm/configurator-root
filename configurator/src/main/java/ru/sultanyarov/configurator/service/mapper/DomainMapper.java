package ru.sultanyarov.configurator.service.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.sultanyarov.configurator.domain.dto.CreateDomainRequest;
import ru.sultanyarov.configurator.domain.dto.DomainPage;
import ru.sultanyarov.configurator.domain.dto.UpdateDomainRequest;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;

@Mapper(componentModel = "spring")
public interface DomainMapper {
    DomainPage toDomainPageDto(Page<Domain> domains);

    ru.sultanyarov.configurator.domain.dto.Domain toDomainDto(Domain domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdByUserId", constant = "-1l")
    Domain toDomain(UpdateDomainRequest updateDomainRequest);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdByUserId", constant = "-1l")
    Domain toDomain(CreateDomainRequest createDomainRequest);
}
