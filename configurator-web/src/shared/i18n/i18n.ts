import { createInstance } from 'i18next';
import { initReactI18next } from 'react-i18next';

import {
  isSupportedLocale,
  localeStorageKey,
  type SupportedLocale,
} from '@/shared/config/preferences';
import { resources } from '@/shared/i18n/resources';

function getInitialLocale(): SupportedLocale {
  if (typeof window === 'undefined') {
    return 'ru';
  }

  const storedLocale = window.localStorage.getItem(localeStorageKey);
  return isSupportedLocale(storedLocale) ? storedLocale : 'ru';
}

function applyDocumentLocale(locale: SupportedLocale) {
  if (typeof document !== 'undefined') {
    document.documentElement.lang = locale;
  }
}

export const i18n = createInstance();
const initialLocale = getInitialLocale();
applyDocumentLocale(initialLocale);

void i18n.use(initReactI18next).init({
  resources,
  lng: initialLocale,
  fallbackLng: 'ru',
  interpolation: {
    escapeValue: false,
  },
});

export async function changeLocale(locale: SupportedLocale) {
  window.localStorage.setItem(localeStorageKey, locale);
  applyDocumentLocale(locale);
  await i18n.changeLanguage(locale);
}
