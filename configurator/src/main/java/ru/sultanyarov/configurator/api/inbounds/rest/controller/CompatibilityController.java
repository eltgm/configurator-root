package ru.sultanyarov.configurator.api.inbounds.rest.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ru.sultanyarov.configurator.api.inbounds.rest.CompatibilityApi;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityLink;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateCompatibilityLinkRequest;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.GraphResponse;
import ru.sultanyarov.configurator.application.facade.CompatibilityFacade;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequiredArgsConstructor
public class CompatibilityController implements CompatibilityApi {
    private final CompatibilityFacade compatibilityFacade;

    @Override
    public ResponseEntity<GraphResponse> domainsIdCompatibilityGraphGet(Long id) {
        return ResponseEntity.ok(compatibilityFacade.getCompatibilityGraph(id));
    }

    @Override
    public ResponseEntity<Void> domainsIdCompatibilityLinkIdDelete(Long id, Long linkId) {
        compatibilityFacade.deleteCompatibilityLink(id, linkId);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<CompatibilityLink> domainsIdCompatibilityPost(
            Long id,
            CreateCompatibilityLinkRequest createCompatibilityLinkRequest
    ) {
        return ResponseEntity.status(CREATED)
                .body(compatibilityFacade.createCompatibilityLink(id, createCompatibilityLinkRequest));
    }
}
