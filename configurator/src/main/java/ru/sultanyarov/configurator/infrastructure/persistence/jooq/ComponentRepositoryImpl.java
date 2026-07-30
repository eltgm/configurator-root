package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import lombok.*;
import lombok.extern.slf4j.*;
import org.jooq.*;
import org.springframework.stereotype.*;
import ru.sultanyarov.configurator.application.port.out.*;
import ru.sultanyarov.configurator.common.util.*;
import ru.sultanyarov.configurator.domain.entity.jooq.*;
import ru.sultanyarov.configurator.domain.model.*;
import ru.sultanyarov.configurator.domain.model.Component;

import java.util.*;

import static ru.sultanyarov.configurator.common.util.PaginationHelper.*;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.*;

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
    public Optional<Component> getComponentById(Long id) {
        List<SelectFieldOrAsterisk> fields = new ArrayList<>(List.of(
                COMPONENT.ID,
                COMPONENT.COMPONENT_TYPE_ID,
                COMPONENT.NAME,
                COMPONENT.BRAND,
                COMPONENT.DESCRIPTION,
                COMPONENT.ARCHIVED,
                COMPONENT.CREATED_AT
        ));
        fields.add(attributeValuesField());
        fields.add(imagesField());

        return dslContext.select(fields)
                .from(COMPONENT)
                .where(COMPONENT.ID.eq(id))
                .fetchOptional(getComponentRecordMapper());
    }

    @Override
    public Optional<Component> updateComponent(Long id, Component component) {
        return dslContext.update(COMPONENT)
                .set(COMPONENT.NAME, component.getName())
                .set(COMPONENT.BRAND, component.getBrand())
                .set(COMPONENT.DESCRIPTION, component.getDescription())
                .where(COMPONENT.ID.eq(id))
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

    @Override
    public Optional<Component> getById(Long id) {
        return dslContext.selectFrom(COMPONENT)
                .where(COMPONENT.ID.eq(id))
                .fetchOptional(getComponentRecordMapper());
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
                .attributes(JooqMapperUtils.getListOrNull(componentRecord, "attributes"))
                .images(JooqMapperUtils.getListOrNull(componentRecord, "images"))
                .build();
    }
}
