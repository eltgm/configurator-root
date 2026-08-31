package ru.sultanyarov.configurator.domain.exception;

public class AttributeNameConflictException extends EntityAlreadyExistsException {
  public AttributeNameConflictException(Long domainId, String name) {
    super("Attribute definition with name {} already exists in domain {}", name, domainId);
  }
}
