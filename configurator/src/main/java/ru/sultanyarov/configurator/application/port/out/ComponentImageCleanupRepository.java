package ru.sultanyarov.configurator.application.port.out;

import java.time.Instant;
import java.util.List;

/** Durable, idempotent image deletion jobs, independent of component/domain lifetime. */
public interface ComponentImageCleanupRepository {
  /** Called in the domain deletion transaction, after locking its components. */
  void enqueueByDomainId(Long domainId);

  List<String> findDue(Instant now, int limit);

  void complete(String objectKey);

  void retryLater(String objectKey, Instant nextAttempt);
}
