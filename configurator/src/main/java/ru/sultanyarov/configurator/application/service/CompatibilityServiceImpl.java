package ru.sultanyarov.configurator.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRepository;
import ru.sultanyarov.configurator.domain.exception.ComponentArchivedException;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Domain;

import java.util.List;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompatibilityServiceImpl implements CompatibilityService {
    private final CompatibilityRepository compatibilityRepository;
    private final DomainService domainService;
    private final ComponentService componentService;

    @Override
    @Transactional
    public CompatibilityLink create(CompatibilityLink compatibilityLink) {
        log.debug(
                "create compatibility link in domain {} between components {} and {}",
                compatibilityLink.domainId(),
                compatibilityLink.componentAId(),
                compatibilityLink.componentBId()
        );

        Domain domain = domainService.getById(compatibilityLink.domainId());
        validateNotSelfLink(compatibilityLink);

        Component firstComponent = componentService.getById(compatibilityLink.componentAId());
        Component secondComponent = componentService.getById(compatibilityLink.componentBId());
        Set<Long> domainComponentTypeIds = componentTypeIds(domain);

        validateBelongsToDomain(firstComponent, domain.id(), domainComponentTypeIds);
        validateBelongsToDomain(secondComponent, domain.id(), domainComponentTypeIds);
        validateActive(firstComponent);
        validateActive(secondComponent);

        CompatibilityLink normalizedLink = normalize(compatibilityLink);
        return compatibilityRepository.create(normalizedLink)
                .orElseThrow(() -> new EntityAlreadyExistsException(
                        "Compatibility link between components {} and {} already exists in domain {}",
                        normalizedLink.componentAId(),
                        normalizedLink.componentBId(),
                        normalizedLink.domainId()
                ));
    }

    private static void validateNotSelfLink(CompatibilityLink compatibilityLink) {
        if (compatibilityLink.componentAId().equals(compatibilityLink.componentBId())) {
            throw new ValidationException(
                    "Component with id {} cannot be compatible with itself",
                    compatibilityLink.componentAId()
            );
        }
    }

    private static Set<Long> componentTypeIds(Domain domain) {
        List<ComponentType> componentTypes = domain.componentTypes() == null
                ? List.of()
                : domain.componentTypes();
        return componentTypes.stream()
                .map(ComponentType::id)
                .collect(toSet());
    }

    private static void validateBelongsToDomain(
            Component component,
            Long domainId,
            Set<Long> domainComponentTypeIds
    ) {
        if (!domainComponentTypeIds.contains(component.getComponentTypeId())) {
            throw new ValidationException(
                    "Component with id {} does not belong to domain with id {}",
                    component.getId(),
                    domainId
            );
        }
    }

    private static void validateActive(Component component) {
        if (Boolean.TRUE.equals(component.getArchived())) {
            throw new ComponentArchivedException(
                    "Cannot create compatibility link for archived component with id {}",
                    component.getId()
            );
        }
    }

    private static CompatibilityLink normalize(CompatibilityLink compatibilityLink) {
        long firstId = Math.min(compatibilityLink.componentAId(), compatibilityLink.componentBId());
        long secondId = Math.max(compatibilityLink.componentAId(), compatibilityLink.componentBId());
        return CompatibilityLink.builder()
                .domainId(compatibilityLink.domainId())
                .componentAId(firstId)
                .componentBId(secondId)
                .comment(compatibilityLink.comment())
                .build();
    }
}
