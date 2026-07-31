package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.Domain;

import static org.assertj.core.api.Assertions.assertThat;

class DomainRepositoryImplTest extends AbstractJooqRepositoryTest {

    @Test
    void shouldCreateUpdateDeleteAndQueryDomains() {
        DomainRepositoryImpl repository = new DomainRepositoryImpl(dslContext);

        Domain created = repository.createDomain(Domain.builder()
                        .name("Keyboards")
                        .description("Mechanical keyboards")
                        .createdByUserId(1L)
                        .build())
                .orElseThrow();

        assertThat(created.id()).isNotNull();
        assertThat(repository.existsById(created.id())).isTrue();
        assertThat(repository.existsByName("Keyboards")).isTrue();

        dslContext.insertInto(Tables.COMPONENT_TYPE)
                .set(Tables.COMPONENT_TYPE.DOMAIN_ID, created.id())
                .set(Tables.COMPONENT_TYPE.NAME, "Switch")
                .set(Tables.COMPONENT_TYPE.CODE, "SWITCH")
                .set(Tables.COMPONENT_TYPE.DESCRIPTION, "switches")
                .set(Tables.COMPONENT_TYPE.ORDER_INDEX, 1)
                .execute();

        Domain fetched = repository.getDomainById(created.id()).orElseThrow();
        assertThat(fetched.name()).isEqualTo("Keyboards");
        assertThat(fetched.componentTypes()).hasSize(1);
        assertThat(fetched.componentTypes().get(0).name()).isEqualTo("Switch");

        Domain updated = repository.updateDomain(created.id(), Domain.builder()
                        .name("Boards")
                        .description("Custom boards")
                        .createdByUserId(2L)
                        .build())
                .orElseThrow();

        assertThat(updated.name()).isEqualTo("Boards");
        assertThat(updated.createdByUserId()).isEqualTo(2L);

        dslContext.insertInto(Tables.DOMAIN)
                .set(Tables.DOMAIN.NAME, "Second")
                .set(Tables.DOMAIN.DESCRIPTION, "Another")
                .set(Tables.DOMAIN.CREATED_BY_USER_ID, 1L)
                .execute();

        var page = repository.getDomains(0, 10);
        assertThat(page.items()).hasSize(2);
        assertThat(page.totalItems()).isEqualTo(2);

        repository.deleteDomainById(created.id());
        assertThat(repository.getDomainById(created.id())).isEmpty();
    }
}
