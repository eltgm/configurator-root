import { componentCatalogViewStorageKey } from '@/shared/config/preferences';
import type { ComponentImage } from '@/shared/api';

export type ComponentCatalogView = 'cards' | 'table';

export function isComponentCatalogView(value: string | null): value is ComponentCatalogView {
  return value === 'cards' || value === 'table';
}

export function readComponentCatalogView(): ComponentCatalogView {
  const storedView = window.localStorage.getItem(componentCatalogViewStorageKey);
  return isComponentCatalogView(storedView) ? storedView : 'cards';
}

export function saveComponentCatalogView(view: ComponentCatalogView) {
  window.localStorage.setItem(componentCatalogViewStorageKey, view);
}

export function toComponentImageUrl(relativeUrl: string): string {
  const normalizedUrl = relativeUrl.startsWith('/') ? relativeUrl : `/${relativeUrl}`;
  return normalizedUrl === '/api' || normalizedUrl.startsWith('/api/')
    ? normalizedUrl
    : `/api${normalizedUrl}`;
}

export function getPrimaryComponentImage(images: ReadonlyArray<ComponentImage> | undefined) {
  return images?.toSorted(
    (left, right) =>
      (left.orderIndex ?? Number.MAX_SAFE_INTEGER) -
        (right.orderIndex ?? Number.MAX_SAFE_INTEGER) || left.id - right.id,
  )[0];
}
