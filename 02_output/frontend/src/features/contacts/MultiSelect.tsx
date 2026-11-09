import { useId, useState } from 'react';
import { CheckIcon, ChevronDownIcon } from 'lucide-react';
import { cn } from '@/lib/utils';

/**
 * A list of values of which several can be chosen (the Tag filters, R-009-01..03). The button
 * opens a listbox whose options are the offered values; Escape closes it.
 */
export function MultiSelect({
  label,
  placeholder,
  options,
  value,
  onChange,
}: {
  label: string;
  placeholder: string;
  options: string[];
  value: string[];
  onChange: (next: string[]) => void;
}) {
  const [open, setOpen] = useState(false);
  const listId = useId();
  const buttonId = useId();
  const closeOnEscape = (e: React.KeyboardEvent) => {
    if (e.key === 'Escape') setOpen(false);
  };
  const toggle = (option: string) =>
    onChange(value.includes(option) ? value.filter((v) => v !== option) : [...value, option]);
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={buttonId} className="text-sm font-medium">
        {label}
      </label>
      <button
        id={buttonId}
        type="button"
        aria-haspopup="listbox"
        aria-expanded={open}
        aria-controls={listId}
        onClick={() => setOpen(!open)}
        onKeyDown={closeOnEscape}
        className="flex h-11 w-full items-center justify-between gap-2 rounded-md border border-input bg-transparent px-3 text-left text-base"
      >
        <span className={cn('truncate', value.length === 0 && 'text-text-muted')}>
          {value.length === 0 ? placeholder : value.join(', ')}
        </span>
        <ChevronDownIcon aria-hidden="true" className="size-4 shrink-0" />
      </button>
      {open ? (
        <ul
          id={listId}
          role="listbox"
          aria-label={label}
          aria-multiselectable="true"
          className="max-h-56 overflow-auto rounded-md border border-input"
        >
          {options.map((option) => {
            const selected = value.includes(option);
            return (
              <li
                key={option}
                role="option"
                aria-selected={selected}
                tabIndex={0}
                onClick={() => toggle(option)}
                onKeyDown={(e) => {
                  closeOnEscape(e);
                  if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    toggle(option);
                  }
                }}
                className="flex min-h-11 cursor-pointer items-center justify-between gap-2 px-3 hover:bg-surface-muted"
              >
                <span>{option}</span>
                {selected ? <CheckIcon aria-hidden="true" className="size-4" /> : null}
              </li>
            );
          })}
        </ul>
      ) : null}
    </div>
  );
}
