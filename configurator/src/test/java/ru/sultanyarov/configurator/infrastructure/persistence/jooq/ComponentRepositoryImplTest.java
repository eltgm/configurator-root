package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.Component;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentRepositoryImplTest extends AbstractJooqRepositoryTest {

    private ComponentRepositoryImpl repository;

    @BeforeEach
    void setUpRepository() {
        repository = new ComponentRepositoryImpl(dslContext);

        dslContext.insertInto(Tables.DOMAIN)
                .set(Tables.DOMAIN.ID, 1L)
                .set(Tables.DOMAIN.NAME, "Domain")
                .set(Tables.DOMAIN.CREATED_BY_USER_ID, 1L)
                .execute();

        dslContext.insertInto(Tables.COMPONENT_TYPE)
                .set(Tables.COMPONENT_TYPE.ID, 1L)
                .set(Tables.COMPONENT_TYPE.DOMAIN_ID, 1L)
                .set(Tables.COMPONENT_TYPE.NAME, "Switch")
                .execute();
    }

    @Test
    void shouldCreateComponentAndMapReturnedRecord() {
        Component created = repository.createComponent(Component.builder()
                        .componentTypeId(1L)
                        .name("Gateron Yellow")
                        .brand("Gateron")
                        .description("Linear")
                        .archived(false)
                        .build())
                .orElseThrow();

        assertThat(created.getId()).isNotNull();
        assertThat(created.getComponentTypeId()).isEqualTo(1L);
        assertThat(created.getName()).isEqualTo("Gateron Yellow");
        assertThat(created.getBrand()).isEqualTo("Gateron");
        assertThat(created.getDescription()).isEqualTo("Linear");
        assertThat(created.getArchived()).isFalse();
    }
}
