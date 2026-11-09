import { useTranslation } from 'react-i18next';

/** "30 days" for a count of days left in Trash; Slovene has four plural forms, the catalog one text for each. */
export function useDaysText() {
  const { t, i18n } = useTranslation();
  return (count: number) => {
    const form = new Intl.PluralRules(i18n.language).select(count);
    const key = form === 'one' || form === 'two' || form === 'few' ? form : 'other';
    return t(`trash.days.${key}`, { count });
  };
}
