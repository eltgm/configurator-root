package ru.sultanyarov.configurator.application.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.Component;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ComponentPage;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.CreateComponentRequest;
import ru.sultanyarov.configurator.application.mapper.ComponentMapper;
import ru.sultanyarov.configurator.application.service.ComponentService;
import ru.sultanyarov.configurator.domain.model.Page;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComponentFacadeImplTest {

    @Mock
    private ComponentService componentService;

    @Mock
    private ComponentMapper componentMapper;

    @InjectMocks
    private ComponentFacadeImpl componentFacade;

    @Test
    void createComponent_shouldMapRequestToEntityAndBackToDto() {
        CreateComponentRequest request = new CreateComponentRequest();
        request.setComponentTypeId(1L);
        ru.sultanyarov.configurator.domain.model.Component entity = new ru.sultanyarov.configurator.domain.model.Component();
        ru.sultanyarov.configurator.domain.model.Component createdEntity = new ru.sultanyarov.configurator.domain.model.Component();
        Component dto = new Component();

        when(componentMapper.toEntity(request)).thenReturn(entity);
        when(componentService.create(entity)).thenReturn(createdEntity);
        when(componentMapper.toDto(createdEntity)).thenReturn(dto);

        Component result = componentFacade.createComponent(request);

        assertThat(result).isSameAs(dto);
        verify(componentMapper).toEntity(request);
        verify(componentService).create(entity);
        verify(componentMapper).toDto(createdEntity);
    }

    @Test
    void getComponentsByDomainId_shouldDelegateSearchToServiceAndMapPageToDto() {
        Page<ru.sultanyarov.configurator.domain.model.Component> page =
                new Page<>(List.of(), 0, 10, 0);
        ComponentPage pageDto = new ComponentPage();

        when(componentService.getByPageByDomainId(1L, 2L, "name", 0, 10))
                .thenReturn(page);
        when(componentMapper.toComponentPageDto(page)).thenReturn(pageDto);

        ComponentPage result = componentFacade.getComponentsByDomainId(1L, 2L, "name", 0, 10);

        assertThat(result).isSameAs(pageDto);
        verify(componentService).getByPageByDomainId(1L, 2L, "name", 0, 10);
        verify(componentMapper).toComponentPageDto(page);
    }

    @Test
    void getComponentById_shouldDelegateRetrievalToServiceAndMapToDto() {
        ru.sultanyarov.configurator.domain.model.Component entity =
                ru.sultanyarov.configurator.domain.model.Component.builder().id(1L).build();
        Component dto = new Component();

        when(componentService.getById(1L)).thenReturn(entity);
        when(componentMapper.toDto(entity)).thenReturn(dto);

        Component result = componentFacade.getComponentById(1L);

        assertThat(result).isSameAs(dto);
        verify(componentService).getById(1L);
        verify(componentMapper).toDto(entity);
    }
}
