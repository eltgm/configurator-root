export const colorSchemeStorageKey = 'configurator.color-scheme';
export const localeStorageKey = 'configurator.locale';
export const selectedDomainStorageKey = 'configurator.selected-domain-id';
export const componentCatalogViewStorageKey = 'configurator.component-catalog-view';

export const supportedLocales = ['ru', 'en'] as const;
export type SupportedLocale = (typeof supportedLocales)[number];

export function isSupportedLocale(value: string | null): value is SupportedLocale {
  return supportedLocales.some((locale) => locale === value);
}
