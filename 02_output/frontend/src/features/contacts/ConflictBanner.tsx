import { useTranslation } from 'react-i18next';
import { Notice } from '@/components/Notice';

/** R-003-07 / BR-07: the save was refused because someone else saved first; the current Contact is shown. */
export function ConflictBanner({ version }: { version: number }) {
  const { t } = useTranslation();
  return (
    <Notice tone="alert" title={t('api.contact.changed_by_someone_else')}>
      <p>{t('contacts.conflictBody', { version })}</p>
    </Notice>
  );
}
