package ru.sultanyarov.configurator.application.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityLink;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateCompatibilityLinkRequest;
import ru.sultanyarov.configurator.application.mapper.CompatibilityMapper;
import ru.sultanyarov.configurator.application.service.CompatibilityService;

@Slf4j
@Service
@RequiredArgsConstructor
public class CompatibilityFacadeImpl implements CompatibilityFacade {
    private final CompatibilityService compatibilityService;
    private final CompatibilityMapper compatibilityMapper;

    @Override
    public CompatibilityLink createCompatibilityLink(Long domainId, CreateCompatibilityLinkRequest request) {
        log.info("Creating compatibility link in domain with id {}", domainId);
        String normalizedComment = normalizeComment(request.getComment());
        return compatibilityMapper.toDto(
                compatibilityService.create(
                        compatibilityMapper.toEntity(
                                domainId,
                                request.getComponentAId(),
                                request.getComponentBId(),
                                normalizedComment
                        )
                )
        );
    }

    private static String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }
}
