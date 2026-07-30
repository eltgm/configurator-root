package ru.sultanyarov.configurator.application.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityLink;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateCompatibilityLinkRequest;
import ru.sultanyarov.configurator.application.mapper.CompatibilityMapper;
import ru.sultanyarov.configurator.application.service.CompatibilityService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompatibilityFacadeImplTest {

    @Mock
    private CompatibilityService compatibilityService;

    @Mock
    private CompatibilityMapper compatibilityMapper;

    @InjectMocks
    private CompatibilityFacadeImpl compatibilityFacade;

    @Test
    void createCompatibilityLink_shouldTrimCommentAndMapCreatedLink() {
        CreateCompatibilityLinkRequest request = new CreateCompatibilityLinkRequest(9L, 3L)
                .comment("  Compatible  ");
        ru.sultanyarov.configurator.domain.model.CompatibilityLink requestedLink =
                new ru.sultanyarov.configurator.domain.model.CompatibilityLink(null, 1L, 9L, 3L, "Compatible");
        ru.sultanyarov.configurator.domain.model.CompatibilityLink createdLink =
                new ru.sultanyarov.configurator.domain.model.CompatibilityLink(11L, 1L, 3L, 9L, "Compatible");
        CompatibilityLink dto = new CompatibilityLink(11L, 1L, 3L, 9L).comment("Compatible");

        when(compatibilityMapper.toEntity(1L, 9L, 3L, "Compatible")).thenReturn(requestedLink);
        when(compatibilityService.create(requestedLink)).thenReturn(createdLink);
        when(compatibilityMapper.toDto(createdLink)).thenReturn(dto);

        CompatibilityLink result = compatibilityFacade.createCompatibilityLink(1L, request);

        assertThat(result).isSameAs(dto);
        verify(compatibilityMapper).toEntity(1L, 9L, 3L, "Compatible");
        verify(compatibilityService).create(requestedLink);
        verify(compatibilityMapper).toDto(createdLink);
    }

    @Test
    void createCompatibilityLink_shouldNormalizeBlankCommentToNull() {
        CreateCompatibilityLinkRequest request = new CreateCompatibilityLinkRequest(3L, 9L)
                .comment("   ");
        ru.sultanyarov.configurator.domain.model.CompatibilityLink requestedLink =
                new ru.sultanyarov.configurator.domain.model.CompatibilityLink(null, 1L, 3L, 9L, null);
        ru.sultanyarov.configurator.domain.model.CompatibilityLink createdLink =
                new ru.sultanyarov.configurator.domain.model.CompatibilityLink(11L, 1L, 3L, 9L, null);
        CompatibilityLink dto = new CompatibilityLink(11L, 1L, 3L, 9L);

        when(compatibilityMapper.toEntity(1L, 3L, 9L, null)).thenReturn(requestedLink);
        when(compatibilityService.create(requestedLink)).thenReturn(createdLink);
        when(compatibilityMapper.toDto(createdLink)).thenReturn(dto);

        CompatibilityLink result = compatibilityFacade.createCompatibilityLink(1L, request);

        assertThat(result).isSameAs(dto);
        verify(compatibilityMapper).toEntity(1L, 3L, 9L, null);
    }

    @Test
    void deleteCompatibilityLink_shouldDelegateToService() {
        compatibilityFacade.deleteCompatibilityLink(1L, 11L);

        verify(compatibilityService).deleteByIdAndDomainId(11L, 1L);
    }
}
