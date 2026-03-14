package ru.sultanyarov.configurator.domain.repository.jooq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.RecordMapper;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.entity.jooq.tables.records.ComponentRecord;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.repository.ComponentRepository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ComponentRepositoryImpl implements ComponentRepository {
    private final DSLContext dslContext;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<Component> createComponent(Component componentToCreate) {
        return dslContext.insertInto(Tables.COMPONENT)
                .set(dslContext.newRecord(Tables.COMPONENT, componentToCreate))
                .returning()
                .fetchOptional(getComponentRecordMapper());
    }

    private RecordMapper<ComponentRecord, Component> getComponentRecordMapper() {
        return record -> Component.builder()
                .id(record.getId())
                .componentTypeId(record.getComponentTypeId())
                .name(record.getName())
                .brand(record.getBrand())
                .description(record.getDescription())
                .archived(record.getArchived())
                .createdAt(record.getCreatedAt())
                .build();
    }
}
