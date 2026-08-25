DELETE FROM configuration_component;
DELETE FROM configuration;
DELETE FROM compatibility_rule_condition;
DELETE FROM compatibility_rule_set;
DELETE FROM compatibility_link;
DELETE FROM component_image;
DELETE FROM attribute_value;
DELETE FROM component;
DELETE FROM component_type_attribute;
DELETE FROM component_type;
DELETE FROM attribute_definition;
DELETE FROM domain;
DELETE FROM app_user;

INSERT INTO app_user (id, email, password_hash, display_name)
VALUES (-1, 'system@configurator.local', 'AUTHENTICATION_NOT_CONFIGURED', 'System User');
