import type { ReactNode } from 'react';

/** The narrow card of the account screens WF-01 to WF-04. */
export function AuthCard({ children }: { children: ReactNode }) {
  return (
    <div className="mx-auto mt-8 flex w-full max-w-[440px] flex-col gap-4 rounded-[12px] border bg-surface-raised p-6">
      {children}
    </div>
  );
}
