package ru.sultanyarov.configurator.application.facade;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.sultanyarov.configurator.api.inbounds.rest.dto.SaveCompatibilityRuleSetRequest;
import ru.sultanyarov.configurator.application.mapper.CompatibilityRuleMapper;
import ru.sultanyarov.configurator.application.service.CompatibilityRuleService;
import ru.sultanyarov.configurator.domain.model.CompatibilityRuleSet;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompatibilityRuleFacadeImplTest {
    @Mock
    private CompatibilityRuleService compatibilityRuleService;
    @Mock
    private CompatibilityRuleMapper compatibilityRuleMapper;
    @InjectMocks
    private CompatibilityRuleFacadeImpl facade;

    @Test
    void create_shouldMapDelegateAndMapResponse() {
        SaveCompatibilityRuleSetRequest request = request();
        CompatibilityRuleSet entity = entity(7L);
        var dto = dto(7L);
        when(compatibilityRuleMapper.toEntity(1L, request)).thenReturn(entity);
        when(compatibilityRuleService.create(entity)).thenReturn(entity);
        when(compatibilityRuleMapper.toDto(entity)).thenReturn(dto);

        assertThat(facade.create(1L, request)).isSameAs(dto);
    }

    @Test
    void getAllByDomainId_shouldMapServiceResult() {
        List<CompatibilityRuleSet> entities = List.of(entity(7L));
        List<ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleSet> dtos =
                List.of(dto(7L));
        when(compatibilityRuleService.getAllByDomainId(1L)).thenReturn(entities);
        when(compatibilityRuleMapper.toDtos(entities)).thenReturn(dtos);

        assertThat(facade.getAllByDomainId(1L)).isSameAs(dtos);
    }

    @Test
    void getByIdAndDomainId_shouldDelegateWithRuleIdFirst() {
        CompatibilityRuleSet entity = entity(7L);
        var dto = dto(7L);
        when(compatibilityRuleService.getByIdAndDomainId(7L, 1L)).thenReturn(entity);
        when(compatibilityRuleMapper.toDto(entity)).thenReturn(dto);

        assertThat(facade.getByIdAndDomainId(7L, 1L)).isSameAs(dto);
    }

    @Test
    void updateByIdAndDomainId_shouldMapReplacementAndResponse() {
        SaveCompatibilityRuleSetRequest request = request();
        CompatibilityRuleSet entity = entity(null);
        CompatibilityRuleSet updated = entity(7L);
        var dto = dto(7L);
        when(compatibilityRuleMapper.toEntity(1L, request)).thenReturn(entity);
        when(compatibilityRuleService.updateByIdAndDomainId(7L, 1L, entity))
                .thenReturn(updated);
        when(compatibilityRuleMapper.toDto(updated)).thenReturn(dto);

        assertThat(facade.updateByIdAndDomainId(7L, 1L, request)).isSameAs(dto);
    }

    @Test
    void deleteByIdAndDomainId_shouldDelegate() {
        facade.deleteByIdAndDomainId(7L, 1L);

        verify(compatibilityRuleService).deleteByIdAndDomainId(7L, 1L);
    }

    private static SaveCompatibilityRuleSetRequest request() {
        return new SaveCompatibilityRuleSetRequest("Rule", 10L, 20L, true, List.of());
    }

    private static CompatibilityRuleSet entity(Long id) {
        return CompatibilityRuleSet.builder()
                .id(id)
                .domainId(1L)
                .name("Rule")
                .componentTypeAId(10L)
                .componentTypeBId(20L)
                .enabled(true)
                .conditions(List.of())
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleSet dto(Long id) {
        return new ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityRuleSet(
                id,
                1L,
                "Rule",
                10L,
                20L,
                true,
                List.of(),
                LocalDateTime.now()
        );
    }
}
