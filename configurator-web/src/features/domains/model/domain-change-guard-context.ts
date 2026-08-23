import { createContext } from 'react';

export interface GuardRegistration {
  dirty: boolean;
  discard: () => void;
}

export interface DomainChangeGuardContextValue {
  hasUnsavedChanges: boolean;
  discardUnsavedChanges: () => void;
  register: (key: symbol, registration: GuardRegistration | null) => void;
}

export const DomainChangeGuardContext = createContext<DomainChangeGuardContextValue | null>(null);
