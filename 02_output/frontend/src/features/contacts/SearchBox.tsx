import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

/** Milliseconds to wait after the last key before the search text is sent. */
const DELAY = 300;

/**
 * The one search box of the Contact list (R-008-01). It reports the trimmed text once the typing
 * pauses; a text of spaces is reported as empty, which is no search (R-008-05). The text is only
 * ever shown as a value, never as markup (R-008-06).
 */
export function SearchBox({ value, onSearch }: { value: string; onSearch: (text: string) => void }) {
  const { t } = useTranslation();
  const [text, setText] = useState(value);
  const sent = useRef(value.trim());
  const report = useRef(onSearch);
  report.current = onSearch;

  // The text changed from outside (Clear filters): show it.
  useEffect(() => {
    if (value.trim() !== sent.current) {
      sent.current = value.trim();
      setText(value);
    }
  }, [value]);

  useEffect(() => {
    const trimmed = text.trim();
    if (trimmed === sent.current) return;
    const timer = setTimeout(() => {
      sent.current = trimmed;
      report.current(trimmed);
    }, DELAY);
    return () => clearTimeout(timer);
  }, [text]);

  return (
    <div role="search" className="mb-6 flex max-w-md flex-col gap-1.5">
      <Label htmlFor="contact-search">{t('contactList.search')}</Label>
      <Input
        id="contact-search"
        type="search"
        autoComplete="off"
        placeholder={t('contactList.searchHint')}
        value={text}
        onChange={(event) => setText(event.target.value)}
      />
    </div>
  );
}
