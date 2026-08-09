import { useEffect } from 'react';

export function useDocumentTitle(title: string, applicationName: string) {
  useEffect(() => {
    document.title = `${title} — ${applicationName}`;
  }, [applicationName, title]);
}
