-- Component image binaries are served through the backend. Keep only the storage object key
-- instead of an environment-specific public MinIO URL.
UPDATE component_image
SET file_path = SUBSTRING(file_path FROM POSITION('/components/' IN file_path) + 1)
WHERE POSITION('/components/' IN file_path) > 0;
