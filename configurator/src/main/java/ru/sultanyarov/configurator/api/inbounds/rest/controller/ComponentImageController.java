package ru.sultanyarov.configurator.api.inbounds.rest.controller;

import static org.springframework.http.HttpHeaders.CACHE_CONTROL;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.api.inbounds.rest.ComponentImagesApi;
import ru.sultanyarov.configurator.application.facade.ComponentFacade;
import ru.sultanyarov.configurator.domain.model.ComponentImageContent;

@RestController
@RequiredArgsConstructor
public class ComponentImageController implements ComponentImagesApi {
  private final ComponentFacade componentFacade;

  @Override
  public ResponseEntity<Void> deleteComponentImagesById(Long id) {
    componentFacade.deleteComponentImage(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Resource> getComponentImagesByIdContent(Long id) {
    return imageResponse(componentFacade.getComponentImageContent(id));
  }

  @Override
  public ResponseEntity<Resource> getComponentImagesByIdThumbnail(Long id) {
    return imageResponse(componentFacade.getComponentImageThumbnail(id));
  }

  private ResponseEntity<Resource> imageResponse(ComponentImageContent image) {
    byte[] content = image.content();

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(image.contentType()))
        .contentLength(image.contentLength())
        .header(CACHE_CONTROL, "private, no-cache")
        .headers(headers -> headers.setContentDisposition(ContentDisposition.inline().build()))
        .body(new ByteArrayResource(content));
  }
}
