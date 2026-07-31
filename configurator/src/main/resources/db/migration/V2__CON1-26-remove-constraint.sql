-- временное решение для mvp, пока нет системы авторизации
ALTER TABLE domain
DROP
CONSTRAINT fk_domain_created_by;