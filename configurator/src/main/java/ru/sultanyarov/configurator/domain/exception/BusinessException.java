package ru.sultanyarov.configurator.domain.exception;

import org.slf4j.helpers.MessageFormatter;

public class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(String messagePattern, Object... args) {
        super(getMessage(messagePattern, args));
    }

    public BusinessException(Throwable cause, String messagePattern, Object... args) {
        super(getMessage(messagePattern, args), cause);
    }

    private static String getMessage(String messagePattern, Object[] args) {
        return MessageFormatter.arrayFormat(messagePattern, args).getMessage();
    }
}
