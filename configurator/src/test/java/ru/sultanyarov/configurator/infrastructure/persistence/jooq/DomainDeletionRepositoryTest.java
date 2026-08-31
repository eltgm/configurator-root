package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import static org.assertj.core.api.Assertions.*;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.*;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DomainDeletionRepositoryTest extends AbstractJooqRepositoryTest {
  DomainRepositoryImpl domains;
  ConfigurationRepositoryImpl configurations;
  ComponentImageCleanupRepositoryImpl cleanup;

  @BeforeEach
  void seed() {
    domains = new DomainRepositoryImpl(dslContext);
    configurations = new ConfigurationRepositoryImpl(dslContext);
    cleanup = new ComponentImageCleanupRepositoryImpl(dslContext);
    dslContext.execute(
        "INSERT INTO domain (id, name, created_by_user_id) VALUES (1, 'Deleted', -1), (2, 'Retained', -1)");
    dslContext.execute(
        "INSERT INTO component_type (id, domain_id, name) VALUES (10, 1, 'A'), (20, 1, 'B'), (30, 2, 'C')");
    dslContext.execute(
        "INSERT INTO component (id, component_type_id, name, archived) VALUES (100, 10, 'Active', false), (200, 20, 'Archived', true), (300, 30, 'Retained', false)");
    dslContext.execute(
        "INSERT INTO attribute_definition (id, domain_id, name, label, data_type) VALUES (1001, 1, 'socket', 'Socket', 'STRING'), (2001, 2, 'socket', 'Socket', 'STRING')");
    dslContext.execute(
        "INSERT INTO component_type_attribute (component_type_id, attribute_definition_id) VALUES (10, 1001), (20, 1001), (30, 2001)");
    dslContext.execute(
        "INSERT INTO attribute_value (component_id, attribute_definition_id, value_string) VALUES (100, 1001, 'AM5'), (200, 1001, 'AM5'), (300, 2001, 'AM4')");
    dslContext.execute(
        "INSERT INTO compatibility_rule_set (id, domain_id, name, component_type_a_id, component_type_b_id) VALUES (1, 1, 'Match', 10, 20)");
    dslContext.execute(
        "INSERT INTO compatibility_rule_condition (rule_set_id, left_attribute_definition_id, right_attribute_definition_id, operator) VALUES (1, 1001, 1001, 'EQUALS')");
    dslContext.execute(
        "INSERT INTO compatibility_link (domain_id, component_a_id, component_b_id) VALUES (1, 100, 200)");
    dslContext.execute(
        "INSERT INTO component_image (component_id, file_path) VALUES (100, 'active.png'), (200, 'archived.png'), (300, 'retained.png')");
  }

  @Test
  void shouldDeleteOwnedCatalogAndQueueOnlyItsImages() {
    assertThat(domains.lockById(1L)).isTrue();
    assertThat(domains.lockById(999L)).isFalse();
    assertThat(configurations.existsByDomainId(1L)).isFalse();
    domains.lockContentsByDomainId(1L);
    cleanup.enqueueByDomainId(1L);
    cleanup.enqueueByDomainId(1L); // A repeated enqueue cannot create duplicate jobs.
    domains.deleteContentsByDomainId(1L);
    domains.deleteDomainById(1L);

    assertThat(dslContext.fetch(DOMAIN).getValues(DOMAIN.ID)).containsExactly(2L);
    assertThat(dslContext.fetch(COMPONENT).getValues(COMPONENT.ID)).containsExactly(300L);
    assertThat(dslContext.fetch(COMPONENT_TYPE).getValues(COMPONENT_TYPE.ID)).containsExactly(30L);
    assertThat(dslContext.fetch(ATTRIBUTE_DEFINITION).getValues(ATTRIBUTE_DEFINITION.ID))
        .containsExactly(2001L);
    assertThat(dslContext.fetchCount(ATTRIBUTE_VALUE)).isEqualTo(1);
    assertThat(dslContext.fetchCount(COMPONENT_TYPE_ATTRIBUTE)).isEqualTo(1);
    assertThat(dslContext.fetchCount(COMPATIBILITY_RULE_SET)).isZero();
    assertThat(dslContext.fetchCount(COMPATIBILITY_RULE_CONDITION)).isZero();
    assertThat(dslContext.fetchCount(COMPATIBILITY_LINK)).isZero();
    assertThat(dslContext.fetch(COMPONENT_IMAGE).getValues(COMPONENT_IMAGE.FILE_PATH))
        .containsExactly("retained.png");
    assertThat(cleanup.findDue(Instant.now().plusSeconds(1), 100))
        .containsExactlyInAnyOrder("active.png", "archived.png");
  }

  @Test
  void shouldRetainFailedJobsAcrossRepositoryRecreationAndRespectRetryTimeAndBatchLimit() {
    cleanup.enqueueByDomainId(1L);
    Instant retryAt = Instant.now().plusSeconds(60);
    cleanup.retryLater("active.png", retryAt);
    var restarted = new ComponentImageCleanupRepositoryImpl(dslContext);
    assertThat(restarted.findDue(Instant.now().plusSeconds(1), 100))
        .containsExactly("archived.png");
    assertThat(restarted.findDue(retryAt.plusSeconds(1), 1)).hasSize(1);
    assertThat(
            dslContext
                .fetchOne(
                    COMPONENT_IMAGE_CLEANUP, COMPONENT_IMAGE_CLEANUP.OBJECT_KEY.eq("active.png"))
                .getAttempts())
        .isEqualTo(1);
    restarted.complete("archived.png");
    restarted.complete("archived.png");
    assertThat(restarted.findDue(retryAt.plusSeconds(1), 100)).containsExactly("active.png");
  }

  @Test
  void shouldFindEmptyForeignConfigurationsAndProtectThemAtTheDatabaseBoundary() {
    dslContext.execute(
        "INSERT INTO app_user (id, email, password_hash) VALUES (99, 'other@example.test', 'unused')");
    dslContext.execute(
        "INSERT INTO configuration (domain_id, name, created_by_user_id) VALUES (1, 'Foreign empty', 99)");
    assertThat(configurations.existsByDomainId(1L)).isTrue();
    assertThat(configurations.existsByDomainId(2L)).isFalse();
    // Remove unrelated blockers to specifically exercise the configuration/domain FK.
    domains.deleteContentsByDomainId(1L);
    assertThatThrownBy(() -> domains.deleteDomainById(1L))
        .isInstanceOf(org.jooq.exception.DataAccessException.class);
    assertThat(dslContext.fetchCount(CONFIGURATION)).isEqualTo(1);
    assertThat(domains.existsById(1L)).isTrue();
  }
}
