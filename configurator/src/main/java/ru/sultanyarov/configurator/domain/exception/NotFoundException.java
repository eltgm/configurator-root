package ru.sultanyarov.configurator.domain.exception;

public class NotFoundException extends BusinessException{
    public NotFoundException(String messagePattern, Object... args) {
        super(messagePattern, args);
    }
}
