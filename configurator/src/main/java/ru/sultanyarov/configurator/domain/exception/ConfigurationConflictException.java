package ru.sultanyarov.configurator.domain.exception;

public class ConfigurationConflictException extends BusinessException {
  public ConfigurationConflictException(String message, Object... args) {
    super(message, args);
  }
}
