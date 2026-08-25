package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.DataType;

class ConfiguratorRepositoryImplTest extends AbstractJooqRepositoryTest {
  private ConfiguratorRepositoryImpl repository;

  @BeforeEach
  void setUpRepository() {
    repository = new ConfiguratorRepositoryImpl(dslContext);
    insertDomain(1L, "Domain");
    insertDomain(2L, "Foreign");
    insertComponentType(10L, 1L, "Processor", 2);
    insertComponentType(20L, 1L, "Motherboard", 1);
    insertComponentType(30L, 2L, "Foreign type", 1);
    insertComponent(1L, 10L, "Base", false);
    insertComponent(2L, 20L, "First candidate", false);
    insertComponent(3L, 10L, "Second candidate", false);
    insertComponent(4L, 20L, "Archived", true);
    insertComponent(5L, 30L, "Foreign", false);
    insertStringAttribute(201L, 20L, "socket");
    insertStringValue(301L, 2L, 201L, "AM5");
  }

  @Test
  void getActiveCandidates_shouldReturnDomainComponentsWithAttributesInStableTypeOrder() {
    assertThat(repository.getActiveCandidates(1L, 1L))
        .extracting(Component::getId)
        .containsExactly(2L, 3L);

    Component first = repository.getActiveCandidates(1L, 1L).getFirst();
    assertThat(first.getAttributes())
        .singleElement()
        .satisfies(
            value -> {
              assertThat(value.id()).isEqualTo(301L);
              assertThat(value.attributeDefinitionId()).isEqualTo(201L);
              assertThat(value.dataType()).isEqualTo(DataType.STRING);
              assertThat(value.value()).isEqualTo("AM5");
            });
  }

  @Test
  void getActiveComponents_shouldIncludeBaseAndExcludeArchivedAndForeignComponents() {
    assertThat(repository.getActiveComponents(1L))
        .extracting(Component::getId)
        .containsExactly(2L, 1L, 3L);
  }

  @Test
  void getManualCompatibilityLinks_shouldReturnDetailsForBothDirectionsAndDomainScope() {
    insertCompatibilityLink(1L, 1L, 1L, 2L, "Base on right");
    insertCompatibilityLink(2L, 1L, 2L, 3L, "Base on left");
    insertCompatibilityLink(3L, 2L, 2L, 5L, "Foreign");

    List<CompatibilityLink> result = repository.getManualCompatibilityLinks(1L, 2L);

    assertThat(result)
        .containsExactly(
            new CompatibilityLink(1L, 1L, 1L, 2L, "Base on right"),
            new CompatibilityLink(2L, 1L, 2L, 3L, "Base on left"));
    assertThat(repository.getAllManualCompatibilityLinks(1L)).containsExactlyElementsOf(result);
  }

  private void insertDomain(Long id, String name) {
    dslContext
        .insertInto(Tables.DOMAIN)
        .set(Tables.DOMAIN.ID, id)
        .set(Tables.DOMAIN.NAME, name)
        .set(Tables.DOMAIN.CREATED_BY_USER_ID, 1L)
        .execute();
  }

  private void insertComponentType(Long id, Long domainId, String name, Integer orderIndex) {
    dslContext
        .insertInto(Tables.COMPONENT_TYPE)
        .set(Tables.COMPONENT_TYPE.ID, id)
        .set(Tables.COMPONENT_TYPE.DOMAIN_ID, domainId)
        .set(Tables.COMPONENT_TYPE.NAME, name)
        .set(Tables.COMPONENT_TYPE.ORDER_INDEX, orderIndex)
        .execute();
  }

  private void insertComponent(Long id, Long componentTypeId, String name, boolean archived) {
    dslContext
        .insertInto(Tables.COMPONENT)
        .set(Tables.COMPONENT.ID, id)
        .set(Tables.COMPONENT.COMPONENT_TYPE_ID, componentTypeId)
        .set(Tables.COMPONENT.NAME, name)
        .set(Tables.COMPONENT.ARCHIVED, archived)
        .execute();
  }

  private void insertStringAttribute(Long id, Long componentTypeId, String name) {
    dslContext
        .insertInto(Tables.ATTRIBUTE_DEFINITION)
        .set(Tables.ATTRIBUTE_DEFINITION.ID, id)
        .set(
            Tables.ATTRIBUTE_DEFINITION.DOMAIN_ID,
            dslContext
                .select(Tables.COMPONENT_TYPE.DOMAIN_ID)
                .from(Tables.COMPONENT_TYPE)
                .where(Tables.COMPONENT_TYPE.ID.eq(componentTypeId)))
        .set(Tables.ATTRIBUTE_DEFINITION.NAME, name)
        .set(Tables.ATTRIBUTE_DEFINITION.LABEL, name)
        .set(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE, DataType.STRING.name())
        .execute();
    dslContext
        .insertInto(Tables.COMPONENT_TYPE_ATTRIBUTE)
        .set(Tables.COMPONENT_TYPE_ATTRIBUTE.COMPONENT_TYPE_ID, componentTypeId)
        .set(Tables.COMPONENT_TYPE_ATTRIBUTE.ATTRIBUTE_DEFINITION_ID, id)
        .execute();
  }

  private void insertStringValue(
      Long id, Long componentId, Long attributeDefinitionId, String value) {
    dslContext
        .insertInto(Tables.ATTRIBUTE_VALUE)
        .set(Tables.ATTRIBUTE_VALUE.ID, id)
        .set(Tables.ATTRIBUTE_VALUE.COMPONENT_ID, componentId)
        .set(Tables.ATTRIBUTE_VALUE.ATTRIBUTE_DEFINITION_ID, attributeDefinitionId)
        .set(Tables.ATTRIBUTE_VALUE.VALUE_STRING, value)
        .execute();
  }

  private void insertCompatibilityLink(
      Long id, Long domainId, Long componentAId, Long componentBId, String comment) {
    dslContext
        .insertInto(Tables.COMPATIBILITY_LINK)
        .set(Tables.COMPATIBILITY_LINK.ID, id)
        .set(Tables.COMPATIBILITY_LINK.DOMAIN_ID, domainId)
        .set(Tables.COMPATIBILITY_LINK.COMPONENT_A_ID, componentAId)
        .set(Tables.COMPATIBILITY_LINK.COMPONENT_B_ID, componentBId)
        .set(Tables.COMPATIBILITY_LINK.COMMENT, comment)
        .execute();
  }
}
