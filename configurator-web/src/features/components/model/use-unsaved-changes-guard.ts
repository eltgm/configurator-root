import { useEffect, useRef } from 'react';
import { useBlocker } from 'react-router-dom';

export function useUnsavedChangesGuard(isDirty: boolean) {
  const allowNavigationRef = useRef(false);
  const blocker = useBlocker(() => isDirty && !allowNavigationRef.current);

  useEffect(() => {
    const preventWindowClose = (event: BeforeUnloadEvent) => {
      if (isDirty && !allowNavigationRef.current) {
        event.preventDefault();
      }
    };
    window.addEventListener('beforeunload', preventWindowClose);
    return () => window.removeEventListener('beforeunload', preventWindowClose);
  }, [isDirty]);

  return {
    blocker,
    allowNavigation: () => {
      allowNavigationRef.current = true;
    },
  };
}
