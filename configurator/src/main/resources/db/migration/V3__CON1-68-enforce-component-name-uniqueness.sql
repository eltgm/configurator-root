DROP INDEX ix_component_type_name;

CREATE UNIQUE INDEX ux_component_type_name
    ON component (component_type_id, name);
