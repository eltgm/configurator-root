package ru.sultanyarov.configurator.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.application.port.out.CompatibilityRepository;
import ru.sultanyarov.configurator.domain.exception.ComponentArchivedException;
import ru.sultanyarov.configurator.domain.exception.EntityAlreadyExistsException;
import ru.sultanyarov.configurator.domain.exception.NotFoundException;
import ru.sultanyarov.configurator.domain.exception.ValidationException;
import ru.sultanyarov.configurator.domain.model.Component;
import ru.sultanyarov.configurator.domain.model.CompatibilityGraph;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;
import ru.sultanyarov.configurator.domain.model.ComponentType;
import ru.sultanyarov.configurator.domain.model.Domain;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompatibilityServiceImplTest {

    @Mock
    private CompatibilityRepository compatibilityRepository;

    @Mock
    private DomainService domainService;

    @Mock
    private ComponentService componentService;

    @InjectMocks
    private CompatibilityServiceImpl compatibilityService;

    @Test
    void getGraphByDomainId_shouldVerifyDomainAndLoadGraph() {
        CompatibilityGraph graph = new CompatibilityGraph(List.of(), List.of());
        when(domainService.getById(1L)).thenReturn(domain(1L));
        when(compatibilityRepository.getGraphByDomainId(1L)).thenReturn(graph);

        CompatibilityGraph result = compatibilityService.getGraphByDomainId(1L);

        assertThat(result).isSameAs(graph);
        verify(domainService).getById(1L);
        verify(compatibilityRepository).getGraphByDomainId(1L);
    }

    @Test
    void getGraphByDomainId_shouldNotAccessRepositoryWhenDomainDoesNotExist() {
        when(domainService.getById(999L))
                .thenThrow(new NotFoundException("Domain with id 999 not found"));

        assertThatThrownBy(() -> compatibilityService.getGraphByDomainId(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Domain with id 999 not found");

        verifyNoInteractions(compatibilityRepository);
    }

    @Test
    void create_shouldAllowSameTypeAndPersistNormalizedUndirectedLink() {
        Domain domain = domain(1L);
        Component componentNine = component(9L, 10L, false);
        Component componentThree = component(3L, 10L, false);
        CompatibilityLink requestedLink = new CompatibilityLink(null, 1L, 9L, 3L, "Compatible");
        CompatibilityLink normalizedLink = new CompatibilityLink(null, 1L, 3L, 9L, "Compatible");
        CompatibilityLink createdLink = new CompatibilityLink(11L, 1L, 3L, 9L, "Compatible");

        when(domainService.getById(1L)).thenReturn(domain);
        when(componentService.getById(9L)).thenReturn(componentNine);
        when(componentService.getById(3L)).thenReturn(componentThree);
        when(compatibilityRepository.create(normalizedLink)).thenReturn(Optional.of(createdLink));

        CompatibilityLink result = compatibilityService.create(requestedLink);

        assertThat(result).isSameAs(createdLink);
        verify(compatibilityRepository).create(normalizedLink);
    }

    @Test
    void create_shouldRejectSelfLinkBeforeLoadingComponents() {
        CompatibilityLink requestedLink = new CompatibilityLink(null, 1L, 3L, 3L, null);
        when(domainService.getById(1L)).thenReturn(domain(1L));

        assertThatThrownBy(() -> compatibilityService.create(requestedLink))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Component with id 3 cannot be compatible with itself");

        verify(componentService, never()).getById(3L);
        verify(compatibilityRepository, never()).create(requestedLink);
    }

    @Test
    void create_shouldRejectComponentFromAnotherDomain() {
        CompatibilityLink requestedLink = new CompatibilityLink(null, 1L, 3L, 9L, null);
        when(domainService.getById(1L)).thenReturn(domain(1L));
        when(componentService.getById(3L)).thenReturn(component(3L, 10L, false));
        when(componentService.getById(9L)).thenReturn(component(9L, 99L, false));

        assertThatThrownBy(() -> compatibilityService.create(requestedLink))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Component with id 9 does not belong to domain with id 1");

        verify(compatibilityRepository, never()).create(requestedLink);
    }

    @Test
    void create_shouldRejectArchivedFirstComponent() {
        CompatibilityLink requestedLink = new CompatibilityLink(null, 1L, 3L, 9L, null);
        when(domainService.getById(1L)).thenReturn(domain(1L));
        when(componentService.getById(3L)).thenReturn(component(3L, 10L, true));
        when(componentService.getById(9L)).thenReturn(component(9L, 10L, false));

        assertThatThrownBy(() -> compatibilityService.create(requestedLink))
                .isInstanceOf(ComponentArchivedException.class)
                .hasMessage("Cannot create compatibility link for archived component with id 3");

        verify(compatibilityRepository, never()).create(requestedLink);
    }

    @Test
    void create_shouldRejectArchivedSecondComponent() {
        CompatibilityLink requestedLink = new CompatibilityLink(null, 1L, 3L, 9L, null);
        when(domainService.getById(1L)).thenReturn(domain(1L));
        when(componentService.getById(3L)).thenReturn(component(3L, 10L, false));
        when(componentService.getById(9L)).thenReturn(component(9L, 10L, true));

        assertThatThrownBy(() -> compatibilityService.create(requestedLink))
                .isInstanceOf(ComponentArchivedException.class)
                .hasMessage("Cannot create compatibility link for archived component with id 9");

        verify(compatibilityRepository, never()).create(requestedLink);
    }

    @Test
    void create_shouldReturnConflictWhenNormalizedLinkAlreadyExists() {
        CompatibilityLink requestedLink = new CompatibilityLink(null, 1L, 9L, 3L, null);
        CompatibilityLink normalizedLink = new CompatibilityLink(null, 1L, 3L, 9L, null);
        when(domainService.getById(1L)).thenReturn(domain(1L));
        when(componentService.getById(9L)).thenReturn(component(9L, 10L, false));
        when(componentService.getById(3L)).thenReturn(component(3L, 10L, false));
        when(compatibilityRepository.create(normalizedLink)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> compatibilityService.create(requestedLink))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Compatibility link between components 3 and 9 already exists in domain 1");
    }

    @Test
    void deleteByIdAndDomainId_shouldDeleteScopedLink() {
        when(domainService.getById(1L)).thenReturn(domain(1L));
        when(compatibilityRepository.deleteByIdAndDomainId(11L, 1L)).thenReturn(true);

        compatibilityService.deleteByIdAndDomainId(11L, 1L);

        verify(domainService).getById(1L);
        verify(compatibilityRepository).deleteByIdAndDomainId(11L, 1L);
    }

    @Test
    void deleteByIdAndDomainId_shouldThrowNotFoundWhenScopedLinkDoesNotExist() {
        when(domainService.getById(1L)).thenReturn(domain(1L));
        when(compatibilityRepository.deleteByIdAndDomainId(11L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> compatibilityService.deleteByIdAndDomainId(11L, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Compatibility link with id 11 not found in domain with id 1");
    }

    @Test
    void deleteByIdAndDomainId_shouldNotAccessRepositoryWhenDomainDoesNotExist() {
        when(domainService.getById(999L))
                .thenThrow(new NotFoundException("Domain with id 999 not found"));

        assertThatThrownBy(() -> compatibilityService.deleteByIdAndDomainId(11L, 999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Domain with id 999 not found");

        verifyNoInteractions(compatibilityRepository);
    }

    private static Domain domain(Long id) {
        return Domain.builder()
                .id(id)
                .componentTypes(List.of(
                        ComponentType.builder().id(10L).domainId(id).build(),
                        ComponentType.builder().id(11L).domainId(id).build()
                ))
                .build();
    }

    private static Component component(Long id, Long componentTypeId, boolean archived) {
        return Component.builder()
                .id(id)
                .componentTypeId(componentTypeId)
                .archived(archived)
                .build();
    }
}
