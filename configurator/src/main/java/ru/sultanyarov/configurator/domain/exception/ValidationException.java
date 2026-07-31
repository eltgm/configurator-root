package ru.sultanyarov.configurator.domain.exception;

public class ValidationException extends BusinessException {
    public ValidationException(String messagePattern, Object... args) {
        super(messagePattern, args);
    }
}
