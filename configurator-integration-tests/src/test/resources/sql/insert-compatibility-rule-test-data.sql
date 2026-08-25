INSERT INTO domain (id, name, description, created_by_user_id, created_at)
VALUES (1, 'Rule Test Domain', 'Domain used to verify automatic compatibility rules', -1, NOW()),
       (2, 'Foreign Rule Domain', 'Domain used to verify rule isolation', -1, NOW());

INSERT INTO component_type (id, domain_id, name, code, description, order_index, created_at)
VALUES (10, 1, 'Processor', 'CPU', 'First side of compatibility rules', 1, NOW()),
       (20, 1, 'Motherboard', 'BOARD', 'Second side of compatibility rules', 2, NOW()),
       (30, 2, 'Foreign Type', 'FOREIGN', 'Type from another domain', 1, NOW());

INSERT INTO attribute_definition (
    id,
    domain_id,
    name,
    label,
    data_type,
    enum_values_json,
    created_at
)
VALUES (101, 1, 'socket', 'Socket', 'STRING', NULL, NOW()),
       (102, 1, 'frequency', 'Frequency', 'NUMBER', NULL, NOW()),
       (103, 1, 'family', 'Family', 'ENUM', '["A","B"]', NOW()),
       (201, 1, 'socket', 'Socket', 'STRING', NULL, NOW()),
       (202, 1, 'max_frequency', 'Maximum frequency', 'NUMBER', NULL, NOW()),
       (203, 1, 'supports_feature', 'Supports feature', 'BOOLEAN', NULL, NOW()),
       (301, 2, 'socket', 'Socket', 'STRING', NULL, NOW());

INSERT INTO component_type_attribute (component_type_id, attribute_definition_id, is_required, order_index, created_at)
VALUES (10, 101, true, 1, NOW()),
       (10, 102, false, 2, NOW()),
       (10, 103, false, 3, NOW()),
       (20, 201, true, 1, NOW()),
       (20, 202, false, 2, NOW()),
       (20, 203, false, 3, NOW()),
       (30, 301, true, 1, NOW());
