INSERT INTO component (id, component_type_id, name, brand, description, archived, created_at)
VALUES (6, 3, 'Second Foreign Component', 'Other', 'Second component from another domain', false, NOW()),
       (7, 2, 'Isolated Component', NULL, 'Active component without compatibility links', false, NOW());

INSERT INTO compatibility_link (id, domain_id, component_a_id, component_b_id, comment, created_at)
VALUES (704, 1, 1, 3, NULL, NOW()),
       (701, 1, 2, 3, 'Same domain', NOW()),
       (702, 1, 1, 5, 'Archived endpoint', NOW()),
       (703, 2, 4, 6, 'Foreign domain', NOW());
