-- =========================================================
--  DDL for Configurator MVP (PostgreSQL)
-- =========================================================

-- ---------- EXTENSIONS (если нужно) ----------
-- CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ---------- TABLE: app_user ----------
CREATE TABLE app_user
(
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Уникальный email
CREATE UNIQUE INDEX ux_app_user_email ON app_user (email);


-- ---------- TABLE: domain ----------
CREATE TABLE domain
(
    id                 BIGSERIAL PRIMARY KEY,
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    created_by_user_id BIGINT       NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_domain_created_by
        FOREIGN KEY (created_by_user_id)
            REFERENCES app_user (id)
            ON DELETE RESTRICT
);

-- (Опционально) гарантируем уникальность названия домена
CREATE UNIQUE INDEX ux_domain_name ON domain (name);

-- Индекс по created_by_user_id
CREATE INDEX ix_domain_created_by_user_id ON domain (created_by_user_id);


-- ---------- TABLE: component_type ----------
CREATE TABLE component_type
(
    id          BIGSERIAL PRIMARY KEY,
    domain_id   BIGINT       NOT NULL,
    name        VARCHAR(255) NOT NULL,
    code        VARCHAR(100),
    description TEXT,
    order_index INT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_component_type_domain
        FOREIGN KEY (domain_id)
            REFERENCES domain (id)
            ON DELETE CASCADE
);

-- Уникальность имени типа в рамках домена
CREATE UNIQUE INDEX ux_component_type_domain_name
    ON component_type (domain_id, name);

-- Частые запросы: все типы домена
CREATE INDEX ix_component_type_domain_id
    ON component_type (domain_id);


-- ---------- TABLE: attribute_definition ----------
CREATE TABLE attribute_definition
(
    id                BIGSERIAL PRIMARY KEY,
    component_type_id BIGINT       NOT NULL,
    name              VARCHAR(255) NOT NULL, -- системное имя
    label             VARCHAR(255) NOT NULL, -- UI-лейбл
    data_type         VARCHAR(20)  NOT NULL, -- STRING | NUMBER | BOOLEAN | ENUM
    enum_values_json  TEXT,
    is_required       BOOLEAN      NOT NULL DEFAULT FALSE,
    order_index       INT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_attribute_definition_component_type
        FOREIGN KEY (component_type_id)
            REFERENCES component_type (id)
            ON DELETE CASCADE,

    -- data_type ограничиваем допустимыми значениями
    CONSTRAINT chk_attribute_definition_data_type
        CHECK (data_type IN ('STRING', 'NUMBER', 'BOOLEAN', 'ENUM'))
);

-- Уникальность имени атрибута внутри типа компонента
CREATE UNIQUE INDEX ux_attribute_definition_component_type_name
    ON attribute_definition (component_type_id, name);

-- Индекс по component_type_id
CREATE INDEX ix_attribute_definition_component_type_id
    ON attribute_definition (component_type_id);


-- ---------- TABLE: component ----------
CREATE TABLE component
(
    id                BIGSERIAL PRIMARY KEY,
    component_type_id BIGINT       NOT NULL,
    name              VARCHAR(255) NOT NULL,
    brand             VARCHAR(255),
    description       TEXT,
    archived          BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_component_component_type
        FOREIGN KEY (component_type_id)
            REFERENCES component_type (id)
            ON DELETE RESTRICT
);

-- Частый запрос: компоненты по типу
CREATE INDEX ix_component_component_type_id
    ON component (component_type_id);

-- Поиск по имени/бренду в рамках типа — комбинированный индекс
CREATE INDEX ix_component_type_name
    ON component (component_type_id, name);

CREATE INDEX ix_component_type_brand
    ON component (component_type_id, brand);


-- ---------- TABLE: attribute_value ----------
CREATE TABLE attribute_value
(
    id                      BIGSERIAL PRIMARY KEY,
    component_id            BIGINT NOT NULL,
    attribute_definition_id BIGINT NOT NULL,
    value_string            VARCHAR(1000),
    value_number            NUMERIC,
    value_boolean           BOOLEAN,

    CONSTRAINT fk_attribute_value_component
        FOREIGN KEY (component_id)
            REFERENCES component (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_attribute_value_attribute_definition
        FOREIGN KEY (attribute_definition_id)
            REFERENCES attribute_definition (id)
            ON DELETE CASCADE
);

-- Один компонент -> одно значение по каждому attribute_definition
CREATE UNIQUE INDEX ux_attribute_value_component_attribute
    ON attribute_value (component_id, attribute_definition_id);

-- Индексы по FK
CREATE INDEX ix_attribute_value_component_id
    ON attribute_value (component_id);

CREATE INDEX ix_attribute_value_attribute_definition_id
    ON attribute_value (attribute_definition_id);


-- ---------- TABLE: component_image ----------
CREATE TABLE component_image
(
    id           BIGSERIAL PRIMARY KEY,
    component_id BIGINT        NOT NULL,
    file_path    VARCHAR(1024) NOT NULL,
    order_index  INT,

    CONSTRAINT fk_component_image_component
        FOREIGN KEY (component_id)
            REFERENCES component (id)
            ON DELETE CASCADE
);

-- Индекс по component_id
CREATE INDEX ix_component_image_component_id
    ON component_image (component_id);


-- ---------- TABLE: compatibility_link ----------
CREATE TABLE compatibility_link
(
    id             BIGSERIAL PRIMARY KEY,
    domain_id      BIGINT      NOT NULL,
    component_a_id BIGINT      NOT NULL,
    component_b_id BIGINT      NOT NULL,
    relation_type  VARCHAR(50) NOT NULL DEFAULT 'COMPATIBLE',
    comment        TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_compatibility_link_domain
        FOREIGN KEY (domain_id)
            REFERENCES domain (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_compatibility_link_component_a
        FOREIGN KEY (component_a_id)
            REFERENCES component (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_compatibility_link_component_b
        FOREIGN KEY (component_b_id)
            REFERENCES component (id)
            ON DELETE CASCADE,

    -- Разрешаем только COMPATIBLE в MVP
    CONSTRAINT chk_compatibility_relation_type
        CHECK (relation_type IN ('COMPATIBLE')),

    -- Гарантируем, что не храним A-B и B-A одновременно:
    -- компонент A < компонент B
    CONSTRAINT chk_compatibility_component_order
        CHECK (component_a_id < component_b_id)
);

-- Уникальность пары компонентов внутри домена
CREATE UNIQUE INDEX ux_compatibility_link_domain_components
    ON compatibility_link (domain_id, component_a_id, component_b_id);

-- Индексы для быстрых запросов по домену и компонентам
CREATE INDEX ix_compatibility_link_domain_id
    ON compatibility_link (domain_id);

CREATE INDEX ix_compatibility_link_component_a_id
    ON compatibility_link (component_a_id);

CREATE INDEX ix_compatibility_link_component_b_id
    ON compatibility_link (component_b_id);


-- ---------- TABLE: configuration ----------
CREATE TABLE configuration
(
    id                 BIGSERIAL PRIMARY KEY,
    domain_id          BIGINT       NOT NULL,
    name               VARCHAR(255) NOT NULL,
    description        TEXT,
    created_by_user_id BIGINT       NOT NULL,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_configuration_domain
        FOREIGN KEY (domain_id)
            REFERENCES domain (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_configuration_created_by
        FOREIGN KEY (created_by_user_id)
            REFERENCES app_user (id)
            ON DELETE RESTRICT
);

-- Частый запрос: конфигурации по домену / пользователю
CREATE INDEX ix_configuration_domain_id
    ON configuration (domain_id);

CREATE INDEX ix_configuration_created_by_user_id
    ON configuration (created_by_user_id);


-- ---------- TABLE: configuration_component ----------
CREATE TABLE configuration_component
(
    id               BIGSERIAL PRIMARY KEY,
    configuration_id BIGINT NOT NULL,
    component_id     BIGINT NOT NULL,
    note             TEXT,

    CONSTRAINT fk_configuration_component_configuration
        FOREIGN KEY (configuration_id)
            REFERENCES configuration (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_configuration_component_component
        FOREIGN KEY (component_id)
            REFERENCES component (id)
            ON DELETE RESTRICT
);

-- Один и тот же компонент не должен дублироваться в одной конфигурации
CREATE UNIQUE INDEX ux_configuration_component_unique
    ON configuration_component (configuration_id, component_id);

-- Индексы по FK
CREATE INDEX ix_configuration_component_configuration_id
    ON configuration_component (configuration_id);

CREATE INDEX ix_configuration_component_component_id
    ON configuration_component (component_id);

-- =========================================================
-- END OF DDL
-- =========================================================
