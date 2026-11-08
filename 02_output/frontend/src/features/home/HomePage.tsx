import { useTranslation } from 'react-i18next';
import { Link } from 'react-router';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';

/** Placeholder start page of the skeleton; the real start is the Group list (US-002). */
export function HomePage() {
  const { t } = useTranslation();
  return (
    <>
      <PageHeader title={t('home.title')} />
      <p className="mb-6 max-w-prose">{t('home.intro')}</p>
      <Button asChild>
        <Link to="/groups">{t('home.action')}</Link>
      </Button>
    </>
  );
}
