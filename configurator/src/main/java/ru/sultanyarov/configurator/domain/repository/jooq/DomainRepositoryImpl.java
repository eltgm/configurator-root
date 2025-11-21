package ru.sultanyarov.configurator.domain.repository.jooq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.domain.entity.jooq.Tables;
import ru.sultanyarov.configurator.domain.exception.BusinessException;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;
import ru.sultanyarov.configurator.domain.repository.DomainRepository;

import java.util.List;

import static ru.sultanyarov.configurator.domain.repository.config.PaginationHelper.jooqPage;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DomainRepositoryImpl implements DomainRepository {
    private final DSLContext dslContext;

    @Override
    public Domain getDomainById(Long id) {
        return dslContext.selectFrom(Tables.DOMAIN)
                .where(Tables.DOMAIN.ID.eq(id))
                .fetchOptionalInto(Domain.class)
                .orElseThrow(() -> new BusinessException("Error while getting domain by id"));
    }

    @Override
    public void deleteDomainById(Long id) {
        dslContext.delete(Tables.DOMAIN)
                .where(Tables.DOMAIN.ID.eq(id))
                .execute();
    }

    @Override
    public Domain createDomain(Domain domain) {
        return dslContext.insertInto(Tables.DOMAIN)
                .set(dslContext.newRecord(Tables.DOMAIN, domain))
                .returning()
                .fetchOptional()
                .orElseThrow(() -> new BusinessException("Error while creating domain"))
                .into(Domain.class);
    }

    @Override
    public Domain updateDomain(Long id, Domain domain) {
        return dslContext.update(Tables.DOMAIN)
                .set(dslContext.newRecord(Tables.DOMAIN, domain))
                .where(Tables.DOMAIN.ID.eq(id))
                .returning()
                .fetchOptional()
                .orElseThrow(() -> new BusinessException("Error while updating domain"))
                .into(Domain.class);
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
                Domain.class
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
