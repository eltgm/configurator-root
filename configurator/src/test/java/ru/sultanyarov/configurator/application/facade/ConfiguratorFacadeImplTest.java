package ru.sultanyarov.configurator.application.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.ConfiguratorResponse;
import ru.sultanyarov.configurator.application.mapper.ConfiguratorMapper;
import ru.sultanyarov.configurator.application.service.ConfiguratorService;
import ru.sultanyarov.configurator.domain.model.ConfiguratorResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfiguratorFacadeImplTest {
    @Mock
    private ConfiguratorService configuratorService;
    @Mock
    private ConfiguratorMapper configuratorMapper;
    @InjectMocks
    private ConfiguratorFacadeImpl facade;

    @Test
    void getCompatibleComponents_shouldMapServiceResult() {
        ConfiguratorResult result = new ConfiguratorResult(7L, List.of());
        ConfiguratorResponse response = new ConfiguratorResponse(7L, List.of());
        when(configuratorService.getCompatibleComponents(1L, 7L)).thenReturn(result);
        when(configuratorMapper.toDto(result)).thenReturn(response);

        assertThat(facade.getCompatibleComponents(1L, 7L)).isSameAs(response);
        verify(configuratorService).getCompatibleComponents(1L, 7L);
        verify(configuratorMapper).toDto(result);
    }
}
