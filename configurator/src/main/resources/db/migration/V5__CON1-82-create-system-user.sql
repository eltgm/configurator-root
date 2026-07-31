INSERT INTO app_user (id, email, password_hash, display_name)
SELECT -1, 'system@configurator.local', 'AUTHENTICATION_NOT_CONFIGURED', 'System User'
WHERE NOT EXISTS (
    SELECT 1
    FROM app_user
    WHERE id = -1
);
