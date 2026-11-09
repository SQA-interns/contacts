import { useTranslation } from 'react-i18next';
import { ConfirmDialog } from '@/components/ConfirmDialog';

interface DeleteForeverDialogProps {
  open: boolean;
  busy: boolean;
  onConfirm: () => void;
  onCancel: () => void;
}

/** WF-10 "delete forever": the confirmation says the deletion cannot be undone (R-013-02). */
export function DeleteForeverDialog({ open, busy, onConfirm, onCancel }: DeleteForeverDialogProps) {
  const { t } = useTranslation();
  return (
    <ConfirmDialog
      open={open}
      title={t('trash.foreverTitle')}
      description={t('trash.foreverBody')}
      confirmLabel={t('trash.forever')}
      destructive
      busy={busy}
      onConfirm={onConfirm}
      onCancel={onCancel}
    />
  );
}
