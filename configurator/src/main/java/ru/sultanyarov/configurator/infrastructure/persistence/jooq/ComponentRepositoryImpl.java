package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.RecordMapper;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.application.port.out.ComponentRepository;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.entity.jooq.tables.records.ComponentRecord;
import ru.sultanyarov.configurator.domain.model.Component;

import java.util.Optional;

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

    private RecordMapper<ComponentRecord, Component> getComponentRecordMapper() {
        return componentRecord -> Component.builder()
                .id(componentRecord.getId())
                .componentTypeId(componentRecord.getComponentTypeId())
                .name(componentRecord.getName())
                .brand(componentRecord.getBrand())
                .description(componentRecord.getDescription())
                .archived(componentRecord.getArchived())
                .createdAt(componentRecord.getCreatedAt())
                .build();
    }
}
