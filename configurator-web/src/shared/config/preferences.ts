export const colorSchemeStorageKey = 'configurator.color-scheme';
export const localeStorageKey = 'configurator.locale';
export const selectedDomainStorageKey = 'configurator.selected-domain-id';
export const componentCatalogViewStorageKey = 'configurator.component-catalog-view';
export const configuratorDraftStorageKeyPrefix = 'configurator.assembly-draft.v1';

export function configuratorDraftStorageKey(domainId: number) {
  return `${configuratorDraftStorageKeyPrefix}.${domainId}`;
}

export const supportedLocales = ['ru', 'en'] as const;
export type SupportedLocale = (typeof supportedLocales)[number];

export function isSupportedLocale(value: string | null): value is SupportedLocale {
  return supportedLocales.some((locale) => locale === value);
}
