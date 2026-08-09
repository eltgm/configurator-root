package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import static org.jooq.impl.DSL.multiset;
import static ru.sultanyarov.configurator.common.util.PaginationHelper.jooqPage;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT_TYPE;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.CONFIGURATION;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.CONFIGURATION_COMPONENT;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.jooq.SelectField;
import org.jooq.SelectFieldOrAsterisk;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.application.port.out.ConfigurationRepository;
import ru.sultanyarov.configurator.common.util.JooqMapperUtils;
import ru.sultanyarov.configurator.domain.model.Configuration;
import ru.sultanyarov.configurator.domain.model.ConfigurationComponent;
import ru.sultanyarov.configurator.domain.model.Page;

@Repository
@RequiredArgsConstructor
public class ConfigurationRepositoryImpl implements ConfigurationRepository {
  private static final String COMPONENTS_FIELD = "components";

  private final DSLContext dslContext;

  @Override
  public Optional<Configuration> create(Configuration configuration) {
    Long configurationId =
        dslContext
            .insertInto(CONFIGURATION)
            .set(CONFIGURATION.DOMAIN_ID, configuration.domainId())
            .set(CONFIGURATION.NAME, configuration.name())
            .set(CONFIGURATION.DESCRIPTION, configuration.description())
            .set(CONFIGURATION.CREATED_BY_USER_ID, configuration.createdByUserId())
            .returning(CONFIGURATION.ID)
            .fetchOptional(CONFIGURATION.ID)
            .orElse(null);
    if (configurationId == null) {
      return Optional.empty();
    }

    insertComponents(configurationId, configuration.components());
    return findByIdAndUserId(configurationId, configuration.createdByUserId());
  }

  @Override
  public Optional<Configuration> update(Long id, Long userId, Configuration configuration) {
    int updatedRows =
        dslContext
            .update(CONFIGURATION)
            .set(CONFIGURATION.NAME, configuration.name())
            .set(CONFIGURATION.DESCRIPTION, configuration.description())
            .where(CONFIGURATION.ID.eq(id))
            .and(CONFIGURATION.CREATED_BY_USER_ID.eq(userId))
            .execute();
    if (updatedRows == 0) {
      return Optional.empty();
    }

    dslContext
        .deleteFrom(CONFIGURATION_COMPONENT)
        .where(CONFIGURATION_COMPONENT.CONFIGURATION_ID.eq(id))
        .execute();
    insertComponents(id, configuration.components());
    return findByIdAndUserId(id, userId);
  }

  @Override
  public Optional<Configuration> findByIdAndUserId(Long id, Long userId) {
    return dslContext
        .select(configurationFields())
        .from(CONFIGURATION)
        .where(CONFIGURATION.ID.eq(id))
        .and(CONFIGURATION.CREATED_BY_USER_ID.eq(userId))
        .fetchOptional(configurationMapper());
  }

  @Override
  public Page<Configuration> findPageByDomainIdAndUserId(
      Long domainId, Long userId, int page, int size) {
    Condition condition =
        CONFIGURATION.DOMAIN_ID.eq(domainId).and(CONFIGURATION.CREATED_BY_USER_ID.eq(userId));
    return jooqPage(
        dslContext,
        dslContext
            .select(configurationFields())
            .from(CONFIGURATION)
            .where(condition)
            .orderBy(List.of(CONFIGURATION.CREATED_AT.desc(), CONFIGURATION.ID.desc())),
        condition,
        CONFIGURATION,
        page,
        size,
        configurationMapper());
  }

  private void insertComponents(Long configurationId, List<ConfigurationComponent> components) {
    var insertQueries =
        components.stream()
            .map(
                component ->
                    dslContext
                        .insertInto(CONFIGURATION_COMPONENT)
                        .set(CONFIGURATION_COMPONENT.CONFIGURATION_ID, configurationId)
                        .set(CONFIGURATION_COMPONENT.COMPONENT_ID, component.id()))
            .toList();
    if (!insertQueries.isEmpty()) {
      dslContext.batch(insertQueries).execute();
    }
  }

  private List<SelectFieldOrAsterisk> configurationFields() {
    return List.of(
        CONFIGURATION.ID,
        CONFIGURATION.DOMAIN_ID,
        CONFIGURATION.NAME,
        CONFIGURATION.DESCRIPTION,
        CONFIGURATION.CREATED_BY_USER_ID,
        CONFIGURATION.CREATED_AT,
        componentsField());
  }

  private SelectField<List<ConfigurationComponent>> componentsField() {
    return multiset(
            dslContext
                .select(
                    COMPONENT.ID,
                    COMPONENT.NAME,
                    COMPONENT.BRAND,
                    COMPONENT.COMPONENT_TYPE_ID,
                    COMPONENT_TYPE.NAME,
                    COMPONENT.ARCHIVED)
                .from(CONFIGURATION_COMPONENT)
                .join(COMPONENT)
                .on(COMPONENT.ID.eq(CONFIGURATION_COMPONENT.COMPONENT_ID))
                .join(COMPONENT_TYPE)
                .on(COMPONENT_TYPE.ID.eq(COMPONENT.COMPONENT_TYPE_ID))
                .where(CONFIGURATION_COMPONENT.CONFIGURATION_ID.eq(CONFIGURATION.ID))
                .orderBy(
                    COMPONENT_TYPE.ORDER_INDEX.asc().nullsLast(),
                    COMPONENT_TYPE.NAME.asc(),
                    COMPONENT.ID.asc()))
        .convertFrom(result -> result.map(this::mapComponent))
        .as(COMPONENTS_FIELD);
  }

  private ConfigurationComponent mapComponent(Record record) {
    return ConfigurationComponent.builder()
        .id(record.get(COMPONENT.ID))
        .name(record.get(COMPONENT.NAME))
        .brand(record.get(COMPONENT.BRAND))
        .componentTypeId(record.get(COMPONENT.COMPONENT_TYPE_ID))
        .componentTypeName(record.get(COMPONENT_TYPE.NAME))
        .archived(Boolean.TRUE.equals(record.get(COMPONENT.ARCHIVED)))
        .build();
  }

  private RecordMapper<Record, Configuration> configurationMapper() {
    return record ->
        Configuration.builder()
            .id(record.get(CONFIGURATION.ID))
            .domainId(record.get(CONFIGURATION.DOMAIN_ID))
            .name(record.get(CONFIGURATION.NAME))
            .description(record.get(CONFIGURATION.DESCRIPTION))
            .createdByUserId(record.get(CONFIGURATION.CREATED_BY_USER_ID))
            .createdAt(record.get(CONFIGURATION.CREATED_AT))
            .components(JooqMapperUtils.getListOrNull(record, COMPONENTS_FIELD))
            .build();
  }
}
