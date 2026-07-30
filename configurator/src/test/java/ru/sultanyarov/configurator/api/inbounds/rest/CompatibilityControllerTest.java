package ru.sultanyarov.configurator.api.inbounds.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.sultanyarov.configurator.api.inbounds.rest.controller.CompatibilityController;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityLink;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateCompatibilityLinkRequest;
import ru.sultanyarov.configurator.application.facade.CompatibilityFacade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompatibilityControllerTest {

    @Mock
    private CompatibilityFacade compatibilityFacade;

    @InjectMocks
    private CompatibilityController compatibilityController;

    @Test
    void domainsIdCompatibilityPost_shouldDelegateCreationToFacade() {
        CreateCompatibilityLinkRequest request = new CreateCompatibilityLinkRequest(9L, 3L);
        CompatibilityLink createdLink = new CompatibilityLink(11L, 1L, 3L, 9L)
                .comment("Compatible");
        when(compatibilityFacade.createCompatibilityLink(1L, request)).thenReturn(createdLink);

        ResponseEntity<CompatibilityLink> response =
                compatibilityController.domainsIdCompatibilityPost(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(createdLink);
        verify(compatibilityFacade).createCompatibilityLink(1L, request);
    }
}
