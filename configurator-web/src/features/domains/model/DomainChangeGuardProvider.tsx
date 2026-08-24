import { type PropsWithChildren, useCallback, useMemo, useState } from 'react';

import {
  DomainChangeGuardContext,
  type DomainChangeGuardContextValue,
  type GuardRegistration,
} from '@/features/domains/model/domain-change-guard-context';

export function DomainChangeGuardProvider({ children }: PropsWithChildren) {
  const [registrations, setRegistrations] = useState(() => new Map<symbol, GuardRegistration>());
  const register = useCallback((key: symbol, registration: GuardRegistration | null) => {
    setRegistrations((current) => {
      const next = new Map(current);
      if (registration) next.set(key, registration);
      else next.delete(key);
      return next;
    });
  }, []);
  const contextValue = useMemo<DomainChangeGuardContextValue>(
    () => ({
      hasUnsavedChanges: [...registrations.values()].some((registration) => registration.dirty),
      discardUnsavedChanges: () => {
        for (const registration of registrations.values()) {
          if (registration.dirty) registration.discard();
        }
      },
      register,
    }),
    [register, registrations],
  );

  return (
    <DomainChangeGuardContext.Provider value={contextValue}>
      {children}
    </DomainChangeGuardContext.Provider>
  );
}
