INSERT INTO attribute_definition (id, domain_id, name, label, data_type, enum_values_json, created_at)
VALUES (201, 1, 'switch_type', 'Switch type', 'ENUM', '["Linear","Tactile"]', NOW());

INSERT INTO component_type_attribute (component_type_id, attribute_definition_id, is_required, order_index, created_at)
VALUES (2, 201, false, 1, NOW());
