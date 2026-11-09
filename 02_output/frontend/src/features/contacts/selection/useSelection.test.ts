import { act, renderHook } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { useSelection } from './useSelection';

describe('useSelection', () => {
  it('adds and removes single Contacts and counts them (R-010-03, R-010-09)', () => {
    const { result } = renderHook(() => useSelection(''));

    act(() => result.current.toggle('a'));
    act(() => result.current.toggle('b'));
    expect(result.current.count(60)).toBe(2);
    expect(result.current.isSelected('a')).toBe(true);

    act(() => result.current.toggle('b'));
    expect(result.current.count(60)).toBe(1);
    expect(result.current.isSelected('b')).toBe(false);
  });

  it('adds the whole page and removes it again when all of it is selected (R-010-04)', () => {
    const { result } = renderHook(() => useSelection(''));

    act(() => result.current.togglePage(['a', 'b', 'c']));
    expect(result.current.count(60)).toBe(3);
    expect(result.current.pageSelected(['a', 'b', 'c'])).toBe(true);

    act(() => result.current.togglePage(['a', 'b', 'c']));
    expect(result.current.count(60)).toBe(0);
  });

  it('selects all results: the count is the total and a Contact can still be unticked (R-010-06)', () => {
    const { result } = renderHook(() => useSelection(''));

    act(() => result.current.selectAll());
    expect(result.current.count(60)).toBe(60);
    expect(result.current.isSelected('zzz')).toBe(true);

    act(() => result.current.toggle('zzz'));
    expect(result.current.count(60)).toBe(59);
    expect(result.current.isSelected('zzz')).toBe(false);
    act(() => result.current.toggle('zzz'));
    expect(result.current.count(60)).toBe(60);
  });

  it('clears everything (R-010-09)', () => {
    const { result } = renderHook(() => useSelection(''));
    act(() => result.current.selectAll());

    act(() => result.current.clear());

    expect(result.current.count(60)).toBe(0);
    expect(result.current.isSelected('a')).toBe(false);
  });

  it('keeps the Selection while the key stays and clears it when the key changes (R-010-07, 08, EX-010-04)', () => {
    const { result, rerender } = renderHook(({ key }) => useSelection(key), { initialProps: { key: 'q=a' } });
    act(() => result.current.toggle('a'));

    rerender({ key: 'q=a' });
    expect(result.current.count(5)).toBe(1);

    rerender({ key: 'q=b' });
    expect(result.current.count(5)).toBe(0);
    rerender({ key: 'q=a' });
    expect(result.current.count(5)).toBe(0);
  });
});
