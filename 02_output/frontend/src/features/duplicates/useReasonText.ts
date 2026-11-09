import { useTranslation } from 'react-i18next';
import type { DuplicateSet } from '@/api/duplicates';

/** The reason of a Duplicate set in the active language, with the plural form of its Contact count (R-024-05). */
export function useReasonText() {
  const { t, i18n } = useTranslation();
  return (set: DuplicateSet) => {
    const count = Number(set.reasonParams?.count ?? set.contacts.length);
    const form = new Intl.PluralRules(i18n.language).select(count);
    const key = form === 'one' || form === 'two' || form === 'few' ? form : 'other';
    const code = set.reasonCode.replace(/^duplicate\./, '');
    return t(`duplicates.reason.${code}.${key}`, { ...set.reasonParams, count });
  };
}
