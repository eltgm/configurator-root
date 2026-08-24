package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.ComponentImage;
import ru.sultanyarov.configurator.domain.model.DataType;
import ru.sultanyarov.configurator.domain.model.Page;

class ComponentRepositoryImplTest extends AbstractJooqRepositoryTest {

  private ComponentRepositoryImpl repository;

  @BeforeEach
  void setUpRepository() {
    repository = new ComponentRepositoryImpl(dslContext);

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
        .insertInto(Tables.COMPONENT_TYPE)
        .set(Tables.COMPONENT_TYPE.ID, 2L)
        .set(Tables.COMPONENT_TYPE.DOMAIN_ID, 1L)
        .set(Tables.COMPONENT_TYPE.NAME, "Keycap")
        .execute();

    dslContext
        .insertInto(Tables.DOMAIN)
        .set(Tables.DOMAIN.ID, 2L)
        .set(Tables.DOMAIN.NAME, "Other domain")
        .set(Tables.DOMAIN.CREATED_BY_USER_ID, 1L)
        .execute();

    dslContext
        .insertInto(Tables.COMPONENT_TYPE)
        .set(Tables.COMPONENT_TYPE.ID, 3L)
        .set(Tables.COMPONENT_TYPE.DOMAIN_ID, 2L)
        .set(Tables.COMPONENT_TYPE.NAME, "Other type")
        .execute();
  }

  @Test
  void shouldCreateComponentAndMapReturnedRecord() {
    Component created =
        repository
            .createComponent(
                Component.builder()
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
  void shouldGetComponentWithAttributeValuesAndImages() {
    insertComponent(7L, 1L, "Switch");
    dslContext
        .insertInto(Tables.ATTRIBUTE_DEFINITION)
        .set(Tables.ATTRIBUTE_DEFINITION.ID, 11L)
        .set(Tables.ATTRIBUTE_DEFINITION.COMPONENT_TYPE_ID, 1L)
        .set(Tables.ATTRIBUTE_DEFINITION.NAME, "force")
        .set(Tables.ATTRIBUTE_DEFINITION.LABEL, "Force")
        .set(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE, DataType.NUMBER.name())
        .set(Tables.ATTRIBUTE_DEFINITION.ORDER_INDEX, 1)
        .execute();
    dslContext
        .insertInto(Tables.ATTRIBUTE_VALUE)
        .set(Tables.ATTRIBUTE_VALUE.COMPONENT_ID, 7L)
        .set(Tables.ATTRIBUTE_VALUE.ATTRIBUTE_DEFINITION_ID, 11L)
        .set(Tables.ATTRIBUTE_VALUE.VALUE_NUMBER, java.math.BigInteger.valueOf(55))
        .execute();
    dslContext
        .insertInto(Tables.COMPONENT_IMAGE)
        .set(Tables.COMPONENT_IMAGE.COMPONENT_ID, 7L)
        .set(Tables.COMPONENT_IMAGE.FILE_PATH, "components/7/main.jpg")
        .set(Tables.COMPONENT_IMAGE.ORDER_INDEX, 1)
        .execute();

    Component component = repository.getById(7L).orElseThrow();

    assertThat(component.getAttributes())
        .singleElement()
        .satisfies(
            attribute -> {
              assertThat(attribute.attributeDefinitionId()).isEqualTo(11L);
              assertThat(attribute.name()).isEqualTo("force");
              assertThat(attribute.label()).isEqualTo("Force");
              assertThat(attribute.dataType()).isEqualTo(DataType.NUMBER);
              assertThat(attribute.value()).isEqualTo("55");
            });
    assertThat(component.getImages())
        .singleElement()
        .satisfies(
            image -> {
              assertThat(image.componentId()).isEqualTo(7L);
              assertThat(image.objectKey()).isEqualTo("components/7/main.jpg");
              assertThat(image.orderIndex()).isEqualTo(1);
            });
  }

  @Test
  void shouldGetComponentImagesOrderedByOrderIndexWithNullLastAndIdAsTieBreaker() {
    insertComponent(7L, 1L, "Switch");
    insertImage(44L, 7L, "/second-two.png", 2);
    insertImage(42L, 7L, "/zero.png", 0);
    insertImage(41L, 7L, "/unordered.png", null);
    insertImage(43L, 7L, "/first-two.png", 2);

    Component component = repository.getById(7L).orElseThrow();

    assertThat(component.getImages())
        .extracting(ComponentImage::id)
        .containsExactly(42L, 43L, 44L, 41L);
  }

  @Test
  void shouldUpdateOnlyEditableComponentFields() {
    insertComponent(7L, 1L, "Switch");
    Component beforeUpdate = repository.getById(7L).orElseThrow();

    Component updated =
        repository
            .updateComponent(
                7L,
                Component.builder()
                    .componentTypeId(2L)
                    .name("Updated switch")
                    .brand("Brand")
                    .description("Description")
                    .archived(true)
                    .build())
            .orElseThrow();

    assertThat(updated.getName()).isEqualTo("Updated switch");
    assertThat(updated.getBrand()).isEqualTo("Brand");
    assertThat(updated.getDescription()).isEqualTo("Description");
    assertThat(updated.getComponentTypeId()).isEqualTo(1L);
    assertThat(updated.getArchived()).isEqualTo(beforeUpdate.getArchived());
    assertThat(updated.getCreatedAt()).isEqualTo(beforeUpdate.getCreatedAt());
  }

  @Test
  void shouldReturnEmptyWhenComponentDoesNotExist() {
    assertThat(repository.getById(999L)).isEmpty();
    assertThat(repository.updateComponent(999L, Component.builder().name("Missing").build()))
        .isEmpty();
  }

  @Test
  void shouldFindOnlyDomainComponentsAndReturnFilteredTotalItems() {
    insertComponent(1L, 1L, "Switch A");
    insertComponent(2L, 1L, "Switch B");
    insertComponent(3L, 2L, "Keycap");
    insertComponent(4L, 3L, "Other domain component");

    Page<Component> page =
        repository.findPageByDomainIdComponentTypeIdNameArchived(1L, null, null, null, 0, 2);

    assertThat(page.page()).isZero();
    assertThat(page.size()).isEqualTo(2);
    assertThat(page.totalItems()).isEqualTo(3);
    assertThat(page.items()).extracting(Component::getId).containsExactly(1L, 2L);
  }

  @Test
  void shouldFilterDomainComponentsByComponentTypeAndName() {
    insertComponent(1L, 1L, "Switch A");
    insertComponent(2L, 1L, "Switch B");
    insertComponent(3L, 2L, "Switch A");
    insertComponent(4L, 3L, "Switch A");

    Page<Component> page =
        repository.findPageByDomainIdComponentTypeIdNameArchived(1L, 1L, "Switch A", null, 0, 10);

    assertThat(page.totalItems()).isEqualTo(1);
    assertThat(page.items())
        .singleElement()
        .satisfies(
            component -> {
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
        repository.findPageByDomainIdComponentTypeIdNameArchived(1L, null, null, null, 1, 2);

    assertThat(page.totalItems()).isEqualTo(3);
    assertThat(page.items()).extracting(Component::getId).containsExactly(3L);
  }

  @Test
  void shouldFilterDomainComponentsByArchiveStatus() {
    insertComponent(1L, 1L, "Active switch");
    insertComponent(2L, 1L, "Archived switch");
    repository.archiveComponentById(2L);

    Page<Component> activePage =
        repository.findPageByDomainIdComponentTypeIdNameArchived(1L, null, null, false, 0, 10);
    Page<Component> archivedPage =
        repository.findPageByDomainIdComponentTypeIdNameArchived(1L, null, null, true, 0, 10);
    Page<Component> unfilteredPage =
        repository.findPageByDomainIdComponentTypeIdNameArchived(1L, null, null, null, 0, 10);

    assertThat(activePage.items()).extracting(Component::getId).containsExactly(1L);
    assertThat(archivedPage.items()).extracting(Component::getId).containsExactly(2L);
    assertThat(unfilteredPage.items()).extracting(Component::getId).containsExactly(1L, 2L);
  }

  @Test
  void shouldGetById() {
    insertComponent(1L, 1L, "Switch A");
    insertComponent(2L, 2L, "Keycap");

    assertThat(repository.getById(2L))
        .hasValueSatisfying(
            component -> {
              assertThat(component.getId()).isEqualTo(2L);
              assertThat(component.getComponentTypeId()).isEqualTo(2L);
              assertThat(component.getName()).isEqualTo("Keycap");
            });
  }

  @Test
  void shouldReturnEmptyWhenComponentByIdDoesNotExist() {
    assertThat(repository.getById(404L)).isEmpty();
  }

  @Test
  void shouldArchiveComponentAndPreserveAllRelatedData() {
    insertComponent(7L, 1L, "Switch");
    insertComponent(8L, 2L, "Keycap");
    dslContext
        .insertInto(Tables.ATTRIBUTE_DEFINITION)
        .set(Tables.ATTRIBUTE_DEFINITION.ID, 11L)
        .set(Tables.ATTRIBUTE_DEFINITION.COMPONENT_TYPE_ID, 1L)
        .set(Tables.ATTRIBUTE_DEFINITION.NAME, "force")
        .set(Tables.ATTRIBUTE_DEFINITION.LABEL, "Force")
        .set(Tables.ATTRIBUTE_DEFINITION.DATA_TYPE, DataType.NUMBER.name())
        .execute();
    dslContext
        .insertInto(Tables.ATTRIBUTE_VALUE)
        .set(Tables.ATTRIBUTE_VALUE.COMPONENT_ID, 7L)
        .set(Tables.ATTRIBUTE_VALUE.ATTRIBUTE_DEFINITION_ID, 11L)
        .set(Tables.ATTRIBUTE_VALUE.VALUE_NUMBER, java.math.BigInteger.valueOf(55))
        .execute();
    dslContext
        .insertInto(Tables.COMPONENT_IMAGE)
        .set(Tables.COMPONENT_IMAGE.COMPONENT_ID, 7L)
        .set(Tables.COMPONENT_IMAGE.FILE_PATH, "components/7/main.jpg")
        .execute();
    dslContext
        .insertInto(Tables.COMPATIBILITY_LINK)
        .set(Tables.COMPATIBILITY_LINK.DOMAIN_ID, 1L)
        .set(Tables.COMPATIBILITY_LINK.COMPONENT_A_ID, 7L)
        .set(Tables.COMPATIBILITY_LINK.COMPONENT_B_ID, 8L)
        .execute();
    dslContext
        .insertInto(Tables.APP_USER)
        .set(Tables.APP_USER.ID, 1L)
        .set(Tables.APP_USER.EMAIL, "user@example.com")
        .set(Tables.APP_USER.PASSWORD_HASH, "hash")
        .execute();
    dslContext
        .insertInto(Tables.CONFIGURATION)
        .set(Tables.CONFIGURATION.ID, 21L)
        .set(Tables.CONFIGURATION.DOMAIN_ID, 1L)
        .set(Tables.CONFIGURATION.NAME, "Configuration")
        .set(Tables.CONFIGURATION.CREATED_BY_USER_ID, 1L)
        .execute();
    dslContext
        .insertInto(Tables.CONFIGURATION_COMPONENT)
        .set(Tables.CONFIGURATION_COMPONENT.CONFIGURATION_ID, 21L)
        .set(Tables.CONFIGURATION_COMPONENT.COMPONENT_ID, 7L)
        .execute();

    boolean archived = repository.archiveComponentById(7L);

    assertThat(archived).isTrue();
    assertThat(
            dslContext
                .select(Tables.COMPONENT.ARCHIVED)
                .from(Tables.COMPONENT)
                .where(Tables.COMPONENT.ID.eq(7L))
                .fetchOne(Tables.COMPONENT.ARCHIVED))
        .isTrue();
    assertThat(
            dslContext.fetchCount(
                Tables.ATTRIBUTE_VALUE, Tables.ATTRIBUTE_VALUE.COMPONENT_ID.eq(7L)))
        .isOne();
    assertThat(
            dslContext.fetchCount(
                Tables.COMPONENT_IMAGE, Tables.COMPONENT_IMAGE.COMPONENT_ID.eq(7L)))
        .isOne();
    assertThat(
            dslContext.fetchCount(
                Tables.COMPATIBILITY_LINK,
                Tables.COMPATIBILITY_LINK
                    .COMPONENT_A_ID
                    .eq(7L)
                    .or(Tables.COMPATIBILITY_LINK.COMPONENT_B_ID.eq(7L))))
        .isOne();
    assertThat(
            dslContext.fetchCount(
                Tables.CONFIGURATION_COMPONENT, Tables.CONFIGURATION_COMPONENT.COMPONENT_ID.eq(7L)))
        .isOne();
  }

  @Test
  void shouldReturnFalseWhenArchivingNonExistentComponent() {
    assertThat(repository.archiveComponentById(404L)).isFalse();
  }

  @Test
  void shouldRestoreArchivedComponent() {
    insertComponent(7L, 1L, "Switch");
    repository.archiveComponentById(7L);

    assertThat(repository.restoreComponentById(7L)).isTrue();
    assertThat(repository.getById(7L).orElseThrow().getArchived()).isFalse();
  }

  @Test
  void shouldReturnFalseWhenRestoringNonExistentComponent() {
    assertThat(repository.restoreComponentById(404L)).isFalse();
  }

  @Test
  void shouldCreateComponentImageMetadata() {
    insertComponent(7L, 1L, "Switch");

    ComponentImage createdImage =
        repository
            .createImage(
                ComponentImage.builder()
                    .componentId(7L)
                    .objectKey("components/7/image.png")
                    .orderIndex(3)
                    .build())
            .orElseThrow();

    assertThat(createdImage.id()).isNotNull();
    assertThat(createdImage.componentId()).isEqualTo(7L);
    assertThat(createdImage.objectKey()).isEqualTo("components/7/image.png");
    assertThat(createdImage.orderIndex()).isEqualTo(3);
  }

  @Test
  void shouldGetComponentImageMetadataById() {
    insertComponent(7L, 1L, "Switch");
    insertImage(42L, 7L, "components/7/image.webp", 4);

    assertThat(repository.getImageById(42L))
        .hasValueSatisfying(
            image -> {
              assertThat(image.id()).isEqualTo(42L);
              assertThat(image.componentId()).isEqualTo(7L);
              assertThat(image.objectKey()).isEqualTo("components/7/image.webp");
              assertThat(image.orderIndex()).isEqualTo(4);
            });
  }

  @Test
  void shouldReturnEmptyWhenComponentImageDoesNotExist() {
    assertThat(repository.getImageById(404L)).isEmpty();
  }

  @Test
  void shouldDeleteComponentImageMetadataById() {
    insertComponent(7L, 1L, "Switch");
    insertImage(42L, 7L, "components/7/image.webp", 4);

    assertThat(repository.deleteImageById(42L)).isTrue();
    assertThat(repository.getImageById(42L)).isEmpty();
  }

  @Test
  void shouldReturnFalseWhenDeletingMissingComponentImageMetadata() {
    assertThat(repository.deleteImageById(404L)).isFalse();
  }

  @Test
  void shouldUpdateCompleteComponentImageOrderWithoutTouchingForeignImages() {
    insertComponent(7L, 1L, "Switch");
    insertComponent(8L, 1L, "Other switch");
    insertImage(41L, 7L, "components/7/first.webp", 5);
    insertImage(42L, 7L, "components/7/second.webp", null);
    insertImage(43L, 7L, "components/7/third.webp", 2);
    insertImage(44L, 8L, "components/8/foreign.webp", 9);

    assertThat(repository.updateImageOrder(7L, java.util.List.of(43L, 41L, 42L))).isEqualTo(3);
    assertThat(repository.getById(7L).orElseThrow().getImages())
        .extracting(ComponentImage::id, ComponentImage::orderIndex)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple(43L, 0),
            org.assertj.core.groups.Tuple.tuple(41L, 1),
            org.assertj.core.groups.Tuple.tuple(42L, 2));
    assertThat(repository.getImageById(44L).orElseThrow().orderIndex()).isEqualTo(9);
  }

  @Test
  void shouldReturnZeroWhenUpdatingEmptyComponentImageOrder() {
    assertThat(repository.updateImageOrder(7L, java.util.List.of())).isZero();
  }

  @Test
  void shouldCalculateNextComponentImageOrderIndex() {
    insertComponent(7L, 1L, "Switch");
    assertThat(repository.getNextImageOrderIndex(7L)).isZero();
    dslContext
        .insertInto(Tables.COMPONENT_IMAGE)
        .set(Tables.COMPONENT_IMAGE.COMPONENT_ID, 7L)
        .set(Tables.COMPONENT_IMAGE.FILE_PATH, "/first.png")
        .set(Tables.COMPONENT_IMAGE.ORDER_INDEX, 2)
        .execute();
    dslContext
        .insertInto(Tables.COMPONENT_IMAGE)
        .set(Tables.COMPONENT_IMAGE.COMPONENT_ID, 7L)
        .set(Tables.COMPONENT_IMAGE.FILE_PATH, "/second.png")
        .set(Tables.COMPONENT_IMAGE.ORDER_INDEX, 5)
        .execute();

    assertThat(repository.getNextImageOrderIndex(7L)).isEqualTo(6);
  }

  private void insertComponent(Long id, Long componentTypeId, String name) {
    dslContext
        .insertInto(Tables.COMPONENT)
        .set(Tables.COMPONENT.ID, id)
        .set(Tables.COMPONENT.COMPONENT_TYPE_ID, componentTypeId)
        .set(Tables.COMPONENT.NAME, name)
        .execute();
  }

  private void insertImage(Long id, Long componentId, String path, Integer orderIndex) {
    dslContext
        .insertInto(Tables.COMPONENT_IMAGE)
        .set(Tables.COMPONENT_IMAGE.ID, id)
        .set(Tables.COMPONENT_IMAGE.COMPONENT_ID, componentId)
        .set(Tables.COMPONENT_IMAGE.FILE_PATH, path)
        .set(Tables.COMPONENT_IMAGE.ORDER_INDEX, orderIndex)
        .execute();
  }
}
