CREATE TABLE compatibility_rule_set
(
    id                  BIGSERIAL PRIMARY KEY,
    domain_id           BIGINT       NOT NULL,
    name                VARCHAR(255) NOT NULL,
    component_type_a_id BIGINT       NOT NULL,
    component_type_b_id BIGINT       NOT NULL,
    enabled             BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_compatibility_rule_set_domain
        FOREIGN KEY (domain_id)
            REFERENCES domain (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_compatibility_rule_set_component_type_a
        FOREIGN KEY (component_type_a_id)
            REFERENCES component_type (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_compatibility_rule_set_component_type_b
        FOREIGN KEY (component_type_b_id)
            REFERENCES component_type (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_compatibility_rule_set_name
        CHECK (TRIM(name) <> ''),

    CONSTRAINT chk_compatibility_rule_set_component_type_order
        CHECK (component_type_a_id < component_type_b_id)
);

CREATE UNIQUE INDEX ux_compatibility_rule_set_domain_pair_name
    ON compatibility_rule_set (domain_id, component_type_a_id, component_type_b_id, name);

CREATE INDEX ix_compatibility_rule_set_domain_id
    ON compatibility_rule_set (domain_id);

CREATE INDEX ix_compatibility_rule_set_component_type_a_id
    ON compatibility_rule_set (component_type_a_id);

CREATE INDEX ix_compatibility_rule_set_component_type_b_id
    ON compatibility_rule_set (component_type_b_id);

CREATE TABLE compatibility_rule_condition
(
    id                            BIGSERIAL PRIMARY KEY,
    rule_set_id                   BIGINT      NOT NULL,
    left_attribute_definition_id  BIGINT      NOT NULL,
    operator                      VARCHAR(20) NOT NULL,
    right_attribute_definition_id BIGINT      NOT NULL,
    order_index                   INT,
    created_at                    TIMESTAMP   NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_compatibility_rule_condition_rule_set
        FOREIGN KEY (rule_set_id)
            REFERENCES compatibility_rule_set (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_compatibility_rule_condition_left_attribute
        FOREIGN KEY (left_attribute_definition_id)
            REFERENCES attribute_definition (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_compatibility_rule_condition_right_attribute
        FOREIGN KEY (right_attribute_definition_id)
            REFERENCES attribute_definition (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_compatibility_rule_condition_distinct_attributes
        CHECK (left_attribute_definition_id <> right_attribute_definition_id),

    CONSTRAINT chk_compatibility_rule_condition_operator
        CHECK (operator IN ('EQUALS', 'NOT_EQUALS', 'GT', 'GTE', 'LT', 'LTE'))
);

CREATE UNIQUE INDEX ux_compatibility_rule_condition_definition_operator
    ON compatibility_rule_condition (
        rule_set_id,
        left_attribute_definition_id,
        operator,
        right_attribute_definition_id
    );

CREATE INDEX ix_compatibility_rule_condition_rule_set_id
    ON compatibility_rule_condition (rule_set_id);

CREATE INDEX ix_compatibility_rule_condition_left_attribute_id
    ON compatibility_rule_condition (left_attribute_definition_id);

CREATE INDEX ix_compatibility_rule_condition_right_attribute_id
    ON compatibility_rule_condition (right_attribute_definition_id);
