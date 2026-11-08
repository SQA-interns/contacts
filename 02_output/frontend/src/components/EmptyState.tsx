import type { ReactNode } from 'react';

/** Empty state (UI-14): a short explanation and the next action. */
export function EmptyState({ title, body, action }: { title: string; body?: string; action?: ReactNode }) {
  return (
    <section className="flex flex-col items-center gap-3 rounded-[12px] border bg-surface p-8 text-center">
      <h2 className="text-xl font-semibold">{title}</h2>
      {body ? <p className="max-w-prose text-text-muted">{body}</p> : null}
      {action}
    </section>
  );
}
