package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.ComponentType;

import static org.assertj.core.api.Assertions.assertThat;

class ComponentTypeRepositoryImplTest extends AbstractJooqRepositoryTest {

    private ComponentTypeRepositoryImpl repository;

    @BeforeEach
    void setUpRepository() {
        repository = new ComponentTypeRepositoryImpl(dslContext);

        dslContext.insertInto(Tables.DOMAIN)
                .set(Tables.DOMAIN.ID, 1L)
                .set(Tables.DOMAIN.NAME, "Domain")
                .set(Tables.DOMAIN.DESCRIPTION, "Description")
                .set(Tables.DOMAIN.CREATED_BY_USER_ID, 1L)
                .execute();
    }

    @Test
    void shouldCreateUpdateDeleteAndQueryComponentTypes() {
        ComponentType created = repository.createComponentType(ComponentType.builder()
                        .domainId(1L)
                        .name("Switch")
                        .code("SW")
                        .description("Switches")
                        .orderIndex(1)
                        .build())
                .orElseThrow();

        assertThat(repository.existsById(created.id())).isTrue();
        assertThat(repository.existsByNameAndDomainId("Switch", 1L)).isTrue();

        dslContext.insertInto(Tables.ATTRIBUTE_DEFINITION)
                .set(Tables.ATTRIBUTE_DEFINITION.ID, 1L)
                .set(Tables.ATTRIBUTE_DEFINITION.COMPONENT_TYPE_ID, created.id())
                .set(Tables.ATTRIBUTE_DEFINITION.NAME, "stem")
                .set(Tables.ATTRIBUTE_DEFINITION.LABEL, "Stem")
                .set(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE, "ENUM")
                .set(Tables.ATTRIBUTE_DEFINITION.ENUM_VALUES_JSON, "[\"MX\",\"ALPS\"]")
                .set(Tables.ATTRIBUTE_DEFINITION.IS_REQUIRED, true)
                .execute();

        dslContext.insertInto(Tables.COMPONENT)
                .set(Tables.COMPONENT.ID, 1L)
                .set(Tables.COMPONENT.COMPONENT_TYPE_ID, created.id())
                .set(Tables.COMPONENT.NAME, "Gateron Yellow")
                .execute();

        ComponentType fetched = repository.getComponentTypeById(created.id()).orElseThrow();
        assertThat(fetched.domain()).isNotNull();
        assertThat(fetched.domain().id()).isEqualTo(1L);
        assertThat(fetched.attributeDefinitions()).hasSize(1);
        assertThat(fetched.attributeDefinitions().get(0).enumValues()).containsExactlyInAnyOrder("MX", "ALPS");
        assertThat(fetched.components()).hasSize(1);
        assertThat(fetched.components().get(0).getName()).isEqualTo("Gateron Yellow");

        ComponentType updated = repository.updateComponentType(created.id(), ComponentType.builder()
                        .id(created.id())
                        .domainId(1L)
                        .name("Keycap")
                        .code("KC")
                        .description("Keycaps")
                        .orderIndex(2)
                        .build())
                .orElseThrow();
        assertThat(updated.name()).isEqualTo("Keycap");

        assertThat(repository.getComponentTypesByDomainId(1L)).hasSize(1);

        dslContext.deleteFrom(Tables.ATTRIBUTE_DEFINITION)
                .where(Tables.ATTRIBUTE_DEFINITION.COMPONENT_TYPE_ID.eq(created.id()))
                .execute();
        dslContext.deleteFrom(Tables.COMPONENT)
                .where(Tables.COMPONENT.COMPONENT_TYPE_ID.eq(created.id()))
                .execute();

        repository.deleteComponentTypeById(created.id());
        assertThat(repository.existsById(created.id())).isFalse();
    }
}
