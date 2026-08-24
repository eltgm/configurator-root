import { useContext, useEffect, useRef } from 'react';

import { DomainChangeGuardContext } from '@/features/domains/model/domain-change-guard-context';

export function useDomainChangeGuard() {
  const context = useContext(DomainChangeGuardContext);
  if (!context) {
    throw new Error('useDomainChangeGuard must be used inside DomainChangeGuardProvider');
  }
  return context;
}

export function useRegisterDomainChangeGuard(isDirty: boolean, discard: () => void) {
  const { register } = useDomainChangeGuard();
  const keyRef = useRef(Symbol('domain-change-guard'));

  useEffect(() => {
    const key = keyRef.current;
    register(key, { dirty: isDirty, discard });
    return () => register(key, null);
  }, [discard, isDirty, register]);
}
