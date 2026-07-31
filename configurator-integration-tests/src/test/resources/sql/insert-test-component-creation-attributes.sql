INSERT INTO attribute_definition (id, component_type_id, name, label, data_type, enum_values_json, is_required, order_index, created_at)
VALUES (101, 1, 'layout', 'Layout', 'ENUM', '["ANSI","ISO"]', true, 1, NOW()),
       (102, 1, 'form_factor', 'Form factor', 'ENUM', '["75%","TKL"]', true, 2, NOW()),
       (103, 1, 'wireless', 'Wireless', 'BOOLEAN', NULL, false, 3, NOW());
