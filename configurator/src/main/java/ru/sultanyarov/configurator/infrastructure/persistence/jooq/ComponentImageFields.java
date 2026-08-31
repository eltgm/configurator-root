package ru.sultanyarov.configurator.infrastructure.persistence.jooq;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.row;
import static org.jooq.impl.DSL.select;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT;
import static ru.sultanyarov.configurator.domain.entity.jooq.Tables.COMPONENT_IMAGE;

import org.jooq.Record;
import org.jooq.SelectField;
import ru.sultanyarov.configurator.domain.model.ComponentImage;

/** Fetches one ordered image per component within the enclosing SQL query. */
final class ComponentImageFields {
  private static final String PRIMARY_IMAGE = "primary_image";

  private ComponentImageFields() {}

  static SelectField<ComponentImage> primaryImage() {
    return field(
            select(
                    row(
                            COMPONENT_IMAGE.ID,
                            COMPONENT_IMAGE.COMPONENT_ID,
                            COMPONENT_IMAGE.FILE_PATH,
                            COMPONENT_IMAGE.ORDER_INDEX)
                        .mapping(ComponentImage::new))
                .from(COMPONENT_IMAGE)
                .where(COMPONENT_IMAGE.COMPONENT_ID.eq(COMPONENT.ID))
                .orderBy(COMPONENT_IMAGE.ORDER_INDEX.asc().nullsLast(), COMPONENT_IMAGE.ID.asc())
                .limit(1))
        .as(PRIMARY_IMAGE);
  }

  static ComponentImage readPrimaryImage(Record record) {
    return record.field(PRIMARY_IMAGE) == null
        ? null
        : record.get(PRIMARY_IMAGE, ComponentImage.class);
  }
}
