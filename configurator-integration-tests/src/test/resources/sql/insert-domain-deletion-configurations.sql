INSERT INTO app_user (id, email, password_hash, display_name)
VALUES (99, 'deletion-test@configurator.local', 'hash', 'Other User');

INSERT INTO configuration (id, domain_id, name, created_by_user_id)
VALUES (900, 1, 'Active build', -1),
       (901, 1, 'Build with archived component', -1),
       (902, 1, 'Empty foreign build', 99);

INSERT INTO configuration_component (configuration_id, component_id)
VALUES (900, 1), (901, 4);
