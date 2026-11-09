import { useState } from 'react';

/** Either the ticked Contacts, or all results of the search and filters except the unticked ones. */
interface State {
  key: string;
  all: boolean;
  ids: ReadonlySet<string>;
}

export interface Selection {
  isSelected: (id: string) => boolean;
  /** Ticks or unticks one Contact (R-010-03, R-010-09). */
  toggle: (id: string) => void;
  /** True when every one of the given Contacts (the page) is selected and the page is not empty. */
  pageSelected: (ids: string[]) => boolean;
  /** Selects the whole page, or removes it when all of it is already selected (R-010-04). */
  togglePage: (ids: string[]) => void;
  /** Selects all results on all pages (R-010-06). */
  selectAll: () => void;
  clear: () => void;
  /** The number of selected Contacts, `total` being the number of results. */
  count: (total: number) => number;
  /** True while "select all results" is active. */
  allResults: boolean;
  /** The ticked Contacts; empty while "select all results" is active. */
  ticked: string[];
  /** The Contacts taken out of "all results"; empty otherwise. */
  unticked: string[];
}

function flip(ids: ReadonlySet<string>, id: string, on: boolean): ReadonlySet<string> {
  const next = new Set(ids);
  if (on) next.add(id);
  else next.delete(id);
  return next;
}

/**
 * The Selection of the contact list (US-010). It lives in the open page only (EX-010-03) and is cleared when
 * `key`, which stands for the search text and the filters, changes (R-010-08); page and sort order are not part of it.
 * In "all results" mode `ids` holds the Contacts taken out of the Selection.
 */
export function useSelection(key: string): Selection {
  const [stored, setStored] = useState<State>({ key, all: false, ids: new Set() });
  const state: State = stored.key === key ? stored : { key, all: false, ids: new Set() };
  if (stored.key !== key) setStored(state);

  const isSelected = (id: string) => state.ids.has(id) !== state.all;
  const update = (change: (s: State) => Pick<State, 'all' | 'ids'>) =>
    setStored((old) => ({ key, ...change(old.key === key ? old : { key, all: false, ids: new Set() }) }));

  return {
    isSelected,
    toggle: (id) => update((s) => ({ all: s.all, ids: flip(s.ids, id, !s.ids.has(id)) })),
    pageSelected: (ids) => ids.length > 0 && ids.every(isSelected),
    togglePage: (ids) =>
      update((s) => {
        const select = !ids.every((id) => s.ids.has(id) !== s.all);
        // Selecting adds Contacts to `ids`, or removes them from the excluded ones in "all results" mode.
        const next = new Set(s.ids);
        for (const id of ids) {
          if (select !== s.all) next.add(id);
          else next.delete(id);
        }
        return { all: s.all, ids: next };
      }),
    selectAll: () => update(() => ({ all: true, ids: new Set() })),
    clear: () => update(() => ({ all: false, ids: new Set() })),
    count: (total) => (state.all ? Math.max(0, total - state.ids.size) : state.ids.size),
    allResults: state.all,
    ticked: state.all ? [] : [...state.ids],
    unticked: state.all ? [...state.ids] : [],
  };
}
