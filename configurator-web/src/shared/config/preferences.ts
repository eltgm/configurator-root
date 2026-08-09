export const colorSchemeStorageKey = 'configurator.color-scheme';
export const localeStorageKey = 'configurator.locale';

export const supportedLocales = ['ru', 'en'] as const;
export type SupportedLocale = (typeof supportedLocales)[number];

export function isSupportedLocale(value: string | null): value is SupportedLocale {
  return supportedLocales.some((locale) => locale === value);
}
