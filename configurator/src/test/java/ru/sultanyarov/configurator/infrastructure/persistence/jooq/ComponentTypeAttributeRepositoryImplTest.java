package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.ComponentTypeAttribute;

class ComponentTypeAttributeRepositoryImplTest extends AbstractJooqRepositoryTest {

  private ComponentTypeAttributeRepositoryImpl repository;

  @BeforeEach
  void setUpRepository() {
    repository = new ComponentTypeAttributeRepositoryImpl(dslContext);
    dslContext
        .insertInto(Tables.DOMAIN)
        .set(Tables.DOMAIN.ID, 1L)
        .set(Tables.DOMAIN.NAME, "Domain")
        .set(Tables.DOMAIN.CREATED_BY_USER_ID, 1L)
        .execute();
    dslContext
        .insertInto(Tables.COMPONENT_TYPE)
        .set(Tables.COMPONENT_TYPE.ID, 10L)
        .set(Tables.COMPONENT_TYPE.DOMAIN_ID, 1L)
        .set(Tables.COMPONENT_TYPE.NAME, "Processor")
        .execute();
    dslContext
        .insertInto(Tables.COMPONENT_TYPE)
        .set(Tables.COMPONENT_TYPE.ID, 20L)
        .set(Tables.COMPONENT_TYPE.DOMAIN_ID, 1L)
        .set(Tables.COMPONENT_TYPE.NAME, "Motherboard")
        .execute();
    dslContext
        .insertInto(Tables.ATTRIBUTE_DEFINITION)
        .set(Tables.ATTRIBUTE_DEFINITION.ID, 101L)
        .set(Tables.ATTRIBUTE_DEFINITION.DOMAIN_ID, 1L)
        .set(Tables.ATTRIBUTE_DEFINITION.NAME, "socket")
        .set(Tables.ATTRIBUTE_DEFINITION.LABEL, "Socket")
        .set(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE, "STRING")
        .execute();
  }

  @Test
  void shouldCreateUpdateListAndDeleteLinks() {
    ComponentTypeAttribute created =
        repository
            .save(
                ComponentTypeAttribute.builder()
                    .componentTypeId(10L)
                    .attributeDefinitionId(101L)
                    .isRequired(true)
                    .orderIndex(1)
                    .build())
            .orElseThrow();

    assertThat(created.isRequired()).isTrue();
    assertThat(repository.exists(10L, 101L)).isTrue();

    ComponentTypeAttribute updated =
        repository
            .save(
                ComponentTypeAttribute.builder()
                    .componentTypeId(10L)
                    .attributeDefinitionId(101L)
                    .isRequired(false)
                    .orderIndex(3)
                    .build())
            .orElseThrow();
    repository.save(
        ComponentTypeAttribute.builder()
            .componentTypeId(20L)
            .attributeDefinitionId(101L)
            .isRequired(true)
            .orderIndex(2)
            .build());

    assertThat(updated.isRequired()).isFalse();
    assertThat(updated.orderIndex()).isEqualTo(3);
    assertThat(repository.getComponentTypeIdsByAttributeDefinitionId(101L))
        .containsExactly(10L, 20L);

    assertThat(repository.delete(10L, 101L)).isTrue();
    assertThat(repository.exists(10L, 101L)).isFalse();
    assertThat(repository.delete(10L, 101L)).isFalse();
  }
}
