package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.Set;
import org.jooq.exception.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.exception.AttributeNameConflictException;
import ru.sultanyarov.configurator.domain.model.AttributeDefinition;
import ru.sultanyarov.configurator.domain.model.DataType;

class AttributeRepositoryImplTest extends AbstractJooqRepositoryTest {

  private AttributeRepositoryImpl repository;

  @BeforeEach
  void setUpRepository() {
    repository = new AttributeRepositoryImpl(dslContext);

    dslContext
        .insertInto(Tables.DOMAIN)
        .set(Tables.DOMAIN.ID, 1L)
        .set(Tables.DOMAIN.NAME, "Domain")
        .set(Tables.DOMAIN.CREATED_BY_USER_ID, 1L)
        .execute();

    dslContext
        .insertInto(Tables.COMPONENT_TYPE)
        .set(Tables.COMPONENT_TYPE.ID, 1L)
        .set(Tables.COMPONENT_TYPE.DOMAIN_ID, 1L)
        .set(Tables.COMPONENT_TYPE.NAME, "Switch")
        .execute();
  }

  @Test
  void shouldCreateUpdateDeleteAndQueryAttributes() {
    AttributeDefinition created =
        repository
            .createAttributeDefinition(
                AttributeDefinition.builder()
                    .domainId(1L)
                    .componentTypeId(1L)
                    .name("stem")
                    .label("Stem")
                    .dataType(DataType.ENUM)
                    .enumValues(Set.of("MX", "ALPS"))
                    .isRequired(true)
                    .orderIndex(1)
                    .build())
            .orElseThrow();

    dslContext
        .insertInto(Tables.COMPONENT_TYPE_ATTRIBUTE)
        .set(Tables.COMPONENT_TYPE_ATTRIBUTE.COMPONENT_TYPE_ID, 1L)
        .set(Tables.COMPONENT_TYPE_ATTRIBUTE.ATTRIBUTE_DEFINITION_ID, created.id())
        .set(Tables.COMPONENT_TYPE_ATTRIBUTE.IS_REQUIRED, true)
        .set(Tables.COMPONENT_TYPE_ATTRIBUTE.ORDER_INDEX, 1)
        .execute();

    assertThat(repository.hasByComponentTypeId(1L)).isTrue();
    assertThat(repository.existsByDomainIdAndName(1L, "stem", null)).isTrue();
    assertThat(repository.existsById(created.id())).isTrue();
    assertThat(repository.getById(created.id())).contains(created);
    assertThat(repository.getByDomainId(1L)).containsExactly(created);
    assertThat(repository.getByComponentTypeId(1L)).hasSize(1);

    AttributeDefinition updated =
        repository
            .updateAttribute(
                created.id(),
                AttributeDefinition.builder()
                    .domainId(1L)
                    .componentTypeId(1L)
                    .name("force")
                    .label("Force")
                    .dataType(DataType.NUMBER)
                    .enumValues(Set.of())
                    .isRequired(false)
                    .orderIndex(2)
                    .createdAt(LocalDateTime.now())
                    .build())
            .orElseThrow();

    assertThat(updated.name()).isEqualTo("force");
    assertThat(updated.dataType()).isEqualTo(DataType.NUMBER);
    assertThat(repository.existsByIdAndDomainId(created.id(), 1L)).isTrue();

    repository.deleteById(created.id());
    assertThat(repository.existsById(created.id())).isFalse();
  }

  @Test
  void shouldEnforceUniquenessOnInsertAndUpdateWithoutLosingExistingData() {
    var socket = repository.createAttributeDefinition(definition("socket")).orElseThrow();
    var other = repository.createAttributeDefinition(definition("other")).orElseThrow();
    assertThat(repository.existsByDomainIdAndName(1L, "socket", socket.id())).isFalse();
    assertThat(repository.existsByDomainIdAndName(2L, "socket", null)).isFalse();
    assertThat(repository.existsByDomainIdAndName(1L, "socket", other.id())).isTrue();
    assertThatThrownBy(() -> repository.createAttributeDefinition(definition("socket")))
        .isInstanceOf(AttributeNameConflictException.class);
    assertThatThrownBy(
            () ->
                dslContext.transaction(
                    configuration ->
                        new AttributeRepositoryImpl(org.jooq.impl.DSL.using(configuration))
                            .updateAttribute(other.id(), definition("socket"))))
        .isInstanceOf(AttributeNameConflictException.class);
    assertThat(repository.getById(other.id()).orElseThrow().name()).isEqualTo("other");
    assertThat(repository.getByDomainId(1L)).hasSize(2);
    assertThat(repository.createAttributeDefinition(definition("Socket"))).isPresent();
    assertThat(repository.getByDomainId(1L)).hasSize(3); // Same label does not hide records.
  }

  @Test
  void shouldNotTranslateUnrelatedConstraintFailuresIntoNameConflicts() {
    assertThatThrownBy(() -> repository.createAttributeDefinition(definition(null)))
        .isInstanceOf(DataAccessException.class);
    var definition = repository.createAttributeDefinition(definition("socket")).orElseThrow();
    assertThatThrownBy(() -> repository.updateAttribute(definition.id(), definition(null)))
        .isInstanceOf(DataAccessException.class);
  }

  private static AttributeDefinition definition(String name) {
    return AttributeDefinition.builder()
        .domainId(1L)
        .name(name)
        .label("Same label")
        .dataType(DataType.STRING)
        .createdAt(LocalDateTime.now())
        .build();
  }
}
