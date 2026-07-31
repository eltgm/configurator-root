package ru.sultanyarov.configurator.application.port.out;

/** Provides the authenticated user without coupling application services to Spring Security. */
public interface CurrentUserProvider {
  Long getCurrentUserId();
}
