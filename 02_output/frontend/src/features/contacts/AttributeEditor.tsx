import { useState, type KeyboardEvent } from 'react';
import { useTranslation } from 'react-i18next';
import type { AttributePair } from '@/api/contacts';
import { FormField } from '@/components/FormField';
import { Button } from '@/components/ui/button';

/** The most pairs a Contact can have (R-004-04). */
export const MAX_ATTRIBUTES = 50;

interface AttributeEditorProps {
  pairs: AttributePair[];
  onChange: (pairs: AttributePair[]) => void;
  /** Messages by field name, e.g. `attributes[1].key`; the server judges the pairs (R-004-02..04). */
  errors: Record<string, string | undefined>;
  /** Keys already used in the Group (R-004-05). */
  suggestions: string[];
}

/** The most suggested keys shown at once. */
const MAX_SUGGESTIONS = 8;

interface KeyFieldProps {
  id: string;
  value: string;
  error?: string;
  suggestions: string[];
  onChange: (key: string) => void;
}

/** The key input with the keys of the Group that match what is typed, as a list under the field (R-004-05, WF-08). */
function KeyField({ id, value, error, suggestions, onChange }: KeyFieldProps) {
  const { t } = useTranslation();
  const [focused, setFocused] = useState(false);
  const [dismissed, setDismissed] = useState(false);
  const [active, setActive] = useState(-1);
  const needle = value.trim().toLowerCase();
  const matches = suggestions
    .filter((key) => key.toLowerCase() !== needle && key.toLowerCase().includes(needle))
    .slice(0, MAX_SUGGESTIONS);
  const open = focused && !dismissed && matches.length > 0;
  const listId = `${id}-options`;

  const choose = (key: string) => {
    onChange(key);
    setActive(-1);
  };
  const type = (key: string) => {
    onChange(key);
    setDismissed(false);
    setActive(-1);
  };
  const onKeyDown = (event: KeyboardEvent) => {
    if (event.key === 'Escape') return setDismissed(true);
    if (!open) return;
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setActive((active + 1) % matches.length);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setActive((active - 1 + matches.length) % matches.length);
    } else if (event.key === 'Enter' && matches[active] !== undefined) {
      event.preventDefault();
      choose(matches[active]);
    }
  };

  return (
    <div className="relative">
      <FormField
        id={id}
        label={t('contacts.attributeKey')}
        role="combobox"
        aria-expanded={open}
        aria-controls={open ? listId : undefined}
        aria-autocomplete="list"
        aria-activedescendant={open && active >= 0 ? `${listId}-${active}` : undefined}
        autoComplete="off"
        value={value}
        error={error}
        onChange={(e) => type(e.target.value)}
        onFocus={() => setFocused(true)}
        onBlur={() => setFocused(false)}
        onKeyDown={onKeyDown}
      />
      {open ? (
        <ul
          id={listId}
          role="listbox"
          aria-label={t('contacts.attributeSuggestions')}
          className="absolute z-10 mt-1 w-full rounded-[8px] border bg-background p-1 shadow-md"
        >
          {matches.map((key, index) => (
            <li
              key={key}
              id={`${listId}-${index}`}
              role="option"
              aria-selected={index === active}
              className={`cursor-pointer rounded-[6px] px-3 py-2.5 ${index === active ? 'bg-surface' : ''}`}
              // mousedown, not click: the click would come after the input lost focus and closed the list
              onMouseDown={(event) => {
                event.preventDefault();
                choose(key);
              }}
            >
              {key}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  );
}

/** Key-value pairs of a Contact: add, change, remove, with key suggestions (R-004-02..05). */
export function AttributeEditor({ pairs, onChange, errors, suggestions }: AttributeEditorProps) {
  const { t } = useTranslation();
  const full = pairs.length >= MAX_ATTRIBUTES;
  const set = (index: number, change: Partial<AttributePair>) =>
    onChange(pairs.map((pair, i) => (i === index ? { ...pair, ...change } : pair)));

  return (
    <fieldset className="flex flex-col gap-3">
      <legend className="mb-1 text-xl font-semibold">{t('contacts.attributes')}</legend>
      {pairs.map((pair, index) => (
        <div
          key={index}
          role="group"
          aria-label={t('contacts.attributeRow', { n: index + 1 })}
          className="grid items-start gap-3 sm:grid-cols-[1fr_1fr_auto]"
        >
          <KeyField
            id={`attribute-key-${index}`}
            value={pair.key}
            error={errors[`attributes[${index}].key`]}
            suggestions={suggestions}
            onChange={(key) => set(index, { key })}
          />
          <FormField
            id={`attribute-value-${index}`}
            label={t('contacts.attributeValue')}
            autoComplete="off"
            value={pair.value}
            error={errors[`attributes[${index}].value`]}
            onChange={(e) => set(index, { value: e.target.value })}
          />
          <Button
            type="button"
            variant="ghost"
            className="h-11 sm:mt-7"
            aria-label={t('contacts.removeAttribute', { n: index + 1 })}
            onClick={() => onChange(pairs.filter((_, i) => i !== index))}
          >
            {t('contacts.remove')}
          </Button>
        </div>
      ))}
      {errors.attributes ? (
        <p role="alert" className="text-sm font-semibold text-danger">
          {errors.attributes}
        </p>
      ) : null}
      {full ? <p className="text-sm text-text-muted">{t('contacts.attributeLimit')}</p> : null}
      <div>
        <Button
          type="button"
          variant="outline"
          className="h-11"
          disabled={full}
          onClick={() => onChange([...pairs, { key: '', value: '' }])}
        >
          {t('contacts.addAttribute')}
        </Button>
      </div>
    </fieldset>
  );
}
