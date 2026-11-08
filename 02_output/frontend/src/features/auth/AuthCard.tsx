import type { ReactNode } from 'react';

/** The narrow card of the account screens WF-01 to WF-04, with the page's one `h1` (UI-19). */
export function AuthCard({ title, children }: { title?: string; children: ReactNode }) {
  return (
    <div className="mx-auto mt-8 flex w-full max-w-[440px] flex-col gap-4 rounded-[12px] border bg-surface-raised p-6">
      {title ? <h1 className="text-[2rem] leading-tight font-semibold">{title}</h1> : null}
      {children}
    </div>
  );
}
