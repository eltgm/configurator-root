package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.CompatibilityGraph;
import ru.sultanyarov.configurator.domain.model.CompatibilityGraphEdge;
import ru.sultanyarov.configurator.domain.model.CompatibilityGraphNode;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;

import static org.assertj.core.api.Assertions.assertThat;

class CompatibilityRepositoryImplTest extends AbstractJooqRepositoryTest {
    private CompatibilityRepositoryImpl repository;

    @BeforeEach
    void setUpRepository() {
        repository = new CompatibilityRepositoryImpl(dslContext);

        dslContext.insertInto(Tables.DOMAIN)
                .set(Tables.DOMAIN.ID, 1L)
                .set(Tables.DOMAIN.NAME, "Domain")
                .set(Tables.DOMAIN.CREATED_BY_USER_ID, 1L)
                .execute();
        dslContext.insertInto(Tables.COMPONENT_TYPE)
                .set(Tables.COMPONENT_TYPE.ID, 10L)
                .set(Tables.COMPONENT_TYPE.DOMAIN_ID, 1L)
                .set(Tables.COMPONENT_TYPE.NAME, "Switch")
                .execute();
        insertComponent(3L, "First");
        insertComponent(9L, "Second");
    }

    @Test
    void getGraphByDomainId_shouldReturnSortedActiveNodesAndScopedActiveEdges() {
        insertComponent(10L, 10L, "Archived", "Legacy", true);
        insertComponent(11L, 10L, "Isolated", null, false);
        insertComponent(14L, 10L, "Fourth", "Brand", false);
        insertDomain(2L, "Foreign domain");
        insertComponentType(20L, 2L, "Foreign type");
        insertComponent(12L, 20L, "Foreign first", null, false);
        insertComponent(13L, 20L, "Foreign second", null, false);

        insertCompatibilityLink(50L, 1L, 9L, 14L, null);
        insertCompatibilityLink(40L, 1L, 3L, 9L, "Visible");
        insertCompatibilityLink(20L, 1L, 3L, 10L, "Archived endpoint");
        insertCompatibilityLink(30L, 1L, 3L, 12L, "Foreign endpoint");
        insertCompatibilityLink(10L, 2L, 12L, 13L, "Foreign domain");

        CompatibilityGraph graph = repository.getGraphByDomainId(1L);

        assertThat(graph.nodes()).containsExactly(
                new CompatibilityGraphNode(3L, "First", 10L, "Switch", null),
                new CompatibilityGraphNode(9L, "Second", 10L, "Switch", null),
                new CompatibilityGraphNode(11L, "Isolated", 10L, "Switch", null),
                new CompatibilityGraphNode(14L, "Fourth", 10L, "Switch", "Brand")
        );
        assertThat(graph.edges()).containsExactly(
                new CompatibilityGraphEdge(40L, 3L, 9L, "Visible"),
                new CompatibilityGraphEdge(50L, 9L, 14L, null)
        );
    }

    @Test
    void getGraphByDomainId_shouldReturnEmptyGraphForDomainWithoutComponents() {
        insertDomain(2L, "Empty domain");

        CompatibilityGraph graph = repository.getGraphByDomainId(2L);

        assertThat(graph.nodes()).isEmpty();
        assertThat(graph.edges()).isEmpty();
    }

    @Test
    void create_shouldPersistLinkAndUseCompatibleRelationType() {
        CompatibilityLink link = new CompatibilityLink(null, 1L, 3L, 9L, "Compatible");

        CompatibilityLink createdLink = repository.create(link).orElseThrow();

        assertThat(createdLink.id()).isNotNull();
        assertThat(createdLink.domainId()).isEqualTo(1L);
        assertThat(createdLink.componentAId()).isEqualTo(3L);
        assertThat(createdLink.componentBId()).isEqualTo(9L);
        assertThat(createdLink.comment()).isEqualTo("Compatible");
        assertThat(dslContext.select(Tables.COMPATIBILITY_LINK.RELATION_TYPE)
                .from(Tables.COMPATIBILITY_LINK)
                .where(Tables.COMPATIBILITY_LINK.ID.eq(createdLink.id()))
                .fetchOne(Tables.COMPATIBILITY_LINK.RELATION_TYPE))
                .isEqualTo("COMPATIBLE");
    }

    @Test
    void create_shouldReturnEmptyForDuplicateNormalizedPair() {
        CompatibilityLink link = new CompatibilityLink(null, 1L, 3L, 9L, null);

        assertThat(repository.create(link)).isPresent();
        assertThat(repository.create(link)).isEmpty();
        assertThat(dslContext.fetchCount(Tables.COMPATIBILITY_LINK)).isOne();
    }

    @Test
    void deleteByIdAndDomainId_shouldPhysicallyDeleteScopedLink() {
        CompatibilityLink createdLink = repository.create(
                new CompatibilityLink(null, 1L, 3L, 9L, null)
        ).orElseThrow();

        boolean deleted = repository.deleteByIdAndDomainId(createdLink.id(), 1L);

        assertThat(deleted).isTrue();
        assertThat(dslContext.fetchCount(Tables.COMPATIBILITY_LINK)).isZero();
    }

    @Test
    void deleteByIdAndDomainId_shouldNotDeleteLinkFromAnotherDomainScope() {
        CompatibilityLink createdLink = repository.create(
                new CompatibilityLink(null, 1L, 3L, 9L, null)
        ).orElseThrow();

        boolean deleted = repository.deleteByIdAndDomainId(createdLink.id(), 2L);

        assertThat(deleted).isFalse();
        assertThat(dslContext.fetchCount(Tables.COMPATIBILITY_LINK)).isOne();
    }

    @Test
    void deleteByIdAndDomainId_shouldReturnFalseWhenLinkDoesNotExist() {
        assertThat(repository.deleteByIdAndDomainId(999L, 1L)).isFalse();
    }

    private void insertComponent(Long id, String name) {
        insertComponent(id, 10L, name, null, false);
    }

    private void insertDomain(Long id, String name) {
        dslContext.insertInto(Tables.DOMAIN)
                .set(Tables.DOMAIN.ID, id)
                .set(Tables.DOMAIN.NAME, name)
                .set(Tables.DOMAIN.CREATED_BY_USER_ID, 1L)
                .execute();
    }

    private void insertComponentType(Long id, Long domainId, String name) {
        dslContext.insertInto(Tables.COMPONENT_TYPE)
                .set(Tables.COMPONENT_TYPE.ID, id)
                .set(Tables.COMPONENT_TYPE.DOMAIN_ID, domainId)
                .set(Tables.COMPONENT_TYPE.NAME, name)
                .execute();
    }

    private void insertComponent(
            Long id,
            Long componentTypeId,
            String name,
            String brand,
            boolean archived
    ) {
        dslContext.insertInto(Tables.COMPONENT)
                .set(Tables.COMPONENT.ID, id)
                .set(Tables.COMPONENT.COMPONENT_TYPE_ID, componentTypeId)
                .set(Tables.COMPONENT.NAME, name)
                .set(Tables.COMPONENT.BRAND, brand)
                .set(Tables.COMPONENT.ARCHIVED, archived)
                .execute();
    }

    private void insertCompatibilityLink(
            Long id,
            Long domainId,
            Long componentAId,
            Long componentBId,
            String comment
    ) {
        dslContext.insertInto(Tables.COMPATIBILITY_LINK)
                .set(Tables.COMPATIBILITY_LINK.ID, id)
                .set(Tables.COMPATIBILITY_LINK.DOMAIN_ID, domainId)
                .set(Tables.COMPATIBILITY_LINK.COMPONENT_A_ID, componentAId)
                .set(Tables.COMPATIBILITY_LINK.COMPONENT_B_ID, componentBId)
                .set(Tables.COMPATIBILITY_LINK.COMMENT, comment)
                .execute();
    }
}
