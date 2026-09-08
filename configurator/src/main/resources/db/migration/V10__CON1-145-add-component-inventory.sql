ALTER TABLE component
    ADD COLUMN total_quantity INTEGER NOT NULL DEFAULT 0
        CONSTRAINT chk_component_total_quantity_non_negative CHECK (total_quantity >= 0);

ALTER TABLE configuration
    ADD COLUMN track_inventory BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE configuration_component
    ADD COLUMN quantity INTEGER NOT NULL DEFAULT 1
        CONSTRAINT chk_configuration_component_quantity_positive CHECK (quantity > 0);

CREATE INDEX ix_configuration_component_component_quantity
    ON configuration_component (component_id, quantity);

CREATE INDEX ix_configuration_track_inventory
    ON configuration (track_inventory);
