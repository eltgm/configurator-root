package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.Page;

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

        dslContext.insertInto(Tables.COMPONENT_TYPE)
                .set(Tables.COMPONENT_TYPE.ID, 2L)
                .set(Tables.COMPONENT_TYPE.DOMAIN_ID, 1L)
                .set(Tables.COMPONENT_TYPE.NAME, "Keycap")
                .execute();

        dslContext.insertInto(Tables.DOMAIN)
                .set(Tables.DOMAIN.ID, 2L)
                .set(Tables.DOMAIN.NAME, "Other domain")
                .set(Tables.DOMAIN.CREATED_BY_USER_ID, 1L)
                .execute();

        dslContext.insertInto(Tables.COMPONENT_TYPE)
                .set(Tables.COMPONENT_TYPE.ID, 3L)
                .set(Tables.COMPONENT_TYPE.DOMAIN_ID, 2L)
                .set(Tables.COMPONENT_TYPE.NAME, "Other type")
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

    @Test
    void shouldFindOnlyDomainComponentsAndReturnFilteredTotalItems() {
        insertComponent(1L, 1L, "Switch A");
        insertComponent(2L, 1L, "Switch B");
        insertComponent(3L, 2L, "Keycap");
        insertComponent(4L, 3L, "Other domain component");

        Page<Component> page = repository.findPageByDomainIdComponentTypeIdName(1L, null, null, 0, 2);

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(2);
        assertThat(page.totalItems()).isEqualTo(3);
        assertThat(page.items())
                .extracting(Component::getId)
                .containsExactly(1L, 2L);
    }

    @Test
    void shouldFilterDomainComponentsByComponentTypeAndName() {
        insertComponent(1L, 1L, "Switch A");
        insertComponent(2L, 1L, "Switch B");
        insertComponent(3L, 2L, "Switch A");
        insertComponent(4L, 3L, "Switch A");

        Page<Component> page =
                repository.findPageByDomainIdComponentTypeIdName(1L, 1L, "Switch A", 0, 10);

        assertThat(page.totalItems()).isEqualTo(1);
        assertThat(page.items()).singleElement()
                .satisfies(component -> {
                    assertThat(component.getId()).isEqualTo(1L);
                    assertThat(component.getComponentTypeId()).isEqualTo(1L);
                    assertThat(component.getName()).isEqualTo("Switch A");
                });
    }

    @Test
    void shouldReturnRequestedPageInStableIdOrder() {
        insertComponent(1L, 1L, "Switch A");
        insertComponent(2L, 1L, "Switch B");
        insertComponent(3L, 2L, "Keycap");

        Page<Component> page =
                repository.findPageByDomainIdComponentTypeIdName(1L, null, null, 1, 2);

        assertThat(page.totalItems()).isEqualTo(3);
        assertThat(page.items())
                .extracting(Component::getId)
                .containsExactly(3L);
    }

    private void insertComponent(Long id, Long componentTypeId, String name) {
        dslContext.insertInto(Tables.COMPONENT)
                .set(Tables.COMPONENT.ID, id)
                .set(Tables.COMPONENT.COMPONENT_TYPE_ID, componentTypeId)
                .set(Tables.COMPONENT.NAME, name)
                .execute();
    }
}
