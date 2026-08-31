package ru.sultanyarov.configurator.service.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.port.out.ComponentImageCleanupRepository;
import ru.sultanyarov.configurator.application.port.out.ConfigurationRepository;
import ru.sultanyarov.configurator.application.port.out.DomainRepository;
import ru.sultanyarov.configurator.application.service.DomainServiceImpl;
import ru.sultanyarov.configurator.domain.exception.DomainHasConfigurationsException;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;
import ru.sultanyarov.configurator.test.data.DomainTestData;

@ExtendWith(MockitoExtension.class)
class DomainServiceImplTest {
  @Mock private DomainRepository domainRepository;

  @Mock private ConfigurationRepository configurationRepository;

  @Mock private ComponentImageCleanupRepository imageCleanupRepository;

  @InjectMocks private DomainServiceImpl domainService;

  @Test
  void getPage_shouldReturnPageOfDomains() {
    // Arrange
    int page = 0;
    int pageSize = 10;
    List<Domain> domains = List.of(DomainTestData.domain(), DomainTestData.domain());
    Page<Domain> expectedPage = new Page<>(domains, page, pageSize, 2);

    when(domainRepository.getDomains(page, pageSize)).thenReturn(expectedPage);

    // Act
    Page<Domain> result = domainService.getPage(page, pageSize);

    // Assert
    assertThat(result).isEqualTo(expectedPage);
    verify(domainRepository).getDomains(page, pageSize);
  }

  @Test
  void getById_shouldReturnDomainWhenItExists() {
    // Arrange
    Long id = 1L;
    Domain expectedDomain = DomainTestData.domainWithId(id);

    when(domainRepository.getDomainById(id)).thenReturn(Optional.of(expectedDomain));

    // Act
    Domain result = domainService.getById(id);

    // Assert
    assertThat(result).isEqualTo(expectedDomain);
    verify(domainRepository).getDomainById(id);
  }

  @Test
  void getById_shouldThrowNotFoundExceptionWhenDomainDoesNotExist() {
    // Arrange
    Long id = 1L;

    when(domainRepository.getDomainById(id)).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> domainService.getById(id)).isInstanceOf(NotFoundException.class);

    verify(domainRepository).getDomainById(anyLong());
  }

  @Test
  void deleteById_shouldLockValidateQueueAndDeleteInOrder() {
    when(domainRepository.lockById(1L)).thenReturn(true);
    domainService.deleteById(1L);
    var order =
        org.mockito.Mockito.inOrder(
            domainRepository, configurationRepository, imageCleanupRepository);
    order.verify(domainRepository).lockById(1L);
    order.verify(configurationRepository).existsByDomainId(1L);
    order.verify(domainRepository).lockContentsByDomainId(1L);
    order.verify(imageCleanupRepository).enqueueByDomainId(1L);
    order.verify(domainRepository).deleteContentsByDomainId(1L);
    order.verify(domainRepository).deleteDomainById(1L);
  }

  @Test
  void deleteById_shouldLeaveEverythingUntouchedWhenConfigurationsExist() {
    when(domainRepository.lockById(1L)).thenReturn(true);
    when(configurationRepository.existsByDomainId(1L)).thenReturn(true);
    assertThatThrownBy(() -> domainService.deleteById(1L))
        .isInstanceOf(DomainHasConfigurationsException.class)
        .hasMessageContaining("Delete all configurations first");
    org.mockito.Mockito.verifyNoInteractions(imageCleanupRepository);
    verify(domainRepository, never()).lockContentsByDomainId(anyLong());
    verify(domainRepository, never()).deleteContentsByDomainId(anyLong());
    verify(domainRepository, never()).deleteDomainById(anyLong());
  }

  @Test
  void deleteById_shouldThrowNotFoundExceptionWhenDomainDoesNotExist() {
    assertThatThrownBy(() -> domainService.deleteById(1L)).isInstanceOf(NotFoundException.class);
    org.mockito.Mockito.verifyNoInteractions(configurationRepository, imageCleanupRepository);
    verify(domainRepository, never()).deleteDomainById(anyLong());
  }

  @Test
  void create_shouldCreateDomainWhenNameIsUnique() {
    // Arrange
    Domain domain = DomainTestData.domain();

    when(domainRepository.existsByName(domain.name())).thenReturn(false);
    when(domainRepository.createDomain(domain)).thenReturn(Optional.of(domain));

    // Act
    Domain result = domainService.create(domain);

    // Assert
    assertThat(result).isEqualTo(domain);
    verify(domainRepository).existsByName(domain.name());
    verify(domainRepository).createDomain(domain);
  }

  @Test
  void create_shouldThrowEntityAlreadyExistsExceptionWhenDomainWithSameNameExists() {
    // Arrange
    Domain domain = DomainTestData.domain();

    when(domainRepository.existsByName(domain.name())).thenReturn(true);

    // Act & Assert
    assertThatThrownBy(() -> domainService.create(domain))
        .isInstanceOf(EntityAlreadyExistsException.class);

    verify(domainRepository).existsByName(domain.name());
    verify(domainRepository, never()).createDomain(any());
  }

  @Test
  void update_shouldUpdateDomainWhenItExistsAndNameIsUnique() {
    // Arrange
    Long id = 1L;
    Domain domain = DomainTestData.domain();

    when(domainRepository.existsByName(domain.name())).thenReturn(false);
    when(domainRepository.getDomainById(anyLong()))
        .thenReturn(
            Optional.of(
                new Domain(
                    2L,
                    "new name",
                    domain.description(),
                    domain.createdByUserId(),
                    domain.componentTypes(),
                    domain.createdAt())));
    when(domainRepository.updateDomain(id, domain)).thenReturn(Optional.of(domain));

    // Act
    Domain result = domainService.update(id, domain);

    // Assert
    assertThat(result).isEqualTo(domain);
    verify(domainRepository).getDomainById(id);
    verify(domainRepository).existsByName(domain.name());
    verify(domainRepository).updateDomain(id, domain);
  }

  @Test
  void update_shouldThrowNotFoundExceptionWhenDomainDoesNotExist() {
    // Arrange
    Long id = 1L;
    Domain domain = DomainTestData.domain();

    when(domainRepository.getDomainById(anyLong())).thenReturn(Optional.empty());

    // Act & Assert
    assertThatThrownBy(() -> domainService.update(id, domain))
        .isInstanceOf(NotFoundException.class);

    verify(domainRepository, never()).existsByName(anyString());
    verify(domainRepository, never()).updateDomain(anyLong(), any());
  }

  @Test
  void update_shouldThrowEntityAlreadyExistsExceptionWhenAnotherDomainWithSameNameExists() {
    // Arrange
    Long id = 1L;
    Domain domain = DomainTestData.domain();

    when(domainRepository.existsByName(domain.name())).thenReturn(true);
    when(domainRepository.getDomainById(anyLong()))
        .thenReturn(
            Optional.of(
                new Domain(
                    2L,
                    "new name",
                    domain.description(),
                    domain.createdByUserId(),
                    domain.componentTypes(),
                    domain.createdAt())));

    // Act & Assert
    assertThatThrownBy(() -> domainService.update(id, domain))
        .isInstanceOf(EntityAlreadyExistsException.class);

    verify(domainRepository).existsByName(domain.name());
    verify(domainRepository, never()).updateDomain(anyLong(), any());
  }
}
