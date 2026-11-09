import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';

interface SelectionBarProps {
  /** The number of selected Contacts. */
  count: number;
  /** The number of results; set when "select all results" is offered (R-010-05). */
  offerAll?: number;
  onSelectAll: () => void;
  onClear: () => void;
  /** Actions at the end of the bar (the Export button, US-011). */
  children?: ReactNode;
}

/** The count of the Selection in the language of the page; Slovene has four plural forms. */
export function useSelectedText() {
  const { t, i18n } = useTranslation();
  return (count: number) => {
    const form = new Intl.PluralRules(i18n.language).select(count);
    const key = form === 'one' || form === 'two' || form === 'few' ? form : 'other';
    return t(`selection.selected.${key}`, { count });
  };
}

/** WF-06: the Selection bar with the count, the offer to select all results and "Clear selection". */
export function SelectionBar({ count, offerAll, onSelectAll, onClear, children }: SelectionBarProps) {
  const { t } = useTranslation();
  const selectedText = useSelectedText();
  return (
    <section
      aria-label={t('selection.region')}
      className="mb-4 flex min-h-11 flex-wrap items-center gap-3 rounded-[12px] border bg-surface px-4 py-2"
    >
      <span className="font-semibold" aria-live="polite">
        {selectedText(count)}
      </span>{' '}
      {offerAll ? (
        <Button variant="ghost" onClick={onSelectAll}>
          {t('selection.selectAllResults', { count: offerAll })}
        </Button>
      ) : null}
      {count > 0 ? (
        <Button variant="ghost" onClick={onClear}>
          {t('selection.clear')}
        </Button>
      ) : null}
      {children}
    </section>
  );
}
