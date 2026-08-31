-- PostgreSQL data migration. jOOQ's schema-only interpreter skips this block;
-- the unique constraint below is interpreted by both jOOQ and PostgreSQL.
-- [jooq ignore start]
DO $$
DECLARE
    conflict RECORD;
BEGIN
    LOCK TABLE attribute_definition, component_type_attribute, attribute_value,
        compatibility_rule_condition IN ACCESS EXCLUSIVE MODE;

    CREATE TEMPORARY TABLE attribute_definition_merge ON COMMIT DROP AS
    SELECT id, domain_id, name, MIN(id) OVER (PARTITION BY domain_id, name) AS canonical_id
    FROM attribute_definition;

    SELECT m.domain_id, m.name, m.id, m.canonical_id INTO conflict
    FROM attribute_definition_merge m
    JOIN attribute_definition source ON source.id = m.id
    JOIN attribute_definition target ON target.id = m.canonical_id
    WHERE m.id <> m.canonical_id
      AND (source.label IS DISTINCT FROM target.label
        OR source.data_type IS DISTINCT FROM target.data_type
        OR ARRAY(SELECT DISTINCT value
                 FROM jsonb_array_elements_text(COALESCE(source.enum_values_json::jsonb, '[]'::jsonb))
                 ORDER BY value)
           IS DISTINCT FROM
           ARRAY(SELECT DISTINCT value
                 FROM jsonb_array_elements_text(COALESCE(target.enum_values_json::jsonb, '[]'::jsonb))
                 ORDER BY value))
    ORDER BY m.id LIMIT 1;
    IF FOUND THEN
        RAISE EXCEPTION 'Incompatible attribute definitions: domain %, name %, IDs % and %',
            conflict.domain_id, conflict.name, conflict.id, conflict.canonical_id;
    END IF;

    SELECT m.domain_id, m.name, m.canonical_id, cta.component_type_id INTO conflict
    FROM component_type_attribute cta
    JOIN attribute_definition_merge m ON m.id = cta.attribute_definition_id
    GROUP BY m.domain_id, m.name, m.canonical_id, cta.component_type_id
    HAVING COUNT(DISTINCT (cta.is_required, cta.order_index)) > 1
    LIMIT 1;
    IF FOUND THEN
        RAISE EXCEPTION 'Conflicting attribute link settings: domain %, name %, canonical ID %, type %',
            conflict.domain_id, conflict.name, conflict.canonical_id, conflict.component_type_id;
    END IF;

    SELECT m.domain_id, m.name, m.canonical_id, av.component_id INTO conflict
    FROM attribute_value av
    JOIN attribute_definition_merge m ON m.id = av.attribute_definition_id
    GROUP BY m.domain_id, m.name, m.canonical_id, av.component_id
    HAVING COUNT(*) > 1
    LIMIT 1;
    IF FOUND THEN
        RAISE EXCEPTION 'Conflicting attribute values: domain %, name %, canonical ID %, component %',
            conflict.domain_id, conflict.name, conflict.canonical_id, conflict.component_id;
    END IF;

    SELECT l.domain_id, l.name, l.canonical_id, crc.rule_set_id INTO conflict
    FROM compatibility_rule_condition crc
    JOIN attribute_definition_merge l ON l.id = crc.left_attribute_definition_id
    JOIN attribute_definition_merge r ON r.id = crc.right_attribute_definition_id
    GROUP BY l.domain_id, l.name, l.canonical_id, crc.rule_set_id, crc.operator, r.canonical_id
    HAVING COUNT(*) > 1
    LIMIT 1;
    IF FOUND THEN
        RAISE EXCEPTION 'Conflicting attribute rule conditions: domain %, name %, canonical ID %, rule %',
            conflict.domain_id, conflict.name, conflict.canonical_id, conflict.rule_set_id;
    END IF;

    INSERT INTO component_type_attribute
        (component_type_id, attribute_definition_id, is_required, order_index, created_at)
    SELECT cta.component_type_id, m.canonical_id, cta.is_required, cta.order_index, MIN(cta.created_at)
    FROM component_type_attribute cta
    JOIN attribute_definition_merge m ON m.id = cta.attribute_definition_id
    WHERE m.id <> m.canonical_id
    GROUP BY cta.component_type_id, m.canonical_id, cta.is_required, cta.order_index
    ON CONFLICT (component_type_id, attribute_definition_id) DO NOTHING;

    UPDATE attribute_value av SET attribute_definition_id = m.canonical_id
    FROM attribute_definition_merge m
    WHERE av.attribute_definition_id = m.id AND m.id <> m.canonical_id;

    UPDATE compatibility_rule_condition crc
    SET left_attribute_definition_id = l.canonical_id,
        right_attribute_definition_id = r.canonical_id
    FROM attribute_definition_merge l, attribute_definition_merge r
    WHERE crc.left_attribute_definition_id = l.id AND crc.right_attribute_definition_id = r.id
      AND (l.id <> l.canonical_id OR r.id <> r.canonical_id);

    FOR conflict IN SELECT * FROM attribute_definition_merge WHERE id <> canonical_id ORDER BY id LOOP
        RAISE NOTICE 'Merged attribute: domain %, name %, ID % -> %',
            conflict.domain_id, conflict.name, conflict.id, conflict.canonical_id;
    END LOOP;

    -- All values and rule references have been transferred. Cascading deletes only remove old links.
    DELETE FROM attribute_definition ad USING attribute_definition_merge m
    WHERE ad.id = m.id AND m.id <> m.canonical_id;
END $$;
-- [jooq ignore stop]

DROP INDEX ix_attribute_definition_domain_name;

ALTER TABLE attribute_definition
    ADD CONSTRAINT ux_attribute_definition_domain_name UNIQUE (domain_id, name);
