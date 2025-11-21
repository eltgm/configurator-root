package ru.sultanyarov.configurator.service.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.sultanyarov.configurator.domain.dto.CreateDomainRequest;
import ru.sultanyarov.configurator.domain.dto.DomainPage;
import ru.sultanyarov.configurator.domain.dto.UpdateDomainRequest;
import ru.sultanyarov.configurator.domain.model.Domain;
import ru.sultanyarov.configurator.domain.model.Page;

import java.time.OffsetDateTime;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

class DomainMapperTest {

    private final DomainMapper domainMapper = Mappers.getMapper(DomainMapper.class);

    @Test
    void toDomainPageDto_shouldMapCorrectly_whenPageIsNotEmpty() {
        // Arrange
        List<Domain> domains = List.of(createDomain("test-domain", "test description"));
        Page<Domain> domainPage = new Page<>(domains, 1, 10, 100L);

        // Act
        DomainPage result = domainMapper.toDomainPageDto(domainPage);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(dp -> {
                    assertThat(dp.getPage()).isEqualTo(1);
                    assertThat(dp.getSize()).isEqualTo(10);
                    assertThat(dp.getTotalItems()).isEqualTo(100);
                    assertThat(dp.getItems()).hasSize(1);
                    assertThat(dp.getItems().get(0))
                            .satisfies(d -> {
                                assertThat(d.getName()).isEqualTo("test-domain");
                                assertThat(d.getDescription()).isEqualTo("test description");
                            });
                });
    }

    @Test
    void toDomainPageDto_shouldReturnNull_whenPageIsNull() {
        // Arrange
        Page<Domain> domainPage = null;

        // Act
        DomainPage result = domainMapper.toDomainPageDto(domainPage);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void toDomainDto_shouldMapCorrectly_whenDomainIsNotNull() {
        // Arrange
        Domain domain = createDomain("test-domain", "test description");

        // Act
        ru.sultanyarov.configurator.domain.dto.Domain result = domainMapper.toDomainDto(domain);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(d -> {
                    assertThat(d.getId()).isEqualTo(domain.id());
                    assertThat(d.getName()).isEqualTo(domain.name());
                    assertThat(d.getDescription()).isEqualTo(domain.description());
                    assertThat(d.getCreatedAt()).isEqualTo(domain.createdAt());
                });
    }

    @Test
    void toDomainDto_shouldReturnNull_whenDomainIsNull() {
        // Arrange
        Domain domain = null;

        // Act
        ru.sultanyarov.configurator.domain.dto.Domain result = domainMapper.toDomainDto(domain);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void toDomain_withCreateDomainRequest_shouldMapCorrectly_whenRequestIsNotNull() {
        // Arrange
        CreateDomainRequest request = createCreateDomainRequest("new-domain", "new description");

        // Act
        Domain result = domainMapper.toDomain(request);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(d -> {
                    assertThat(d.id()).isNull();
                    assertThat(d.name()).isEqualTo(request.getName());
                    assertThat(d.description()).isEqualTo(request.getDescription());
                    assertThat(d.createdByUserId()).isEqualTo(-1L);
                    assertThat(d.createdAt()).isNull();
                });
    }

    @Test
    void toDomain_withCreateDomainRequest_shouldReturnNull_whenRequestIsNull() {
        // Arrange
        CreateDomainRequest request = null;

        // Act
        Domain result = domainMapper.toDomain(request);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void toDomain_withUpdateDomainRequest_shouldMapCorrectly_whenRequestIsNotNull() {
        // Arrange
        UpdateDomainRequest request = createUpdateDomainRequest("updated-domain", "updated description");

        // Act
        Domain result = domainMapper.toDomain(request);

        // Assert
        assertThat(result)
                .isNotNull()
                .satisfies(d -> {
                    assertThat(d.id()).isNull();
                    assertThat(d.name()).isEqualTo(request.getName());
                    assertThat(d.description()).isEqualTo(request.getDescription());
                    assertThat(d.createdByUserId()).isEqualTo(-1L);
                    assertThat(d.createdAt()).isNull();
                });
    }

    @Test
    void toDomain_withUpdateDomainRequest_shouldReturnNull_whenRequestIsNull() {
        // Arrange
        UpdateDomainRequest request = null;

        // Act
        Domain result = domainMapper.toDomain(request);

        // Assert
        assertThat(result).isNull();
    }

    private Domain createDomain(String name, String description) {
        return new Domain(
                1L,
                name,
                description,
                100L,
                OffsetDateTime.now().truncatedTo(SECONDS)
        );
    }

    private CreateDomainRequest createCreateDomainRequest(String name, String description) {
        CreateDomainRequest request = new CreateDomainRequest();
        request.setName(name);
        request.setDescription(description);
        return request;
    }

    private UpdateDomainRequest createUpdateDomainRequest(String name, String description) {
        UpdateDomainRequest request = new UpdateDomainRequest();
        request.setName(name);
        request.setDescription(description);
        return request;
    }
}