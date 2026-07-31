package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRepository;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.entity.jooq.tables.Component;
import ru.sultanyarov.configurator.domain.entity.jooq.tables.ComponentType;
import ru.sultanyarov.configurator.domain.model.CompatibilityGraph;
import ru.sultanyarov.configurator.domain.model.CompatibilityGraphEdge;
import ru.sultanyarov.configurator.domain.model.CompatibilityGraphNode;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;

import java.util.List;
import java.util.Optional;

import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT_TYPE;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPATIBILITY_LINK;

@Repository
@RequiredArgsConstructor
public class CompatibilityRepositoryImpl implements CompatibilityRepository {
    private final DSLContext dslContext;

    @Override
    public CompatibilityGraph getGraphByDomainId(Long domainId) {
        List<CompatibilityGraphNode> nodes = dslContext.select(
                        COMPONENT.ID,
                        COMPONENT.NAME,
                        COMPONENT.COMPONENT_TYPE_ID,
                        COMPONENT_TYPE.NAME,
                        COMPONENT.BRAND
                )
                .from(COMPONENT)
                .join(COMPONENT_TYPE)
                .on(COMPONENT_TYPE.ID.eq(COMPONENT.COMPONENT_TYPE_ID))
                .where(COMPONENT_TYPE.DOMAIN_ID.eq(domainId))
                .and(COMPONENT.ARCHIVED.isFalse())
                .orderBy(COMPONENT.ID.asc())
                .fetch(record -> CompatibilityGraphNode.builder()
                        .id(record.get(COMPONENT.ID))
                        .name(record.get(COMPONENT.NAME))
                        .componentTypeId(record.get(COMPONENT.COMPONENT_TYPE_ID))
                        .componentTypeName(record.get(COMPONENT_TYPE.NAME))
                        .brand(record.get(COMPONENT.BRAND))
                        .build());

        Component componentA = Tables.COMPONENT.as("component_a");
        ComponentType componentTypeA = Tables.COMPONENT_TYPE.as("component_type_a");
        Component componentB = Tables.COMPONENT.as("component_b");
        ComponentType componentTypeB = Tables.COMPONENT_TYPE.as("component_type_b");

        List<CompatibilityGraphEdge> edges = dslContext.select(
                        COMPATIBILITY_LINK.ID,
                        COMPATIBILITY_LINK.COMPONENT_A_ID,
                        COMPATIBILITY_LINK.COMPONENT_B_ID,
                        COMPATIBILITY_LINK.COMMENT
                )
                .from(COMPATIBILITY_LINK)
                .join(componentA)
                .on(componentA.ID.eq(COMPATIBILITY_LINK.COMPONENT_A_ID))
                .join(componentTypeA)
                .on(componentTypeA.ID.eq(componentA.COMPONENT_TYPE_ID))
                .join(componentB)
                .on(componentB.ID.eq(COMPATIBILITY_LINK.COMPONENT_B_ID))
                .join(componentTypeB)
                .on(componentTypeB.ID.eq(componentB.COMPONENT_TYPE_ID))
                .where(COMPATIBILITY_LINK.DOMAIN_ID.eq(domainId))
                .and(componentTypeA.DOMAIN_ID.eq(domainId))
                .and(componentTypeB.DOMAIN_ID.eq(domainId))
                .and(componentA.ARCHIVED.isFalse())
                .and(componentB.ARCHIVED.isFalse())
                .orderBy(COMPATIBILITY_LINK.ID.asc())
                .fetch(record -> CompatibilityGraphEdge.builder()
                        .id(record.get(COMPATIBILITY_LINK.ID))
                        .source(record.get(COMPATIBILITY_LINK.COMPONENT_A_ID))
                        .target(record.get(COMPATIBILITY_LINK.COMPONENT_B_ID))
                        .comment(record.get(COMPATIBILITY_LINK.COMMENT))
                        .build());

        return CompatibilityGraph.builder()
                .nodes(nodes)
                .edges(edges)
                .build();
    }

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
