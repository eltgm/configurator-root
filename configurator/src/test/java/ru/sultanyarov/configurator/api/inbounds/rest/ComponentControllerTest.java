package ru.sultanyarov.configurator.api.inbounds.rest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.sultanyarov.configurator.api.inbounds.rest.controller.ComponentController;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.application.facade.ComponentFacade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentControllerTest {

    @Mock
    private ComponentFacade componentFacade;

    @InjectMocks
    private ComponentController componentController;

    @Test
    void componentsPost_shouldDelegateCreationToFacade() {
        CreateComponentRequest request = new CreateComponentRequest();
        Component component = new Component();

        when(componentFacade.createComponent(request)).thenReturn(component);

        ResponseEntity<Component> response = componentController.componentsPost(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isSameAs(component);
        verify(componentFacade).createComponent(request);
    }

    @Test
    void domainsDomainIdComponentsGet_shouldDelegateSearchToFacade() {
        ComponentPage componentPage = new ComponentPage();

        when(componentFacade.getComponentsByDomainId(1L, 2L, "name", 0, 10))
                .thenReturn(componentPage);

        ResponseEntity<ComponentPage> response =
                componentController.domainsDomainIdComponentsGet(1L, 2L, "name", 0, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(componentPage);
        verify(componentFacade).getComponentsByDomainId(1L, 2L, "name", 0, 10);
    }

    @Test
    void stubbedEndpoints_shouldReturnNullUntilImplemented() {
        assertThat(componentController.componentsIdDelete(1L)).isNull();
        assertThat(componentController.componentsIdGet(1L)).isNull();
        assertThat(componentController.componentsIdImagesGet(1L)).isNull();
        assertThat(componentController.componentsIdImagesPost(1L, null, 1)).isNull();
        assertThat(componentController.componentsIdPut(1L, new CreateComponentRequest())).isNull();
    }
}
