package ru.sultanyarov.configurator.domain.exception;

public class EntityAlreadyExistsException extends BusinessException {
    public EntityAlreadyExistsException(String messagePattern, Object... args) {
        super(messagePattern, args);
    }
}
