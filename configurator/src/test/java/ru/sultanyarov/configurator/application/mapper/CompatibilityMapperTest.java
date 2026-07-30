package ru.sultanyarov.configurator.application.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sultanyarov.configurator.domain.model.CompatibilityGraph;
import ru.sultanyarov.configurator.domain.model.CompatibilityGraphEdge;
import ru.sultanyarov.configurator.domain.model.CompatibilityGraphNode;
import ru.sultanyarov.configurator.domain.model.CompatibilityLink;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CompatibilityMapperTest {
    private final CompatibilityMapper mapper = Mappers.getMapper(CompatibilityMapper.class);

    @Test
    void toDto_shouldMapCompatibilityGraph() {
        CompatibilityGraph graph = new CompatibilityGraph(
                List.of(new CompatibilityGraphNode(3L, "Keyboard", 10L, "Cases", "Keychron")),
                List.of(new CompatibilityGraphEdge(11L, 3L, 9L, "Same socket"))
        );

        var result = mapper.toDto(graph);

        assertThat(result.getNodes()).singleElement().satisfies(node -> {
            assertThat(node.getId()).isEqualTo(3L);
            assertThat(node.getName()).isEqualTo("Keyboard");
            assertThat(node.getComponentTypeId()).isEqualTo(10L);
            assertThat(node.getComponentTypeName()).isEqualTo("Cases");
            assertThat(node.getBrand()).isEqualTo("Keychron");
        });
        assertThat(result.getEdges()).singleElement().satisfies(edge -> {
            assertThat(edge.getId()).isEqualTo(11L);
            assertThat(edge.getSource()).isEqualTo(3L);
            assertThat(edge.getTarget()).isEqualTo(9L);
            assertThat(edge.getComment()).isEqualTo("Same socket");
        });
    }

    @Test
    void toEntity_shouldMapCreationArgumentsToDomainModel() {
        CompatibilityLink result = mapper.toEntity(1L, 9L, 3L, "Compatible");

        assertThat(result.id()).isNull();
        assertThat(result.domainId()).isEqualTo(1L);
        assertThat(result.componentAId()).isEqualTo(9L);
        assertThat(result.componentBId()).isEqualTo(3L);
        assertThat(result.comment()).isEqualTo("Compatible");
    }

    @Test
    void toDto_shouldMapDomainModel() {
        CompatibilityLink link = new CompatibilityLink(11L, 1L, 3L, 9L, "Compatible");

        ru.sultanyarov.configurator.api.inbounds.rest.dto.CompatibilityLink result = mapper.toDto(link);

        assertThat(result.getId()).isEqualTo(11L);
        assertThat(result.getDomainId()).isEqualTo(1L);
        assertThat(result.getComponentAId()).isEqualTo(3L);
        assertThat(result.getComponentBId()).isEqualTo(9L);
        assertThat(result.getComment()).isEqualTo("Compatible");
    }
}
