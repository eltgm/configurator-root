package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
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
        dslContext.insertInto(Tables.COMPONENT)
                .set(Tables.COMPONENT.ID, id)
                .set(Tables.COMPONENT.COMPONENT_TYPE_ID, 10L)
                .set(Tables.COMPONENT.NAME, name)
                .execute();
    }
}
