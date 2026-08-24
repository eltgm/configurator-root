INSERT INTO component (id, component_type_id, name, brand, description, archived, created_at)
VALUES (2, 1, 'Existing component name', 'Existing brand', 'Existing description', false, NOW());

INSERT INTO attribute_value (component_id, attribute_definition_id, value_string)
VALUES (1, 101, 'ANSI'),
       (1, 102, '75%');

INSERT INTO attribute_value (component_id, attribute_definition_id, value_boolean)
VALUES (1, 103, true);

INSERT INTO component_image (id, component_id, file_path, order_index)
VALUES (501, 1, 'components/1/main.jpg', 1);
