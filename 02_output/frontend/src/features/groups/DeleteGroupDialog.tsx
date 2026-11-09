import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import { ApiError, errorKey, errorMessage } from '@/api/errors';
import { trashGroup } from '@/api/groups';
import { FormField } from '@/components/FormField';
import { Notice } from '@/components/Notice';
import { Button } from '@/components/ui/button';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';

interface DeleteGroupDialogProps {
  /** The Group to delete; the dialog is open while there is one. */
  group: { id: string; name: string } | undefined;
  onClose: () => void;
  /** Called after the Group went to the Group Trash. */
  onDeleted?: () => void;
}

/** WF-14: delete a Group by typing its name; it goes to the Group Trash for 30 days (R-020-01, R-020-02). */
export function DeleteGroupDialog({ group, onClose, onDeleted }: DeleteGroupDialogProps) {
  return (
    <Dialog open={group !== undefined} onOpenChange={(open) => (open ? undefined : onClose())}>
      <DialogContent>
        {group ? <DeleteGroupForm group={group} onClose={onClose} onDeleted={onDeleted} /> : null}
      </DialogContent>
    </Dialog>
  );
}

function DeleteGroupForm({
  group,
  onClose,
  onDeleted,
}: {
  group: { id: string; name: string };
  onClose: () => void;
  onDeleted?: () => void;
}) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const [typed, setTyped] = useState('');
  const remove = useMutation({
    mutationFn: () => trashGroup(group.id, typed),
    onSuccess: async () => {
      toast.success(t('deleteGroup.done'), {
        action: { label: t('deleteGroup.openTrash'), onClick: () => void navigate('/group-trash') },
      });
      onClose();
      onDeleted?.();
      await queryClient.invalidateQueries({ queryKey: ['groups'] });
      await queryClient.invalidateQueries({ queryKey: ['group-trash'] });
    },
  });
  const matches = typed.trim() === group.name;
  const mismatch = remove.error instanceof ApiError && remove.error.code === 'group.name_mismatch';
  const busy = remove.isPending;

  function submit(event: FormEvent) {
    event.preventDefault();
    if (matches) remove.mutate();
  }

  return (
    <>
      <DialogHeader>
        <DialogTitle className="break-words">{t('deleteGroup.title', { name: group.name })}</DialogTitle>
        <DialogDescription>{t('deleteGroup.body')}</DialogDescription>
      </DialogHeader>
      {remove.isError && !mismatch ? <Notice tone="alert" title={errorMessage(t, remove.error)} /> : null}
      <form noValidate onSubmit={submit} className="flex flex-col gap-4">
        <FormField
          id="delete-group-name"
          label={t('deleteGroup.nameLabel')}
          autoComplete="off"
          value={typed}
          error={mismatch ? t(errorKey('group.name_mismatch')) : undefined}
          onChange={(e) => setTyped(e.target.value)}
        />
        <DialogFooter>
          <Button type="button" variant="ghost" className="h-11" onClick={onClose} disabled={busy}>
            {t('common.cancel')}
          </Button>
          <Button
            type="submit"
            variant="destructive"
            className="h-11"
            disabled={!matches || busy}
            aria-busy={busy || undefined}
          >
            {busy ? t('deleteGroup.busy') : t('deleteGroup.submit')}
          </Button>
        </DialogFooter>
      </form>
    </>
  );
}
