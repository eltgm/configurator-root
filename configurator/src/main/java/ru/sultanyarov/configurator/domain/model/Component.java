package ru.sultanyarov.configurator.domain.model;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
  private ComponentImage primaryImage;
  private LocalDateTime createdAt;
}
