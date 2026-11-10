import { useState, type KeyboardEvent, type ReactNode } from 'react';

/** The most suggestions shown at once. */
const MAX_SUGGESTIONS = 8;

interface UseSuggestionsOptions {
  /** Id of the input; the list gets `<id>-options`. */
  id: string;
  /** What is typed in the input now. */
  value: string;
  /** Candidates; the ones that contain the typed text are shown (case does not matter). */
  suggestions: string[];
  /** Accessible name of the list. */
  label: string;
  /** Called with the suggestion the User picked with the mouse or with Enter on the active option. */
  onChoose: (suggestion: string) => void;
}

/**
 * A list of suggestions under a text input (WF-07, WF-08), as a combobox with a listbox: visible, reachable with
 * mouse and keyboard (arrows, Enter, Escape). Spread `inputProps` on the input, call `typed()` in its `onChange`
 * and draw `listbox` next to it inside a `relative` parent.
 */
export function useSuggestions({ id, value, suggestions, label, onChoose }: UseSuggestionsOptions) {
  const [focused, setFocused] = useState(false);
  const [dismissed, setDismissed] = useState(false);
  const [active, setActive] = useState(-1);
  const needle = value.trim().toLowerCase();
  const matches = suggestions
    .filter((item) => item.toLowerCase() !== needle && item.toLowerCase().includes(needle))
    .slice(0, MAX_SUGGESTIONS);
  const open = focused && !dismissed && matches.length > 0;
  const listId = `${id}-options`;

  const choose = (item: string) => {
    onChoose(item);
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

  const listbox: ReactNode = open ? (
    <ul
      id={listId}
      role="listbox"
      aria-label={label}
      className="absolute z-10 mt-1 w-full rounded-[8px] border bg-background p-1 shadow-md"
    >
      {matches.map((item, index) => (
        <li
          key={item}
          id={`${listId}-${index}`}
          role="option"
          aria-selected={index === active}
          className={`cursor-pointer rounded-[6px] px-3 py-2.5 ${index === active ? 'bg-surface' : ''}`}
          // mousedown, not click: the click would come after the input lost focus and closed the list
          onMouseDown={(event) => {
            event.preventDefault();
            choose(item);
          }}
        >
          {item}
        </li>
      ))}
    </ul>
  ) : null;

  return {
    inputProps: {
      role: 'combobox' as const,
      'aria-expanded': open,
      'aria-controls': open ? listId : undefined,
      'aria-autocomplete': 'list' as const,
      'aria-activedescendant': open && active >= 0 ? `${listId}-${active}` : undefined,
      autoComplete: 'off',
      onFocus: () => setFocused(true),
      onBlur: () => setFocused(false),
      onKeyDown,
    },
    /** Call when the text changes: the list opens again and no option is active. */
    typed: () => {
      setDismissed(false);
      setActive(-1);
    },
    listbox,
  };
}
