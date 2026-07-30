package ru.sultanyarov.configurator.domain.exception;

public class UnsupportedImageFormatException extends BusinessException {
    public UnsupportedImageFormatException(String messagePattern, Object... args) {
        super(messagePattern, args);
    }
}
