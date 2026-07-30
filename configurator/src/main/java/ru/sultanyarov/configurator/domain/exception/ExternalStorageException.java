package ru.sultanyarov.configurator.domain.exception;

public class ExternalStorageException extends BusinessException {
    public ExternalStorageException(Throwable cause, String messagePattern, Object... args) {
        super(cause, messagePattern, args);
    }
}
