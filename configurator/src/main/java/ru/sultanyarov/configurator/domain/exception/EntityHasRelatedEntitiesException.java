package ru.sultanyarov.configurator.domain.exception;

public class EntityHasRelatedEntitiesException extends BusinessException {
    public EntityHasRelatedEntitiesException(String messagePattern, Object... args) {
        super(messagePattern, args);
    }
}
