package ru.sultanyarov.configurator.api.inbounds.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.sultanyarov.configurator.api.inbounds.rest.controller.ComponentImageController;
import ru.sultanyarov.configurator.application.facade.ComponentFacade;
import ru.sultanyarov.configurator.domain.model.ComponentImageContent;

@ExtendWith(MockitoExtension.class)
class ComponentImageControllerTest {
  @Mock private ComponentFacade componentFacade;

  @InjectMocks private ComponentImageController controller;

  @Test
  void componentImagesIdContentGet_shouldReturnOriginalContentAndSafeHeaders() throws Exception {
    byte[] bytes = new byte[] {1, 2, 3};
    when(componentFacade.getComponentImageContent(42L))
        .thenReturn(new ComponentImageContent(bytes, "image/png"));

    ResponseEntity<Resource> response = controller.componentImagesIdContentGet(42L);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getHeaders().getContentType().toString()).isEqualTo("image/png");
    assertThat(response.getHeaders().getContentLength()).isEqualTo(bytes.length);
    assertThat(response.getHeaders().getFirst(HttpHeaders.CACHE_CONTROL))
        .isEqualTo("private, no-cache");
    assertThat(response.getHeaders().getContentDisposition().getType()).isEqualTo("inline");
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getContentAsByteArray()).containsExactly(bytes);
    verify(componentFacade).getComponentImageContent(42L);
  }
}
