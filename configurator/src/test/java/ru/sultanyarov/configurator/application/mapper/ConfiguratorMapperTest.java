package ru.sultanyarov.configurator.application.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sultanyarov.configurator.domain.model.CompatibleComponent;
import ru.sultanyarov.configurator.domain.model.CompatibleComponentGroup;
import ru.sultanyarov.configurator.domain.model.ConfiguratorResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfiguratorMapperTest {
    private final ConfiguratorMapper mapper = Mappers.getMapper(ConfiguratorMapper.class);

    @Test
    void toDto_shouldMapNestedCompatibleComponents() {
        ConfiguratorResult result = new ConfiguratorResult(
                1L,
                List.of(new CompatibleComponentGroup(
                        20L,
                        "Motherboard",
                        List.of(new CompatibleComponent(2L, "Board", "Brand", 20L))
                ))
        );

        var dto = mapper.toDto(result);

        assertThat(dto.getBaseComponentId()).isEqualTo(1L);
        assertThat(dto.getCompatibleByType()).singleElement().satisfies(group -> {
            assertThat(group.getComponentTypeId()).isEqualTo(20L);
            assertThat(group.getComponentTypeName()).isEqualTo("Motherboard");
            assertThat(group.getComponents()).singleElement().satisfies(component -> {
                assertThat(component.getId()).isEqualTo(2L);
                assertThat(component.getName()).isEqualTo("Board");
                assertThat(component.getBrand()).isEqualTo("Brand");
                assertThat(component.getComponentTypeId()).isEqualTo(20L);
            });
        });
    }
}
