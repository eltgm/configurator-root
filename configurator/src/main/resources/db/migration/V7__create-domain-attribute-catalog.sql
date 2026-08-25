ALTER TABLE attribute_definition
    ADD COLUMN domain_id BIGINT;

UPDATE attribute_definition ad
SET domain_id = (
    SELECT ct.domain_id
    FROM component_type ct
    WHERE ct.id = ad.component_type_id
);

ALTER TABLE attribute_definition
    ALTER COLUMN domain_id SET NOT NULL;

ALTER TABLE attribute_definition
    ADD CONSTRAINT fk_attribute_definition_domain
        FOREIGN KEY (domain_id)
            REFERENCES domain (id)
            ON DELETE CASCADE;

CREATE TABLE component_type_attribute
(
    component_type_id       BIGINT    NOT NULL,
    attribute_definition_id BIGINT    NOT NULL,
    is_required             BOOLEAN   NOT NULL DEFAULT FALSE,
    order_index             INT,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_component_type_attribute
        PRIMARY KEY (component_type_id, attribute_definition_id),

    CONSTRAINT fk_component_type_attribute_component_type
        FOREIGN KEY (component_type_id)
            REFERENCES component_type (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_component_type_attribute_definition
        FOREIGN KEY (attribute_definition_id)
            REFERENCES attribute_definition (id)
            ON DELETE CASCADE
);

INSERT INTO component_type_attribute (
    component_type_id,
    attribute_definition_id,
    is_required,
    order_index,
    created_at
)
SELECT component_type_id,
       id,
       is_required,
       order_index,
       created_at
FROM attribute_definition;

ALTER TABLE attribute_definition
    DROP CONSTRAINT fk_attribute_definition_component_type;

DROP INDEX ux_attribute_definition_component_type_name;
DROP INDEX ix_attribute_definition_component_type_id;

ALTER TABLE attribute_definition
    DROP COLUMN component_type_id;

ALTER TABLE attribute_definition
    DROP COLUMN is_required;

ALTER TABLE attribute_definition
    DROP COLUMN order_index;

CREATE INDEX ix_attribute_definition_domain_id
    ON attribute_definition (domain_id);

CREATE INDEX ix_attribute_definition_domain_name
    ON attribute_definition (domain_id, name);

CREATE INDEX ix_component_type_attribute_definition_id
    ON component_type_attribute (attribute_definition_id);

CREATE INDEX ix_component_type_attribute_order
    ON component_type_attribute (component_type_id, order_index, attribute_definition_id);

ALTER TABLE compatibility_rule_condition
    DROP CONSTRAINT chk_compatibility_rule_condition_distinct_attributes;
