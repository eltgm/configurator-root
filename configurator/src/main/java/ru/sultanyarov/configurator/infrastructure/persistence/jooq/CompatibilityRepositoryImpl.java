package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRepository;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;

import java.util.Optional;

import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPATIBILITY_LINK;

@Repository
@RequiredArgsConstructor
public class CompatibilityRepositoryImpl implements CompatibilityRepository {
    private final DSLContext dslContext;

    @Override
    public Optional<CompatibilityLink> create(CompatibilityLink compatibilityLink) {
        return dslContext.insertInto(COMPATIBILITY_LINK)
                .set(COMPATIBILITY_LINK.DOMAIN_ID, compatibilityLink.domainId())
                .set(COMPATIBILITY_LINK.COMPONENT_A_ID, compatibilityLink.componentAId())
                .set(COMPATIBILITY_LINK.COMPONENT_B_ID, compatibilityLink.componentBId())
                .set(COMPATIBILITY_LINK.COMMENT, compatibilityLink.comment())
                .onConflict(
                        COMPATIBILITY_LINK.DOMAIN_ID,
                        COMPATIBILITY_LINK.COMPONENT_A_ID,
                        COMPATIBILITY_LINK.COMPONENT_B_ID
                )
                .doNothing()
                .returning()
                .fetchOptional(record -> CompatibilityLink.builder()
                        .id(record.get(COMPATIBILITY_LINK.ID))
                        .domainId(record.get(COMPATIBILITY_LINK.DOMAIN_ID))
                        .componentAId(record.get(COMPATIBILITY_LINK.COMPONENT_A_ID))
                        .componentBId(record.get(COMPATIBILITY_LINK.COMPONENT_B_ID))
                        .comment(record.get(COMPATIBILITY_LINK.COMMENT))
                        .build());
    }

    @Override
    public boolean deleteByIdAndDomainId(Long linkId, Long domainId) {
        return dslContext.deleteFrom(COMPATIBILITY_LINK)
                .where(COMPATIBILITY_LINK.ID.eq(linkId))
                .and(COMPATIBILITY_LINK.DOMAIN_ID.eq(domainId))
                .execute() > 0;
    }
}
