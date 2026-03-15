package ru.sultanyarov.configurator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public final class Component {
    private Long id;
    private Long componentTypeId;
    private String name;
    private String brand;
    private String description;
    private Boolean archived;
    private List<AttributeValue> attributes;
    private List<ComponentImage> images;
    private LocalDateTime createdAt;
}
