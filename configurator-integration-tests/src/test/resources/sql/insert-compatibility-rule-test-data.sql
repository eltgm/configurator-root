INSERT INTO domain (id, name, description, created_by_user_id, created_at)
VALUES (1, 'Rule Test Domain', 'Domain used to verify automatic compatibility rules', -1, NOW()),
       (2, 'Foreign Rule Domain', 'Domain used to verify rule isolation', -1, NOW());

INSERT INTO component_type (id, domain_id, name, code, description, order_index, created_at)
VALUES (10, 1, 'Processor', 'CPU', 'First side of compatibility rules', 1, NOW()),
       (20, 1, 'Motherboard', 'BOARD', 'Second side of compatibility rules', 2, NOW()),
       (30, 2, 'Foreign Type', 'FOREIGN', 'Type from another domain', 1, NOW());

INSERT INTO attribute_definition (
    id,
    component_type_id,
    name,
    label,
    data_type,
    enum_values_json,
    is_required,
    order_index,
    created_at
)
VALUES (101, 10, 'socket', 'Socket', 'STRING', NULL, true, 1, NOW()),
       (102, 10, 'frequency', 'Frequency', 'NUMBER', NULL, false, 2, NOW()),
       (103, 10, 'family', 'Family', 'ENUM', '["A","B"]', false, 3, NOW()),
       (201, 20, 'socket', 'Socket', 'STRING', NULL, true, 1, NOW()),
       (202, 20, 'max_frequency', 'Maximum frequency', 'NUMBER', NULL, false, 2, NOW()),
       (203, 20, 'supports_feature', 'Supports feature', 'BOOLEAN', NULL, false, 3, NOW()),
       (301, 30, 'socket', 'Socket', 'STRING', NULL, true, 1, NOW());
