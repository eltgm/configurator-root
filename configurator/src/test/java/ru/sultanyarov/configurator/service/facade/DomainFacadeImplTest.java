package ru.sultanyarov.configurator.service.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.domain.dto.CreateDomainRequest;
import ru.sultanyarov.configurator.domain.dto.Domain;
import ru.sultanyarov.configurator.domain.dto.DomainPage;
import ru.sultanyarov.configurator.domain.dto.UpdateDomainRequest;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.Page;
import ru.sultanyarov.configurator.service.core.DomainService;
import ru.sultanyarov.configurator.service.mapper.DomainMapper;
import ru.sultanyarov.configurator.test.data.DomainTestData;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DomainFacadeImplTest {
    @Mock
    private DomainService domainService;

    @Mock
    private DomainMapper domainMapper;

    @InjectMocks
    private DomainFacadeImpl domainFacade;

    @Test
    void createDomain_shouldCreateDomainWhenRequestIsValid() {
        // Arrange
        CreateDomainRequest request = new CreateDomainRequest();
        request.setName("Test Domain");
        request.setDescription("Test Description");

        ru.sultanyarov.configurator.domain.model.Domain domain = DomainTestData.domainWithName(request.getName());
        ru.sultanyarov.configurator.domain.model.Domain createdDomain = DomainTestData.domainWithIdAndName(1L, request.getName());
        Domain expectedDto = new Domain();
        expectedDto.setId(1L);
        expectedDto.setName("Test Domain");
        expectedDto.setDescription("Test Description");
        expectedDto.setCreatedAt(createdDomain.createdAt());

        when(domainMapper.toDomain(request)).thenReturn(domain);
        when(domainService.create(any(ru.sultanyarov.configurator.domain.model.Domain.class))).thenReturn(createdDomain);
        when(domainMapper.toDomainDto(createdDomain)).thenReturn(expectedDto);

        // Act
        Domain result = domainFacade.createDomain(request);

        // Assert
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedDto);
        verify(domainMapper).toDomain(request);
        verify(domainService).create(any(ru.sultanyarov.configurator.domain.model.Domain.class));
        verify(domainMapper).toDomainDto(createdDomain);
    }

    @Test
    void createDomain_shouldThrowValidationExceptionWhenNameIsEmpty() {
        // Arrange
        CreateDomainRequest request = new CreateDomainRequest();
        request.setName("");

        // Act & Assert
        assertThatThrownBy(() -> domainFacade.createDomain(request))
                .isInstanceOf(ValidationException.class);

        verify(domainService, never()).create(any());
    }

    @Test
    void createDomain_shouldThrowValidationExceptionWhenNameIsNull() {
        // Arrange
        CreateDomainRequest request = new CreateDomainRequest();
        request.setName(null);

        // Act & Assert
        assertThatThrownBy(() -> domainFacade.createDomain(request))
                .isInstanceOf(ValidationException.class);

        verify(domainService, never()).create(any());
    }

    @Test
    void getDomainById_shouldReturnDomainWhenItExists() {
        // Arrange
        Long id = 1L;
        ru.sultanyarov.configurator.domain.model.Domain domain = DomainTestData.domainWithId(id);
        Domain expectedDto = new Domain();
        expectedDto.setId(1L);
        expectedDto.setName("Test Domain");
        expectedDto.setDescription("Test Description");
        expectedDto.setCreatedAt(domain.createdAt());

        when(domainService.getById(id)).thenReturn(domain);
        when(domainMapper.toDomainDto(domain)).thenReturn(expectedDto);

        // Act
        Domain result = domainFacade.getDomainById(id);

        // Assert
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedDto);
        verify(domainService).getById(id);
        verify(domainMapper).toDomainDto(domain);
    }

    @Test
    void getDomainById_shouldThrowNotFoundExceptionWhenDomainDoesNotExist() {
        // Arrange
        Long id = 1L;

        when(domainService.getById(id)).thenThrow(new NotFoundException("Domain not found"));

        // Act & Assert
        assertThatThrownBy(() -> domainFacade.getDomainById(id))
                .isInstanceOf(NotFoundException.class);

        verify(domainService).getById(id);
    }

    @Test
    void deleteDomainById_shouldDeleteDomainWhenItExists() {
        // Arrange
        Long id = 1L;

        // Act
        domainFacade.deleteDomainById(id);

        // Assert
        verify(domainService).deleteById(id);
    }

    @Test
    void updateDomain_shouldUpdateDomainWhenRequestIsValid() {
        // Arrange
        Long id = 1L;
        UpdateDomainRequest request = new UpdateDomainRequest();
        request.setName("Updated Domain");
        request.setDescription("Updated Description");

        ru.sultanyarov.configurator.domain.model.Domain domain = DomainTestData.domainWithName(request.getName());
        ru.sultanyarov.configurator.domain.model.Domain updatedDomain = DomainTestData.domainWithIdAndName(id, request.getName());
        Domain expectedDto = new Domain();
        expectedDto.setId(1L);
        expectedDto.setName("Updated Domain");
        expectedDto.setDescription("Updated Description");
        expectedDto.setCreatedAt(updatedDomain.createdAt());

        when(domainMapper.toDomain(request)).thenReturn(domain);
        when(domainService.update(anyLong(), any(ru.sultanyarov.configurator.domain.model.Domain.class))).thenReturn(updatedDomain);
        when(domainMapper.toDomainDto(updatedDomain)).thenReturn(expectedDto);

        // Act
        Domain result = domainFacade.updateDomain(id, request);

        // Assert
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedDto);
        verify(domainMapper).toDomain(request);
        verify(domainService).update(anyLong(), any(ru.sultanyarov.configurator.domain.model.Domain.class));
        verify(domainMapper).toDomainDto(updatedDomain);
    }

    @Test
    void updateDomain_shouldThrowValidationExceptionWhenNameIsEmpty() {
        // Arrange
        Long id = 1L;
        UpdateDomainRequest request = new UpdateDomainRequest();
        request.setName("");

        // Act & Assert
        assertThatThrownBy(() -> domainFacade.updateDomain(id, request))
                .isInstanceOf(ValidationException.class);

        verify(domainService, never()).update(anyLong(), any());
    }

    @Test
    void updateDomain_shouldThrowValidationExceptionWhenNameIsNull() {
        // Arrange
        Long id = 1L;
        UpdateDomainRequest request = new UpdateDomainRequest();
        request.setName(null);

        // Act & Assert
        assertThatThrownBy(() -> domainFacade.updateDomain(id, request))
                .isInstanceOf(ValidationException.class);

        verify(domainService, never()).update(anyLong(), any());
    }

    @Test
    void getDomains_shouldReturnPageOfDomains() {
        // Arrange
        int page = 0;
        int pageSize = 10;
        List<ru.sultanyarov.configurator.domain.model.Domain> domains = List.of(DomainTestData.domain(), DomainTestData.domain());
        Page<ru.sultanyarov.configurator.domain.model.Domain> domainPage = new Page<>(domains, page, pageSize, 2);

        Domain domainDto1 = new Domain();
        domainDto1.setId(1L);
        domainDto1.setName("Test Domain");
        domainDto1.setDescription("Test Description");
        domainDto1.setCreatedAt(domains.get(0).createdAt());

        Domain domainDto2 = new Domain();
        domainDto2.setId(2L);
        domainDto2.setName("Test Domain");
        domainDto2.setDescription("Test Description");
        domainDto2.setCreatedAt(domains.get(1).createdAt());

        DomainPage expectedDto = new DomainPage();
        expectedDto.setItems(List.of(domainDto1, domainDto2));
        expectedDto.setPage(0);
        expectedDto.setSize(10);
        expectedDto.setTotalItems(2);

        when(domainService.getPage(page, pageSize)).thenReturn(domainPage);
        when(domainMapper.toDomainPageDto(domainPage)).thenReturn(expectedDto);

        // Act
        DomainPage result = domainFacade.getDomains(page, pageSize);

        // Assert
        assertThat(result).usingRecursiveComparison().isEqualTo(expectedDto);
        verify(domainService).getPage(page, pageSize);
        verify(domainMapper).toDomainPageDto(domainPage);
    }
}
