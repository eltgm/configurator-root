package ru.sultanyarov.configurator.domain.exception;

public class ImageTooLargeException extends BusinessException {
    public ImageTooLargeException(String messagePattern, Object... args) {
        super(messagePattern, args);
    }
}
