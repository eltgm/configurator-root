INSERT INTO attribute_definition (id, domain_id, name, label, data_type, enum_values_json, created_at)
VALUES (101, 1, 'layout', 'Layout', 'ENUM', '["ANSI","ISO"]', NOW()),
       (102, 1, 'form_factor', 'Form factor', 'ENUM', '["75%","TKL"]', NOW()),
       (103, 1, 'wireless', 'Wireless', 'BOOLEAN', NULL, NOW());

INSERT INTO component_type_attribute (component_type_id, attribute_definition_id, is_required, order_index, created_at)
VALUES (1, 101, true, 1, NOW()),
       (1, 102, true, 2, NOW()),
       (1, 103, false, 3, NOW());
