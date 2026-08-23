import { useEffect } from 'react';

export const documentTitleChangedEvent = 'configurator:document-title-changed';

export type DocumentTitleChangedEvent = CustomEvent<{ title: string }>;

export function useDocumentTitle(title: string, applicationName: string) {
  useEffect(() => {
    document.title = `${title} — ${applicationName}`;
    window.dispatchEvent(
      new CustomEvent(documentTitleChangedEvent, {
        detail: { title },
      }),
    );
  }, [applicationName, title]);
}
