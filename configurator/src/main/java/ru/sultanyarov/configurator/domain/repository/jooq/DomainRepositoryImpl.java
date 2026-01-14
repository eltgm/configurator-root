package ru.sultanyarov.configurator.domain.repository.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.jooq.RecordMapper;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.entity.jooq.tables.records.DomainRecord;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;
import ru.sultanyarov.configurator.domain.repository.DomainRepository;

import java.util.List;
import java.util.Optional;

import static ru.sultanyarov.configurator.domain.repository.config.PaginationHelper.jooqPage;

@Repository
@RequiredArgsConstructor
public class DomainRepositoryImpl implements DomainRepository {
    private final DSLContext dslContext;

    @Override
    public Optional<Domain> getDomainById(Long id) {
        var d = Tables.DOMAIN;
        var ct = Tables.COMPONENT_TYPE;

        return dslContext
                .select(
                        d.ID,
                        d.NAME,
                        d.DESCRIPTION,
                        d.CREATED_BY_USER_ID,
                        d.CREATED_AT,

                        org.jooq.impl.DSL.multiset(
                                        dslContext.select(
                                                        ct.ID,
                                                        ct.DOMAIN_ID,
                                                        ct.NAME,
                                                        ct.CODE,
                                                        ct.DESCRIPTION,
                                                        ct.ORDER_INDEX,
                                                        ct.CREATED_AT
                                                )
                                                .from(ct)
                                                .where(ct.DOMAIN_ID.eq(d.ID))
                                                .orderBy(ct.ORDER_INDEX.asc())
                                )
                                .convertFrom(r -> r.map(rr -> ComponentType.builder()
                                        .id(rr.get(ct.ID))
                                        .domainId(rr.get(ct.DOMAIN_ID))
                                        .name(rr.get(ct.NAME))
                                        .description(rr.get(ct.DESCRIPTION))
                                        .orderIndex(rr.get(ct.ORDER_INDEX))
                                        .createdAt(rr.get(ct.CREATED_AT))
                                        .build()
                                ))
                                .as("componentTypes")
                )
                .from(d)
                .where(d.ID.eq(id))
                .fetchOptional(r -> Domain.builder()
                        .id(r.get(d.ID))
                        .name(r.get(d.NAME))
                        .description(r.get(d.DESCRIPTION))
                        .createdByUserId(r.get(d.CREATED_BY_USER_ID))
                        .createdAt(r.get(d.CREATED_AT))
                        .componentTypes(r.get("componentTypes", List.class))
                        .build()
                );
    }

    @Override
    public void deleteDomainById(Long id) {
        dslContext.delete(Tables.DOMAIN)
                .where(Tables.DOMAIN.ID.eq(id))
                .execute();
    }

    @Override
    public Optional<Domain> createDomain(Domain domain) {
        DomainRecord domainRecord = dslContext.newRecord(Tables.DOMAIN);
        domainRecord.setName(domain.name());
        domainRecord.setDescription(domain.description());
        domainRecord.setCreatedByUserId(domain.createdByUserId());

        return dslContext.insertInto(Tables.DOMAIN)
                .set(domainRecord)
                .returning()
                .fetchOptional(getDomainRecordMapper());
    }

    @Override
    public Optional<Domain> updateDomain(Long id, Domain domain) {
        return dslContext.update(Tables.DOMAIN)
                .set(dslContext.newRecord(Tables.DOMAIN, domain))
                .where(Tables.DOMAIN.ID.eq(id))
                .returning()
                .fetchOptional(getDomainRecordMapper());
    }

    private static RecordMapper<DomainRecord, Domain> getDomainRecordMapper() {
        return record -> Domain.builder()
                .id(record.getId())
                .name(record.getName())
                .description(record.getDescription())
                .createdByUserId(record.getCreatedByUserId())
                .createdAt(record.getCreatedAt())
                .build();
    }

    @Override
    public Page<Domain> getDomains(int page, int pageSize) {
        return jooqPage(
                dslContext,
                dslContext.selectFrom(Tables.DOMAIN)
                        .orderBy(List.of(Tables.DOMAIN.ID.asc())),
                Tables.DOMAIN,
                page,
                pageSize,
                record -> Domain.builder()
                        .id(record.get(Tables.DOMAIN.ID))
                        .name(record.get(Tables.DOMAIN.NAME))
                        .description(record.get(Tables.DOMAIN.DESCRIPTION))
                        .createdByUserId(record.get(Tables.DOMAIN.CREATED_BY_USER_ID))
                        .createdAt(record.get(Tables.DOMAIN.CREATED_AT))
                        .build()
        );
    }

    @Override
    public boolean existsByName(String name) {
        return dslContext.fetchExists(
                dslContext.selectFrom(Tables.DOMAIN)
                        .where(Tables.DOMAIN.NAME.eq(name))
        );
    }

    @Override
    public boolean existsById(Long id) {
        return dslContext.fetchExists(
                dslContext.selectFrom(Tables.DOMAIN)
                        .where(Tables.DOMAIN.ID.eq(id))
        );
    }
}
