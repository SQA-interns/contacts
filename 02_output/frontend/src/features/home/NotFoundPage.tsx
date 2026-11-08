import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { EmptyState } from '@/components/EmptyState';
import { Button } from '@/components/ui/button';

/** Not found and "no access" look the same (BR-01). */
export function NotFoundPage() {
  const { t } = useTranslation();
  return (
    <EmptyState
      title={t('error.notFoundTitle')}
      body={t('error.notFoundBody')}
      action={
        <Button asChild variant="secondary">
          <Link to="/">{t('error.backHome')}</Link>
        </Button>
      }
    />
  );
}
