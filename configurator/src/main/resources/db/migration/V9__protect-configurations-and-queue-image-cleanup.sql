ALTER TABLE configuration DROP CONSTRAINT fk_configuration_domain;
ALTER TABLE configuration
    ADD CONSTRAINT fk_configuration_domain
        FOREIGN KEY (domain_id) REFERENCES domain (id) ON DELETE RESTRICT;

-- Independent of the deleted domain/components: jobs must survive deletion and restarts.
CREATE TABLE component_image_cleanup (
    object_key VARCHAR(1024) PRIMARY KEY,
    next_attempt_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0)
);

CREATE INDEX ix_component_image_cleanup_next_attempt
    ON component_image_cleanup (next_attempt_at, object_key);
