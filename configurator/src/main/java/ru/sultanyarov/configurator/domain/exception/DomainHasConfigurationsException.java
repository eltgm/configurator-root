package ru.sultanyarov.configurator.domain.exception;

public class DomainHasConfigurationsException extends BusinessException {
  public DomainHasConfigurationsException(Long domainId) {
    super(
        "Cannot delete domain with id {} because it has configurations. Delete all configurations first",
        domainId);
  }
}
