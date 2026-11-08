import type { ReactNode } from 'react';

/**
 * A message at the top of a form: `alert` for a failure, `status` for a confirmation (UI-13, UI-15).
 */
export function Notice({
  tone,
  title,
  children,
}: {
  tone: 'alert' | 'status';
  title: string;
  children?: ReactNode;
}) {
  return (
    <div
      role={tone}
      className={`flex flex-col gap-2 rounded-[8px] border-2 bg-surface-raised px-4 py-3 ${
        tone === 'alert' ? 'border-danger' : 'border-border'
      }`}
    >
      <p className="font-semibold">{title}</p>
      {children}
    </div>
  );
}
