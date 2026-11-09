import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { ApiError } from '@/api/errors';
import { restoreVersion } from '@/api/versions';
import type { Contact } from '@/api/contacts';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { Button } from '@/components/ui/button';

interface RestoreVersionButtonProps {
  groupId: string;
  contactId: string;
  /** The Version to restore. */
  number: number;
  /** The current version number the user sees (R-023-04). */
  current: number;
  /** "Obnovi" in the list, "Obnovi to različico" in an opened Version (WF-07). */
  label: string;
  variant?: 'secondary' | 'ghost';
  /** Called before a restore starts (the earlier refusal goes away) and with the refusal when it fails. */
  onRefused: (error: unknown) => void;
  onRestored: () => void;
}

/**
 * The button "Obnovi" of WF-07 with its confirmation (R-023-01, UI-15): saves the content of an
 * earlier Version as a new Version. A refusal (R-023-03, R-023-04) is handed to the tab to show.
 */
export function RestoreVersionButton({
  groupId,
  contactId,
  number,
  current,
  label,
  variant = 'secondary',
  onRefused,
  onRestored,
}: RestoreVersionButtonProps) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [confirming, setConfirming] = useState(false);
  const contactKey = ['contacts', groupId, contactId];
  const restore = useMutation({
    mutationFn: () => restoreVersion(groupId, contactId, number, current),
    onMutate: () => onRefused(null),
    onSuccess: async (contact: Contact) => {
      queryClient.setQueryData(contactKey, contact);
      await queryClient.invalidateQueries({ queryKey: ['contacts', groupId] });
      toast.success(t('versions.restored', { number, version: contact.version }));
      onRestored();
    },
    onError: async (error) => {
      if (error instanceof ApiError && error.code === 'contact.changed_by_someone_else') {
        queryClient.setQueryData(contactKey, error.current as Contact);
        await queryClient.invalidateQueries({ queryKey: ['contacts', groupId] });
      }
      onRefused(error);
    },
    onSettled: () => setConfirming(false),
  });

  return (
    <>
      <Button variant={variant} onClick={() => setConfirming(true)}>
        {label}
      </Button>
      <ConfirmDialog
        open={confirming}
        title={t('versions.restoreTitle', { number })}
        description={t('versions.restoreBody', { number, next: current + 1 })}
        confirmLabel={restore.isPending ? t('versions.restoreBusy') : t('versions.restore')}
        busy={restore.isPending}
        onConfirm={() => restore.mutate()}
        onCancel={() => setConfirming(false)}
      />
    </>
  );
}
