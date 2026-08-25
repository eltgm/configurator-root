package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.application.port.out.ComponentTypeAttributeRepository;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.ComponentTypeAttribute;

@Repository
@RequiredArgsConstructor
public class ComponentTypeAttributeRepositoryImpl implements ComponentTypeAttributeRepository {
  private static final ru.sultanyarov.configurator.domain.entity.jooq.tables.ComponentTypeAttribute
      CTA = Tables.COMPONENT_TYPE_ATTRIBUTE;

  private final DSLContext dslContext;

  @Override
  public Optional<ComponentTypeAttribute> save(ComponentTypeAttribute link) {
    return dslContext
        .insertInto(CTA)
        .set(CTA.COMPONENT_TYPE_ID, link.componentTypeId())
        .set(CTA.ATTRIBUTE_DEFINITION_ID, link.attributeDefinitionId())
        .set(CTA.IS_REQUIRED, Boolean.TRUE.equals(link.isRequired()))
        .set(CTA.ORDER_INDEX, link.orderIndex())
        .onConflict(CTA.COMPONENT_TYPE_ID, CTA.ATTRIBUTE_DEFINITION_ID)
        .doUpdate()
        .set(CTA.IS_REQUIRED, Boolean.TRUE.equals(link.isRequired()))
        .set(CTA.ORDER_INDEX, link.orderIndex())
        .returning()
        .fetchOptional(
            r ->
                ComponentTypeAttribute.builder()
                    .componentTypeId(r.get(CTA.COMPONENT_TYPE_ID))
                    .attributeDefinitionId(r.get(CTA.ATTRIBUTE_DEFINITION_ID))
                    .isRequired(r.get(CTA.IS_REQUIRED))
                    .orderIndex(r.get(CTA.ORDER_INDEX))
                    .createdAt(r.get(CTA.CREATED_AT))
                    .build());
  }

  @Override
  public Optional<ComponentTypeAttribute> get(Long componentTypeId, Long attributeDefinitionId) {
    return dslContext
        .selectFrom(CTA)
        .where(CTA.COMPONENT_TYPE_ID.eq(componentTypeId))
        .and(CTA.ATTRIBUTE_DEFINITION_ID.eq(attributeDefinitionId))
        .fetchOptional(
            r ->
                ComponentTypeAttribute.builder()
                    .componentTypeId(r.get(CTA.COMPONENT_TYPE_ID))
                    .attributeDefinitionId(r.get(CTA.ATTRIBUTE_DEFINITION_ID))
                    .isRequired(r.get(CTA.IS_REQUIRED))
                    .orderIndex(r.get(CTA.ORDER_INDEX))
                    .createdAt(r.get(CTA.CREATED_AT))
                    .build());
  }

  @Override
  public boolean exists(Long componentTypeId, Long attributeDefinitionId) {
    return dslContext.fetchExists(
        dslContext
            .selectFrom(CTA)
            .where(CTA.COMPONENT_TYPE_ID.eq(componentTypeId))
            .and(CTA.ATTRIBUTE_DEFINITION_ID.eq(attributeDefinitionId)));
  }

  @Override
  public boolean delete(Long componentTypeId, Long attributeDefinitionId) {
    return dslContext
            .deleteFrom(CTA)
            .where(CTA.COMPONENT_TYPE_ID.eq(componentTypeId))
            .and(CTA.ATTRIBUTE_DEFINITION_ID.eq(attributeDefinitionId))
            .execute()
        > 0;
  }

  @Override
  public List<Long> getComponentTypeIdsByAttributeDefinitionId(Long attributeDefinitionId) {
    return dslContext
        .select(CTA.COMPONENT_TYPE_ID)
        .from(CTA)
        .where(CTA.ATTRIBUTE_DEFINITION_ID.eq(attributeDefinitionId))
        .orderBy(CTA.COMPONENT_TYPE_ID)
        .fetch(CTA.COMPONENT_TYPE_ID);
  }
}
