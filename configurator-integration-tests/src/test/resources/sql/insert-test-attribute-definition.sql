INSERT INTO attribute_definition (id, domain_id, name, label, data_type, created_at)
VALUES (1, 1, 'original_attribute', 'Original Attribute', 'STRING', NOW()),
       (2, 1, 'attribute_one', 'Attribute One', 'STRING', NOW()),
       (3, 1, 'attribute_two', 'Attribute Two', 'STRING', NOW());

INSERT INTO component_type_attribute (component_type_id, attribute_definition_id, is_required, order_index, created_at)
VALUES (1, 1, true, 1, NOW()),
       (1, 2, true, 1, NOW()),
       (1, 3, true, 2, NOW());
