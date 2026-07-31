package ru.sultanyarov.configurator.domain.exception;

public class ComponentArchivedException extends BusinessException {
    public ComponentArchivedException(String messagePattern, Object... args) {
        super(messagePattern, args);
    }
}
