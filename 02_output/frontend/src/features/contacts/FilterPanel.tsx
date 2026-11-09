import { useEffect, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import type { ContactFilters, Facets } from '@/api/contacts';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { MultiSelect } from './MultiSelect';

const SELECT = 'h-11 w-full rounded-md border border-input bg-transparent px-3 text-base';

function countryName(code: string, language: string): string {
  try {
    return new Intl.DisplayNames([language], { type: 'region' }).of(code) ?? code;
  } catch {
    return code;
  }
}

/** The value of the Attribute filter is sent when the typing pauses, like the search text. */
function useDelayed(value: string, onChange: (text: string) => void) {
  const [text, setText] = useState(value);
  const sent = useRef(value);
  const report = useRef(onChange);
  report.current = onChange;
  useEffect(() => {
    if (value !== sent.current) {
      sent.current = value;
      setText(value);
    }
  }, [value]);
  useEffect(() => {
    if (text === sent.current) return;
    const timer = setTimeout(() => {
      sent.current = text;
      report.current(text);
    }, 300);
    return () => clearTimeout(timer);
  }, [text]);
  return [text, setText] as const;
}

/** WF-06 "Filtri": the filters of the Contact list (US-009); they offer only what the Group has (R-009-11). */
export function FilterPanel({
  facets,
  filters,
  onChange,
  onClear,
  className,
}: {
  facets: Facets;
  filters: ContactFilters;
  onChange: (next: ContactFilters) => void;
  onClear: () => void;
  className?: string;
}) {
  const { t, i18n } = useTranslation();
  const [value, setValue] = useDelayed(filters.attrValue, (attrValue) => onChange({ ...filters, attrValue }));
  const byValue = !filters.attrExists;
  return (
    <aside aria-label={t('filters.title')} className={className}>
      <div className="mb-4 flex items-center justify-between gap-2">
        <h2 className="text-lg font-semibold">{t('filters.title')}</h2>
        <Button variant="ghost" onClick={onClear}>
          {t('contactList.clearFilters')}
        </Button>
      </div>
      <div className="flex flex-col gap-5">
        <div className="flex flex-col gap-2">
          <MultiSelect
            label={t('contactList.tags')}
            placeholder={t('filters.chooseTags')}
            options={facets.tags}
            value={filters.tags}
            onChange={(tags) => onChange({ ...filters, tags })}
          />
          <fieldset className="flex flex-col gap-2">
            <legend className="sr-only">{t('filters.tagMode')}</legend>
            {(['all', 'any'] as const).map((mode) => (
              <Label key={mode} className="min-h-6 gap-2 font-normal">
                <input
                  type="radio"
                  name="tag-mode"
                  checked={filters.tagMode === mode}
                  onChange={() => onChange({ ...filters, tagMode: mode })}
                />
                {t(`filters.tagMode_${mode}`)}
              </Label>
            ))}
          </fieldset>
        </div>
        <MultiSelect
          label={t('filters.withoutTags')}
          placeholder={t('filters.chooseTags')}
          options={facets.tags}
          value={filters.withoutTags}
          onChange={(withoutTags) => onChange({ ...filters, withoutTags })}
        />
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="filter-country">{t('contactList.country')}</Label>
          <select
            id="filter-country"
            className={SELECT}
            value={filters.country}
            onChange={(e) => onChange({ ...filters, country: e.target.value })}
          >
            <option value="">{t('filters.allCountries')}</option>
            {facets.countries.map((code) => (
              <option key={code} value={code}>
                {countryName(code, i18n.language)}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="filter-company">{t('contactList.company')}</Label>
          <select
            id="filter-company"
            className={SELECT}
            value={filters.company}
            onChange={(e) => onChange({ ...filters, company: e.target.value })}
          >
            <option value="">{t('filters.allCompanies')}</option>
            {facets.companies.map((company) => (
              <option key={company} value={company}>
                {company}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-col gap-2">
          <Label htmlFor="filter-attr-key">{t('filters.attribute')}</Label>
          <select
            id="filter-attr-key"
            className={SELECT}
            value={filters.attrKey}
            onChange={(e) => onChange({ ...filters, attrKey: e.target.value })}
          >
            <option value="">{t('filters.attributeKey')}</option>
            {facets.attributeKeys.map((key) => (
              <option key={key} value={key.toLowerCase()}>
                {key}
              </option>
            ))}
          </select>
          <fieldset className="flex flex-col gap-2" disabled={!filters.attrKey}>
            <legend className="sr-only">{t('filters.attributeMode')}</legend>
            <Label className="min-h-6 gap-2 font-normal">
              <input
                type="radio"
                name="attr-mode"
                checked={byValue}
                onChange={() => onChange({ ...filters, attrExists: false })}
              />
              {t('filters.attributeHasValue')}
            </Label>
            <Input
              aria-label={t('filters.attributeValue')}
              placeholder={t('filters.attributeValue')}
              value={value}
              disabled={!byValue}
              onChange={(e) => setValue(e.target.value)}
            />
            <Label className="min-h-6 gap-2 font-normal">
              <input
                type="radio"
                name="attr-mode"
                checked={!byValue}
                onChange={() => onChange({ ...filters, attrExists: true })}
              />
              {t('filters.attributeExists')}
            </Label>
          </fieldset>
        </div>
        <p className="text-sm text-text-muted">{t('filters.offered')}</p>
      </div>
    </aside>
  );
}
