import { type PropsWithChildren, useEffect, useMemo, useState } from 'react';

import { useDomainsQuery } from '@/features/domains/api/domains';
import { DomainContext, type DomainContextValue } from '@/features/domains/model/domain-context';
import type { Domain } from '@/shared/api';
import { selectedDomainStorageKey } from '@/shared/config/preferences';

const emptyDomains: Array<Domain> = [];

function readStoredDomainId(): number | null {
  const storedValue = window.localStorage.getItem(selectedDomainStorageKey);
  if (!storedValue) {
    return null;
  }

  const domainId = Number(storedValue);
  return Number.isSafeInteger(domainId) && domainId > 0 ? domainId : null;
}

export function DomainProvider({ children }: PropsWithChildren) {
  const { data, error, isPending, isSuccess, refetch } = useDomainsQuery();
  const [preferredDomainId, setPreferredDomainId] = useState(readStoredDomainId);
  const domains = data ?? emptyDomains;
  const selectedDomain =
    domains.find((domain) => domain.id === preferredDomainId) ?? domains[0] ?? null;
  const selectedDomainId = selectedDomain?.id ?? null;

  useEffect(() => {
    if (!isSuccess) {
      return;
    }
    if (selectedDomainId === null) {
      window.localStorage.removeItem(selectedDomainStorageKey);
    } else {
      window.localStorage.setItem(selectedDomainStorageKey, String(selectedDomainId));
    }
  }, [isSuccess, selectedDomainId]);

  const contextValue = useMemo<DomainContextValue>(
    () => ({
      domains,
      selectedDomain,
      selectedDomainId,
      selectDomain: (domainId) => {
        if (
          !Number.isSafeInteger(domainId) ||
          domainId <= 0 ||
          !domains.some((domain) => domain.id === domainId)
        ) {
          return;
        }
        setPreferredDomainId(domainId);
        window.localStorage.setItem(selectedDomainStorageKey, String(domainId));
      },
      isLoading: isPending,
      error,
      refetch: () => {
        void refetch();
      },
    }),
    [domains, error, isPending, refetch, selectedDomain, selectedDomainId],
  );

  return <DomainContext.Provider value={contextValue}>{children}</DomainContext.Provider>;
}
