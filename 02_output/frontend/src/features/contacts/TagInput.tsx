import { useId, useState, type KeyboardEvent } from 'react';
import { X } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

/** The most Tags a Contact can have (R-005-04). */
export const MAX_TAGS = 50;

/** A Tag name: 1 to 50 letters, digits, `-` or `_` (R-005-02); the server judges it again. */
const TAG_NAME = /^[\p{L}\p{N}_-]{1,50}$/u;

interface TagInputProps {
  tags: string[];
  onChange: (tags: string[]) => void;
  /** Message of the server for the Tags, shown next to the field. */
  error?: string;
  /** Tags already in the Group (R-005-07). */
  suggestions: string[];
  /** Called with the text as it is typed, so the caller can load the suggestions. */
  onType?: (text: string) => void;
}

/** The Tags of a Contact: type a name and confirm to put it on, remove it with its button. */
export function TagInput({ tags, onChange, error, suggestions, onType }: TagInputProps) {
  const { t } = useTranslation();
  const listId = useId();
  const [text, setText] = useState('');
  const [rule, setRule] = useState<string>();
  const full = tags.length >= MAX_TAGS;
  const same = (a: string, b: string) => a.toLowerCase() === b.toLowerCase();

  const type = (value: string) => {
    setText(value);
    setRule(undefined);
    onType?.(value);
  };
  const add = () => {
    const name = text.trim();
    if (!TAG_NAME.test(name)) return setRule(t('api.validation.tag_invalid'));
    if (tags.some((tag) => same(tag, name))) return type('');
    if (full) return setRule(t('api.validation.tag_limit'));
    onChange([...tags, suggestions.find((s) => same(s, name)) ?? name]);
    type('');
  };
  const onKeyDown = (event: KeyboardEvent) => {
    if (event.key !== 'Enter') return;
    event.preventDefault();
    add();
  };
  const message = rule ?? error;

  return (
    <fieldset className="flex flex-col gap-3">
      <legend className="mb-1 text-xl font-semibold">{t('contacts.tags')}</legend>
      {tags.length > 0 ? (
        <ul className="flex flex-wrap gap-2">
          {tags.map((tag) => (
            <li key={tag} className="flex items-center gap-1 rounded-[8px] border pl-2 text-sm">
              {tag}
              <Button
                type="button"
                variant="ghost"
                className="h-8 px-2"
                aria-label={t('contacts.removeTag', { tag })}
                onClick={() => onChange(tags.filter((other) => other !== tag))}
              >
                <X aria-hidden="true" className="size-4" />
              </Button>
            </li>
          ))}
        </ul>
      ) : null}
      <datalist id={listId}>
        {suggestions.map((name) => (
          <option key={name} value={name} />
        ))}
      </datalist>
      <div className="flex items-end gap-3">
        <div className="flex flex-1 flex-col gap-1.5">
          <Label htmlFor={`${listId}-tag`}>{t('contacts.addTag')}</Label>
          <Input
            id={`${listId}-tag`}
            className="h-11"
            list={listId}
            autoComplete="off"
            value={text}
            disabled={full}
            aria-invalid={message ? true : undefined}
            onChange={(e) => type(e.target.value)}
            onKeyDown={onKeyDown}
          />
        </div>
        <Button type="button" variant="outline" className="h-11" disabled={full} onClick={add}>
          {t('contacts.addTagButton')}
        </Button>
      </div>
      {message ? (
        <p role="alert" className="text-sm font-semibold text-danger">
          {message}
        </p>
      ) : null}
      {full ? <p className="text-sm text-text-muted">{t('api.validation.tag_limit')}</p> : null}
    </fieldset>
  );
}
