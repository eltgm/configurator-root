package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.RecordMapper;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.application.port.out.ComponentRepository;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.Page;

import java.util.List;
import java.util.Optional;

import static ru.sultanyarov.configurator.common.util.PaginationHelper.jooqPage;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT_TYPE;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ComponentRepositoryImpl implements ComponentRepository {
    private static final ru.sultanyarov.configurator.domain.entity.jooq.tables.Component COMPONENT = Tables.COMPONENT;

    private final DSLContext dslContext;

    @Override
    public Optional<Component> createComponent(Component componentToCreate) {
        return dslContext.insertInto(COMPONENT)
                .set(dslContext.newRecord(COMPONENT, componentToCreate))
                .returning()
                .fetchOptional(getComponentRecordMapper());
    }

    @Override
    public boolean hasByComponentTypeId(Long componentTypeId) {
        return dslContext.fetchExists(
                dslContext.selectFrom(COMPONENT)
                        .where(COMPONENT.COMPONENT_TYPE_ID.eq(componentTypeId))
        );
    }

    @Override
    public Page<Component> findPageByDomainIdComponentTypeIdName(Long domainId, Long componentTypeId, String name, Integer page, Integer size) {
        Condition condition = COMPONENT.COMPONENT_TYPE_ID.in(
                dslContext.select(COMPONENT_TYPE.ID)
                        .from(COMPONENT_TYPE)
                        .where(COMPONENT_TYPE.DOMAIN_ID.eq(domainId))
        );

        if (componentTypeId != null) {
            condition = condition.and(COMPONENT.COMPONENT_TYPE_ID.eq(componentTypeId));
        }

        if (name != null && !name.isBlank()) {
            condition = condition.and(COMPONENT.NAME.eq(name));
        }

        return jooqPage(
                dslContext,
                dslContext
                        .selectFrom(COMPONENT)
                        .where(condition)
                        .orderBy(List.of(COMPONENT.ID)),
                condition,
                COMPONENT,
                page,
                size,
                getComponentRecordMapper()
        );
    }

    private RecordMapper<org.jooq.Record, Component> getComponentRecordMapper() {
        return componentRecord -> Component.builder()
                .id(componentRecord.get(COMPONENT.ID))
                .componentTypeId(componentRecord.get(COMPONENT.COMPONENT_TYPE_ID))
                .name(componentRecord.get(COMPONENT.NAME))
                .brand(componentRecord.get(COMPONENT.BRAND))
                .description(componentRecord.get(COMPONENT.DESCRIPTION))
                .archived(componentRecord.get(COMPONENT.ARCHIVED))
                .createdAt(componentRecord.get(COMPONENT.CREATED_AT))
                .build();
    }
}
