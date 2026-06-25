INSERT INTO domain (id, name, description, created_by_user_id, created_at)
VALUES (2, 'Second Domain', 'Domain used to verify component isolation', -1, NOW());

INSERT INTO component_type (id, domain_id, name, code, description, order_index, created_at)
VALUES (3, 2, 'Foreign Component Type', 'FOREIGN_CODE', 'Type from another domain', 1, NOW());

INSERT INTO component (id, component_type_id, name, brand, description, archived, created_at)
VALUES (2, 1, 'Keychron K2', 'Keychron', 'Second component of the first type', false, NOW()),
       (3, 2, 'Gateron Yellow', 'Gateron', 'Component of the second type', false, NOW()),
       (4, 3, 'Foreign Component', 'Other', 'Component from another domain', false, NOW());
