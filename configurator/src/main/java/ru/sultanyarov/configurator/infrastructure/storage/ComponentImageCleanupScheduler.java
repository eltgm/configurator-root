package ru.sultanyarov.configurator.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import ru.sultanyarov.configurator.application.service.ComponentImageCleanupService;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.storage.component-images.cleanup.enabled", matchIfMissing = true)
@RequiredArgsConstructor
public class ComponentImageCleanupScheduler {
  private final ComponentImageCleanupService cleanupService;

  @Scheduled(
      initialDelayString = "${app.storage.component-images.cleanup.delay-ms:5000}",
      fixedDelayString = "${app.storage.component-images.cleanup.delay-ms:5000}")
  public void cleanUp() {
    cleanupService.cleanUpPendingImages();
  }
}
