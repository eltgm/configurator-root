package ru.sultanyarov.configurator.application.service;

import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.sultanyarov.configurator.application.port.out.ComponentImageCleanupRepository;
import ru.sultanyarov.configurator.application.port.out.ComponentImageStorage;

@Service
@Slf4j
@RequiredArgsConstructor
public class ComponentImageCleanupService {
  private static final int BATCH_SIZE = 100;
  private static final Duration RETRY_DELAY = Duration.ofMinutes(1);
  private final ComponentImageCleanupRepository cleanupRepository;
  private final ComponentImageStorage imageStorage;

  // No encompassing DB transaction: only committed jobs are visible, and network I/O holds no
  // locks.
  public void cleanUpPendingImages() {
    for (String objectKey : cleanupRepository.findDue(Instant.now(), BATCH_SIZE)) {
      try {
        // Storage deletion includes thumbnails and is idempotent. A crash before complete is safe.
        imageStorage.delete(objectKey);
        cleanupRepository.complete(objectKey);
      } catch (RuntimeException exception) {
        log.warn("Will retry cleanup of component image {}", objectKey, exception);
        cleanupRepository.retryLater(objectKey, Instant.now().plus(RETRY_DELAY));
      }
    }
  }
}
