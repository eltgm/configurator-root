package ru.sultanyarov.configurator.infrastructure.security;

import org.springframework.stereotype.Component;
import ru.sultanyarov.configurator.application.port.out.CurrentUserProvider;

/**
 * Temporary adapter used until JWT authentication is implemented. Replacing this bean with a Spring
 * Security-backed adapter does not require changes to configuration use cases.
 */
@Component
public class TemporaryCurrentUserProvider implements CurrentUserProvider {
  public static final long SYSTEM_USER_ID = -1L;

  @Override
  public Long getCurrentUserId() {
    return SYSTEM_USER_ID;
  }
}
