package ru.sultanyarov.configurator.service.core;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.port.out.ComponentImageCleanupRepository;
import ru.sultanyarov.configurator.application.port.out.ComponentImageStorage;
import ru.sultanyarov.configurator.application.service.ComponentImageCleanupService;
import ru.sultanyarov.configurator.domain.exception.ExternalStorageException;

@ExtendWith(MockitoExtension.class)
class ComponentImageCleanupServiceTest {
  @Mock ComponentImageCleanupRepository repository;
  @Mock ComponentImageStorage storage;
  @InjectMocks ComponentImageCleanupService service;

  @Test
  void shouldAcknowledgeOnlySuccessfulDeletionsAndContinueAfterStorageFailure() {
    when(repository.findDue(any(), eq(100))).thenReturn(List.of("failed", "deleted"));
    doThrow(new ExternalStorageException(new IllegalStateException("offline"), "offline"))
        .when(storage)
        .delete("failed");
    Instant before = Instant.now();

    service.cleanUpPendingImages();

    verify(repository, never()).complete("failed");
    verify(repository)
        .retryLater(eq("failed"), argThat(time -> !time.isBefore(before.plusSeconds(60))));
    var order = inOrder(storage, repository);
    order.verify(storage).delete("deleted");
    order.verify(repository).complete("deleted");
  }

  @Test
  void shouldRetryIfAcknowledgementFailsAfterDeletingTheObject() {
    when(repository.findDue(any(), anyInt())).thenReturn(List.of("image"));
    doThrow(new IllegalStateException("database unavailable"))
        .doNothing()
        .when(repository)
        .complete("image");
    service.cleanUpPendingImages();
    verify(repository).retryLater(eq("image"), any());
    service.cleanUpPendingImages();
    verify(storage, times(2)).delete("image");
    verify(repository, times(2)).complete("image");
  }

  @Test
  void shouldNotContactStorageWhenThereAreNoDueJobs() {
    when(repository.findDue(any(), anyInt())).thenReturn(List.of());
    service.cleanUpPendingImages();
    verifyNoInteractions(storage);
  }
}
