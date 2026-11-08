import { useTranslation } from 'react-i18next';
import { Button } from '@/components/ui/button';

/** Error state (UI-14): plain message and a retry, never a technical detail (ES-07). */
export function ErrorState({ message, onRetry }: { message?: string; onRetry?: () => void }) {
  const { t } = useTranslation();
  return (
    <section
      role="alert"
      className="flex flex-col items-center gap-3 rounded-[12px] border bg-surface p-8 text-center"
    >
      <p>{message ?? t('error.generic')}</p>
      {onRetry ? (
        <Button variant="secondary" onClick={onRetry}>
          {t('common.retry')}
        </Button>
      ) : null}
    </section>
  );
}
