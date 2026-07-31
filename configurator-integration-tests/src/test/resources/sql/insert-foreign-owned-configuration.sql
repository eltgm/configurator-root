INSERT INTO app_user (id, email, password_hash, display_name)
VALUES (99, 'other-user@configurator.local', 'hash', 'Other User');

INSERT INTO configuration (id, domain_id, name, description, created_by_user_id, created_at)
VALUES (900, 1, 'Foreign configuration', NULL, 99, NOW());

INSERT INTO configuration_component (configuration_id, component_id)
VALUES (900, 1);
