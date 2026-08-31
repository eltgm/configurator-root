package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.*;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import ru.sultanyarov.configurator.application.port.out.ComponentImageCleanupRepository;

@Repository
@RequiredArgsConstructor
public class ComponentImageCleanupRepositoryImpl implements ComponentImageCleanupRepository {
  private final DSLContext dslContext;

  @Override
  public void enqueueByDomainId(Long domainId) {
    dslContext
        .insertInto(COMPONENT_IMAGE_CLEANUP, COMPONENT_IMAGE_CLEANUP.OBJECT_KEY)
        .select(
            dslContext
                .select(COMPONENT_IMAGE.FILE_PATH)
                .from(COMPONENT_IMAGE)
                .join(COMPONENT)
                .on(COMPONENT.ID.eq(COMPONENT_IMAGE.COMPONENT_ID))
                .join(COMPONENT_TYPE)
                .on(COMPONENT_TYPE.ID.eq(COMPONENT.COMPONENT_TYPE_ID))
                .where(COMPONENT_TYPE.DOMAIN_ID.eq(domainId)))
        .onDuplicateKeyIgnore()
        .execute();
  }

  @Override
  public List<String> findDue(Instant now, int limit) {
    return dslContext
        .select(COMPONENT_IMAGE_CLEANUP.OBJECT_KEY)
        .from(COMPONENT_IMAGE_CLEANUP)
        .where(COMPONENT_IMAGE_CLEANUP.NEXT_ATTEMPT_AT.le(now.atOffset(ZoneOffset.UTC)))
        .orderBy(COMPONENT_IMAGE_CLEANUP.NEXT_ATTEMPT_AT, COMPONENT_IMAGE_CLEANUP.OBJECT_KEY)
        .limit(limit)
        .fetch(COMPONENT_IMAGE_CLEANUP.OBJECT_KEY);
  }

  @Override
  public void complete(String objectKey) {
    dslContext
        .deleteFrom(COMPONENT_IMAGE_CLEANUP)
        .where(COMPONENT_IMAGE_CLEANUP.OBJECT_KEY.eq(objectKey))
        .execute();
  }

  @Override
  public void retryLater(String objectKey, Instant nextAttempt) {
    dslContext
        .update(COMPONENT_IMAGE_CLEANUP)
        .set(COMPONENT_IMAGE_CLEANUP.NEXT_ATTEMPT_AT, nextAttempt.atOffset(ZoneOffset.UTC))
        .set(COMPONENT_IMAGE_CLEANUP.ATTEMPTS, COMPONENT_IMAGE_CLEANUP.ATTEMPTS.plus(1))
        .where(COMPONENT_IMAGE_CLEANUP.OBJECT_KEY.eq(objectKey))
        .execute();
  }
}
