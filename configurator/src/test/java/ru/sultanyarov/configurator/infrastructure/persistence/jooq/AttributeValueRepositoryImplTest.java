package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.AttributeValue;
import ru.sultanyarov.configurator.domain.model.DataType;

class AttributeValueRepositoryImplTest extends AbstractJooqRepositoryTest {

  private AttributeValueRepositoryImpl repository;

  @BeforeEach
  void setUpRepository() {
    repository = new AttributeValueRepositoryImpl(dslContext);

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

    dslContext
        .insertInto(Tables.COMPONENT)
        .set(Tables.COMPONENT.ID, 1L)
        .set(Tables.COMPONENT.COMPONENT_TYPE_ID, 1L)
        .set(Tables.COMPONENT.NAME, "Component")
        .execute();

    dslContext
        .insertInto(Tables.ATTRIBUTE_DEFINITION)
        .set(Tables.ATTRIBUTE_DEFINITION.ID, 1L)
        .set(Tables.ATTRIBUTE_DEFINITION.DOMAIN_ID, 1L)
        .set(Tables.ATTRIBUTE_DEFINITION.NAME, "name")
        .set(Tables.ATTRIBUTE_DEFINITION.LABEL, "Name")
        .set(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE, "STRING")
        .execute();
    dslContext
        .insertInto(Tables.ATTRIBUTE_DEFINITION)
        .set(Tables.ATTRIBUTE_DEFINITION.ID, 2L)
        .set(Tables.ATTRIBUTE_DEFINITION.DOMAIN_ID, 1L)
        .set(Tables.ATTRIBUTE_DEFINITION.NAME, "force")
        .set(Tables.ATTRIBUTE_DEFINITION.LABEL, "Force")
        .set(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE, "NUMBER")
        .execute();
    dslContext
        .insertInto(Tables.ATTRIBUTE_DEFINITION)
        .set(Tables.ATTRIBUTE_DEFINITION.ID, 3L)
        .set(Tables.ATTRIBUTE_DEFINITION.DOMAIN_ID, 1L)
        .set(Tables.ATTRIBUTE_DEFINITION.NAME, "clicky")
        .set(Tables.ATTRIBUTE_DEFINITION.LABEL, "Clicky")
        .set(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE, "BOOLEAN")
        .execute();
  }

  @Test
  void shouldReturnEmptyListWhenNoAttributesProvided() {
    assertThat(repository.createAttributeValues(List.of(), 1L)).isEmpty();
  }

  @Test
  void shouldPersistAndReadBackAttributeValuesOfDifferentTypes() {
    List<AttributeValue> created =
        repository.createAttributeValues(
            List.of(
                AttributeValue.builder()
                    .attributeDefinitionId(1L)
                    .dataType(DataType.STRING)
                    .value("linear")
                    .build(),
                AttributeValue.builder()
                    .attributeDefinitionId(2L)
                    .dataType(DataType.NUMBER)
                    .value("45")
                    .build(),
                AttributeValue.builder()
                    .attributeDefinitionId(3L)
                    .dataType(DataType.BOOLEAN)
                    .value("true")
                    .build()),
            1L);

    assertThat(created).hasSize(3);
    assertThat(created)
        .extracting(AttributeValue::value)
        .containsExactlyInAnyOrder("linear", "45", "true");
    assertThat(created)
        .extracting(AttributeValue::name)
        .containsExactlyInAnyOrder("name", "force", "clicky");
  }

  @Test
  void shouldDeleteAllAttributeValuesByComponentId() {
    repository.createAttributeValues(
        List.of(
            AttributeValue.builder()
                .attributeDefinitionId(1L)
                .dataType(DataType.STRING)
                .value("linear")
                .build(),
            AttributeValue.builder()
                .attributeDefinitionId(2L)
                .dataType(DataType.NUMBER)
                .value("45")
                .build()),
        1L);

    repository.deleteByComponentId(1L);

    assertThat(
            dslContext.fetchCount(
                Tables.ATTRIBUTE_VALUE, Tables.ATTRIBUTE_VALUE.COMPONENT_ID.eq(1L)))
        .isZero();
  }

  @Test
  void shouldDeleteDefinitionValuesOnlyForSelectedComponentType() {
    dslContext
        .insertInto(Tables.COMPONENT_TYPE)
        .set(Tables.COMPONENT_TYPE.ID, 2L)
        .set(Tables.COMPONENT_TYPE.DOMAIN_ID, 1L)
        .set(Tables.COMPONENT_TYPE.NAME, "Other type")
        .execute();
    dslContext
        .insertInto(Tables.COMPONENT)
        .set(Tables.COMPONENT.ID, 2L)
        .set(Tables.COMPONENT.COMPONENT_TYPE_ID, 2L)
        .set(Tables.COMPONENT.NAME, "Other component")
        .execute();
    repository.createAttributeValues(
        List.of(
            AttributeValue.builder()
                .attributeDefinitionId(1L)
                .dataType(DataType.STRING)
                .value("first")
                .build()),
        1L);
    repository.createAttributeValues(
        List.of(
            AttributeValue.builder()
                .attributeDefinitionId(1L)
                .dataType(DataType.STRING)
                .value("second")
                .build()),
        2L);

    repository.deleteByAttributeDefinitionIdAndComponentTypeId(1L, 1L);

    assertThat(
            dslContext.fetchCount(
                Tables.ATTRIBUTE_VALUE, Tables.ATTRIBUTE_VALUE.COMPONENT_ID.eq(1L)))
        .isZero();
    assertThat(
            dslContext.fetchCount(
                Tables.ATTRIBUTE_VALUE, Tables.ATTRIBUTE_VALUE.COMPONENT_ID.eq(2L)))
        .isEqualTo(1);
  }
}
