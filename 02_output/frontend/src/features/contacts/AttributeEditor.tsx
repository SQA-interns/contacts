import { useId } from 'react';
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

/** Key-value pairs of a Contact: add, change, remove, with key suggestions (R-004-02..05). */
export function AttributeEditor({ pairs, onChange, errors, suggestions }: AttributeEditorProps) {
  const { t } = useTranslation();
  const listId = useId();
  const full = pairs.length >= MAX_ATTRIBUTES;
  const set = (index: number, change: Partial<AttributePair>) =>
    onChange(pairs.map((pair, i) => (i === index ? { ...pair, ...change } : pair)));

  return (
    <fieldset className="flex flex-col gap-3">
      <legend className="mb-1 text-xl font-semibold">{t('contacts.attributes')}</legend>
      <datalist id={listId}>
        {suggestions.map((key) => (
          <option key={key} value={key} />
        ))}
      </datalist>
      {pairs.map((pair, index) => (
        <div
          key={index}
          role="group"
          aria-label={t('contacts.attributeRow', { n: index + 1 })}
          className="grid items-start gap-3 sm:grid-cols-[1fr_1fr_auto]"
        >
          <FormField
            id={`attribute-key-${index}`}
            label={t('contacts.attributeKey')}
            list={listId}
            autoComplete="off"
            value={pair.key}
            error={errors[`attributes[${index}].key`]}
            onChange={(e) => set(index, { key: e.target.value })}
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
