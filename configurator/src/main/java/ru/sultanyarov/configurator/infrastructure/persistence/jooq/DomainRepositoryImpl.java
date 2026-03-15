package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.Record;
import org.jooq.RecordMapper;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.application.port.out.DomainRepository;
import ru.sultanyarov.configurator.common.util.JooqMapperUtils;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.entity.jooq.tables.records.DomainRecord;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;

import java.util.List;
import java.util.Optional;

import static ru.sultanyarov.configurator.common.util.PaginationHelper.jooqPage;

@Repository
@RequiredArgsConstructor
public class DomainRepositoryImpl implements DomainRepository {
    private static final ru.sultanyarov.configurator.domain.entity.jooq.tables.Domain DOMAIN = Tables.DOMAIN;

    private final DSLContext dslContext;

    @Override
    public Optional<Domain> getDomainById(Long id) {
        return dslContext
                .select(
                        DOMAIN.ID,
                        DOMAIN.NAME,
                        DOMAIN.DESCRIPTION,
                        DOMAIN.CREATED_BY_USER_ID,
                        DOMAIN.CREATED_AT,

                        org.jooq.impl.DSL.multiset(
                                        dslContext.select(
                                                        Tables.COMPONENT_TYPE.ID,
                                                        Tables.COMPONENT_TYPE.DOMAIN_ID,
                                                        Tables.COMPONENT_TYPE.NAME,
                                                        Tables.COMPONENT_TYPE.CODE,
                                                        Tables.COMPONENT_TYPE.DESCRIPTION,
                                                        Tables.COMPONENT_TYPE.ORDER_INDEX,
                                                        Tables.COMPONENT_TYPE.CREATED_AT
                                                )
                                                .from(Tables.COMPONENT_TYPE)
                                                .where(Tables.COMPONENT_TYPE.DOMAIN_ID.eq(DOMAIN.ID))
                                                .orderBy(Tables.COMPONENT_TYPE.ORDER_INDEX.asc())
                                )
                                .convertFrom(r -> r.map(getComponentTypeRecordMapper()
                                ))
                                .as("componentTypes")
                )
                .from(DOMAIN)
                .where(DOMAIN.ID.eq(id))
                .fetchOptional(getDomainRecordMapper());
    }

    private RecordMapper<Record, ComponentType> getComponentTypeRecordMapper() {
        return rr -> ComponentType.builder()
                .id(rr.get(Tables.COMPONENT_TYPE.ID))
                .domainId(rr.get(Tables.COMPONENT_TYPE.DOMAIN_ID))
                .name(rr.get(Tables.COMPONENT_TYPE.NAME))
                .description(rr.get(Tables.COMPONENT_TYPE.DESCRIPTION))
                .orderIndex(rr.get(Tables.COMPONENT_TYPE.ORDER_INDEX))
                .createdAt(rr.get(Tables.COMPONENT_TYPE.CREATED_AT))
                .build();
    }

    @Override
    public void deleteDomainById(Long id) {
        dslContext.delete(DOMAIN)
                .where(DOMAIN.ID.eq(id))
                .execute();
    }

    @Override
    public Optional<Domain> createDomain(Domain domain) {
        DomainRecord domainRecord = dslContext.newRecord(DOMAIN);
        domainRecord.setName(domain.name());
        domainRecord.setDescription(domain.description());
        domainRecord.setCreatedByUserId(domain.createdByUserId());

        return dslContext.insertInto(DOMAIN)
                .set(domainRecord)
                .returning()
                .fetchOptional(getDomainRecordMapper());
    }

    @Override
    public Optional<Domain> updateDomain(Long id, Domain domain) {
        return dslContext.update(DOMAIN)
                .set(dslContext.newRecord(DOMAIN, domain))
                .where(DOMAIN.ID.eq(id))
                .returning()
                .fetchOptional(getDomainRecordMapper());
    }

    @Override
    public Page<Domain> getDomains(int page, int pageSize) {
        return jooqPage(
                dslContext,
                dslContext.selectFrom(DOMAIN)
                        .orderBy(List.of(DOMAIN.ID.asc())),
                DOMAIN,
                page,
                pageSize,
                getDomainRecordMapper()
        );
    }

    private RecordMapper<org.jooq.Record, Domain> getDomainRecordMapper() {
        return domainRecord -> Domain.builder()
                .id(domainRecord.get(DOMAIN.ID))
                .name(domainRecord.get(DOMAIN.NAME))
                .description(domainRecord.get(DOMAIN.DESCRIPTION))
                .createdByUserId(domainRecord.get(DOMAIN.CREATED_BY_USER_ID))
                .createdAt(domainRecord.get(DOMAIN.CREATED_AT))
                .componentTypes(JooqMapperUtils.getListOrNull(domainRecord, "componentTypes"))
                .build();
    }

    @Override
    public boolean existsByName(String name) {
        return dslContext.fetchExists(
                dslContext.selectFrom(DOMAIN)
                        .where(DOMAIN.NAME.eq(name))
        );
    }

    @Override
    public boolean existsById(Long id) {
        return dslContext.fetchExists(
                dslContext.selectFrom(DOMAIN)
                        .where(DOMAIN.ID.eq(id))
        );
    }
}
