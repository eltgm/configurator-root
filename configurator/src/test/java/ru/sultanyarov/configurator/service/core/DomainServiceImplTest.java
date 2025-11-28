package ru.sultanyarov.configurator.service.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;
import ru.sultanyarov.configurator.domain.repository.DomainRepository;
import ru.sultanyarov.configurator.test.data.DomainTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainServiceImplTest {
    @Mock
    private DomainRepository domainRepository;

    @InjectMocks
    private DomainServiceImpl domainService;

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

        when(domainRepository.existsById(id)).thenReturn(true);
        when(domainRepository.getDomainById(id)).thenReturn(expectedDomain);

        // Act
        Domain result = domainService.getById(id);

        // Assert
        assertThat(result).isEqualTo(expectedDomain);
        verify(domainRepository).existsById(id);
        verify(domainRepository).getDomainById(id);
    }

    @Test
    void getById_shouldThrowNotFoundExceptionWhenDomainDoesNotExist() {
        // Arrange
        Long id = 1L;

        when(domainRepository.existsById(id)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> domainService.getById(id))
                .isInstanceOf(NotFoundException.class);

        verify(domainRepository).existsById(id);
        verify(domainRepository, never()).getDomainById(anyLong());
    }

    @Test
    void deleteById_shouldDeleteDomainWhenItExists() {
        // Arrange
        Long id = 1L;

        when(domainRepository.existsById(id)).thenReturn(true);

        // Act
        domainService.deleteById(id);

        // Assert
        verify(domainRepository).existsById(id);
        verify(domainRepository).deleteDomainById(id);
    }

    @Test
    void deleteById_shouldThrowNotFoundExceptionWhenDomainDoesNotExist() {
        // Arrange
        Long id = 1L;

        when(domainRepository.existsById(id)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> domainService.deleteById(id))
                .isInstanceOf(NotFoundException.class);

        verify(domainRepository).existsById(id);
        verify(domainRepository, never()).deleteDomainById(anyLong());
    }

    @Test
    void create_shouldCreateDomainWhenNameIsUnique() {
        // Arrange
        Domain domain = DomainTestData.domain();

        when(domainRepository.existsByName(domain.name())).thenReturn(false);
        when(domainRepository.createDomain(domain)).thenReturn(domain);

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

        when(domainRepository.existsById(id)).thenReturn(true);
        when(domainRepository.existsByName(domain.name())).thenReturn(false);
        when(domainRepository.updateDomain(id, domain)).thenReturn(domain);

        // Act
        Domain result = domainService.update(id, domain);

        // Assert
        assertThat(result).isEqualTo(domain);
        verify(domainRepository).existsById(id);
        verify(domainRepository).existsByName(domain.name());
        verify(domainRepository).updateDomain(id, domain);
    }

    @Test
    void update_shouldThrowNotFoundExceptionWhenDomainDoesNotExist() {
        // Arrange
        Long id = 1L;
        Domain domain = DomainTestData.domain();

        when(domainRepository.existsById(id)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> domainService.update(id, domain))
                .isInstanceOf(NotFoundException.class);

        verify(domainRepository).existsById(id);
        verify(domainRepository, never()).existsByName(anyString());
        verify(domainRepository, never()).updateDomain(anyLong(), any());
    }

    @Test
    void update_shouldThrowEntityAlreadyExistsExceptionWhenAnotherDomainWithSameNameExists() {
        // Arrange
        Long id = 1L;
        Domain domain = DomainTestData.domain();

        when(domainRepository.existsById(id)).thenReturn(true);
        when(domainRepository.existsByName(domain.name())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> domainService.update(id, domain))
                .isInstanceOf(EntityAlreadyExistsException.class);

        verify(domainRepository).existsById(id);
        verify(domainRepository).existsByName(domain.name());
        verify(domainRepository, never()).updateDomain(anyLong(), any());
    }
}
