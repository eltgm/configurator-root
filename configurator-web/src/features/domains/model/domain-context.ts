import { createContext, useContext } from 'react';

import type { Domain } from '@/shared/api';

export interface DomainContextValue {
  domains: Array<Domain>;
  selectedDomain: Domain | null;
  selectedDomainId: number | null;
  selectDomain: (domainId: number) => void;
  isLoading: boolean;
  error: unknown;
  refetch: () => void;
}

export const DomainContext = createContext<DomainContextValue | null>(null);

export function useDomainContext() {
  const context = useContext(DomainContext);
  if (!context) {
    throw new Error('useDomainContext must be used inside DomainProvider');
  }
  return context;
}
