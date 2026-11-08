import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import en from './locales/en.json';
import sl from './locales/sl.json';

export const LANGUAGES = ['sl', 'en'] as const;
export type Language = (typeof LANGUAGES)[number];
export const DEFAULT_LANGUAGE: Language = 'sl';
const STORAGE_KEY = 'language';

export function isLanguage(value: unknown): value is Language {
  return typeof value === 'string' && (LANGUAGES as readonly string[]).includes(value);
}

/** The language chosen last in this browser, else Slovene (R-019-01, R-019-03). */
export function storedLanguage(storage: Pick<Storage, 'getItem'> | undefined = safeStorage()): Language {
  try {
    const value = storage?.getItem(STORAGE_KEY);
    return isLanguage(value) ? value : DEFAULT_LANGUAGE;
  } catch {
    return DEFAULT_LANGUAGE;
  }
}

export function setLanguage(language: Language): Promise<unknown> {
  try {
    safeStorage()?.setItem(STORAGE_KEY, language);
  } catch {
    /* storage may be blocked; the language still changes for this visit */
  }
  document.documentElement.lang = language;
  return i18n.changeLanguage(language);
}

function safeStorage(): Storage | undefined {
  try {
    return typeof window === 'undefined' ? undefined : window.localStorage;
  } catch {
    return undefined;
  }
}

void i18n.use(initReactI18next).init({
  resources: { sl: { translation: sl }, en: { translation: en } },
  lng: storedLanguage(),
  fallbackLng: DEFAULT_LANGUAGE,
  interpolation: { escapeValue: false },
  returnNull: false,
});
if (typeof document !== 'undefined') document.documentElement.lang = i18n.language;

export default i18n;
