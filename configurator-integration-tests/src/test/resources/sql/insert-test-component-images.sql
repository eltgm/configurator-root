INSERT INTO component (id, component_type_id, name, archived, created_at)
VALUES (2, 1, 'Foreign image component', false, NOW());

INSERT INTO component_image (id, component_id, file_path, order_index)
VALUES (601, 1, 'components/1/unordered.png', NULL),
       (604, 1, 'components/1/second-two.png', 2),
       (602, 1, 'components/1/zero.png', 0),
       (603, 1, 'components/1/first-two.png', 2),
       (605, 2, 'components/2/foreign.png', 0);
