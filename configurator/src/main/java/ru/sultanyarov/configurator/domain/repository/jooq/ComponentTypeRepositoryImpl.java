package ru.sultanyarov.configurator.domain.repository.jooq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.RecordMapper;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.entity.jooq.tables.records.ComponentTypeRecord;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.repository.ComponentTypeRepository;

import java.util.List;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ComponentTypeRepositoryImpl implements ComponentTypeRepository {
    private final DSLContext dslContext;

    @Override
    public Optional<ComponentType> createComponentType(ComponentType componentType) {
        return dslContext.insertInto(Tables.COMPONENT_TYPE)
                .set(dslContext.newRecord(Tables.COMPONENT_TYPE, componentType))
                .returning()
                .fetchOptional(getComponentTypeRecordMapper());
    }

    @Override
    public boolean existsByNameAndDomainId(String name, Long domainId) {
        return dslContext.fetchExists(
                dslContext.selectFrom(Tables.COMPONENT_TYPE)
                        .where(Tables.COMPONENT_TYPE.NAME.eq(name))
                        .and(Tables.COMPONENT_TYPE.DOMAIN_ID.eq(domainId))
        );
    }

    @Override
    public boolean existsById(Long id) {
        return dslContext.fetchExists(
                dslContext.selectFrom(Tables.COMPONENT_TYPE)
                        .where(Tables.COMPONENT_TYPE.ID.eq(id))
        );
    }

    @Override
    public Optional<ComponentType> updateComponentType(Long id, ComponentType componentType) {
        return dslContext.update(Tables.COMPONENT_TYPE)
                .set(dslContext.newRecord(Tables.COMPONENT_TYPE, componentType))
                .where(Tables.COMPONENT_TYPE.ID.eq(id))
                .returning()
                .fetchOptional(getComponentTypeRecordMapper());
    }

    @Override
    public void deleteComponentTypeById(Long id) {
        dslContext.deleteFrom(Tables.COMPONENT_TYPE)
                .where(Tables.COMPONENT_TYPE.ID.eq(id))
                .execute();
    }

    @Override
    public Optional<ComponentType> getComponentTypeById(Long id) {
        return dslContext.selectFrom(
                        Tables.COMPONENT_TYPE
                                .leftJoin(Tables.DOMAIN)
                                .on(Tables.DOMAIN.ID.eq(Tables.COMPONENT_TYPE.DOMAIN_ID))
                )
                .where(Tables.COMPONENT_TYPE.ID.eq(id))
                .fetchOptional(record -> ComponentType.builder()
                        .id(record.get(Tables.COMPONENT_TYPE.ID))
                        .domainId(record.get(Tables.COMPONENT_TYPE.DOMAIN_ID))
                        .name(record.get(Tables.COMPONENT_TYPE.NAME))
                        .code(record.get(Tables.COMPONENT_TYPE.CODE))
                        .description(record.get(Tables.COMPONENT_TYPE.DESCRIPTION))
                        .orderIndex(record.get(Tables.COMPONENT_TYPE.ORDER_INDEX))
                        .createdAt(record.get(Tables.COMPONENT_TYPE.CREATED_AT))
                        .build());
    }

    @Override
    public List<ComponentType> getComponentTypesByDomainId(Long domainId) {
        return dslContext.selectFrom(Tables.COMPONENT_TYPE)
                .where(Tables.COMPONENT_TYPE.DOMAIN_ID.eq(domainId))
                .fetch(getComponentTypeRecordMapper());
    }

    private RecordMapper<ComponentTypeRecord, ComponentType> getComponentTypeRecordMapper() {
        return record -> ComponentType.builder()
                .id(record.getId())
                .domainId(record.getDomainId())
                .name(record.getName())
                .code(record.getCode())
                .description(record.getDescription())
                .orderIndex(record.getOrderIndex())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
