INSERT INTO domain (id, name, description, created_by_user_id, created_at)
VALUES (1, 'Configurator Domain', 'Automatic compatibility test domain', -1, NOW()),
       (2, 'Foreign Domain', 'Foreign configurator domain', -1, NOW());

INSERT INTO component_type (id, domain_id, name, code, description, order_index, created_at)
VALUES (10, 1, 'Processor', 'CPU', 'Processor type', 1, NOW()),
       (20, 1, 'Motherboard', 'BOARD', 'Motherboard type', 2, NOW()),
       (30, 1, 'Cooler', 'COOLER', 'Cooler type', 3, NOW()),
       (40, 2, 'Foreign Type', 'FOREIGN', 'Foreign type', 1, NOW());

INSERT INTO attribute_definition (
    id,
    domain_id,
    name,
    label,
    data_type,
    created_at
)
VALUES (101, 1, 'socket', 'Socket', 'STRING', NOW()),
       (102, 1, 'power', 'Power', 'NUMBER', NOW()),
       (103, 1, 'feature', 'Feature', 'BOOLEAN', NOW()),
       (202, 1, 'power_limit', 'Power limit', 'NUMBER', NOW());

INSERT INTO component_type_attribute (component_type_id, attribute_definition_id, is_required, order_index, created_at)
VALUES (10, 101, true, 1, NOW()),
       (10, 102, true, 2, NOW()),
       (10, 103, false, 3, NOW()),
       (20, 101, true, 1, NOW()),
       (20, 202, true, 2, NOW()),
       (20, 103, false, 3, NOW());

INSERT INTO component (
    id,
    component_type_id,
    name,
    brand,
    description,
    archived,
    created_at
)
VALUES (1, 10, 'Base CPU', 'CPU Brand', NULL, false, NOW()),
       (2, 20, 'Automatic and manual board', 'Board Brand', NULL, false, NOW()),
       (3, 20, 'Manual board', NULL, NULL, false, NOW()),
       (4, 20, 'Archived matching board', 'Legacy', NULL, true, NOW()),
       (5, 30, 'Manual cooler', 'Cooler Brand', NULL, false, NOW()),
       (6, 20, 'Disabled-rule-only board', 'Board Brand', NULL, false, NOW()),
       (7, 40, 'Foreign component', NULL, NULL, false, NOW()),
       (8, 30, 'Isolated cooler', NULL, NULL, false, NOW()),
       (9, 30, 'Transitive cooler', 'Path Brand', NULL, false, NOW());

INSERT INTO attribute_value (
    id,
    component_id,
    attribute_definition_id,
    value_string,
    value_number,
    value_boolean
)
VALUES (1001, 1, 101, 'AM5', NULL, NULL),
       (1002, 1, 102, NULL, 100, NULL),
       (1003, 1, 103, NULL, NULL, true),
       (2001, 2, 101, 'AM5', NULL, NULL),
       (2002, 2, 202, NULL, 200, NULL),
       (2003, 2, 103, NULL, NULL, true),
       (3001, 3, 101, 'AM4', NULL, NULL),
       (3002, 3, 202, NULL, 200, NULL),
       (3003, 3, 103, NULL, NULL, false),
       (4001, 4, 101, 'AM5', NULL, NULL),
       (4002, 4, 202, NULL, 200, NULL),
       (4003, 4, 103, NULL, NULL, true),
       (6001, 6, 101, 'AM4', NULL, NULL),
       (6002, 6, 202, NULL, 200, NULL),
       (6003, 6, 103, NULL, NULL, true);

INSERT INTO compatibility_rule_set (
    id,
    domain_id,
    name,
    component_type_a_id,
    component_type_b_id,
    enabled,
    created_at
)
VALUES (701, 1, 'Socket and power', 10, 20, true, NOW()),
       (702, 1, 'Disabled feature fallback', 10, 20, false, NOW());

INSERT INTO compatibility_rule_condition (
    id,
    rule_set_id,
    left_attribute_definition_id,
    operator,
    right_attribute_definition_id,
    order_index,
    created_at
)
VALUES (7101, 701, 101, 'EQUALS', 101, 0, NOW()),
       (7102, 701, 102, 'LTE', 202, 1, NOW()),
       (7201, 702, 103, 'EQUALS', 103, 0, NOW());

INSERT INTO compatibility_link (
    id,
    domain_id,
    component_a_id,
    component_b_id,
    comment,
    created_at
)
VALUES (801, 1, 1, 2, 'Duplicate automatic source', NOW()),
       (802, 1, 1, 3, 'Manual link blocked by automatic mismatch', NOW()),
       (803, 1, 1, 5, 'Manual cross-type compatibility', NOW()),
       (804, 1, 2, 9, 'Second transitive hop', NOW());
