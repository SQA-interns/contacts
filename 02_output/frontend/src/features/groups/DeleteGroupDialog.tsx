import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';
import { toast } from 'sonner';
import { errorMessage } from '@/api/errors';
import { trashGroup } from '@/api/groups';
import { FormField } from '@/components/FormField';
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

interface Deletion {
  id: string;
  name: string;
  toastId: string | number;
}

/**
 * WF-14: delete a Group by typing its name; it goes to the Group Trash for 30 days (R-020-01, R-020-02). The dialog
 * closes on confirming, so that the Group stays in the list until the server has moved it (D-62); the progress, the
 * result and a refusal are shown in a toast.
 */
export function DeleteGroupDialog({ group, onClose, onDeleted }: DeleteGroupDialogProps) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const remove = useMutation({
    mutationFn: ({ id, name }: Deletion) => trashGroup(id, name),
    onSuccess: async (_none, { toastId }) => {
      toast.success(t('deleteGroup.done'), {
        id: toastId,
        action: { label: t('deleteGroup.openTrash'), onClick: () => void navigate('/group-trash') },
      });
      onDeleted?.();
      await queryClient.invalidateQueries({ queryKey: ['groups'] });
      await queryClient.invalidateQueries({ queryKey: ['group-trash'] });
    },
    onError: (error, { toastId }) => {
      toast.error(errorMessage(t, error), { id: toastId });
    },
  });

  function confirm(typed: string) {
    if (!group) return;
    const toastId = toast.loading(t('deleteGroup.busy'));
    onClose();
    remove.mutate({ id: group.id, name: typed, toastId });
  }

  return (
    <Dialog open={group !== undefined} onOpenChange={(open) => (open ? undefined : onClose())}>
      <DialogContent className="data-[state=closed]:animate-none">
        {group ? <DeleteGroupForm name={group.name} onConfirm={confirm} onCancel={onClose} /> : null}
      </DialogContent>
    </Dialog>
  );
}

function DeleteGroupForm({
  name,
  onConfirm,
  onCancel,
}: {
  name: string;
  onConfirm: (typed: string) => void;
  onCancel: () => void;
}) {
  const { t } = useTranslation();
  const [typed, setTyped] = useState('');
  const matches = typed.trim() === name;

  function submit(event: FormEvent) {
    event.preventDefault();
    if (matches) onConfirm(typed);
  }

  return (
    <>
      <DialogHeader>
        <DialogTitle className="break-words">{t('deleteGroup.title', { name })}</DialogTitle>
        <DialogDescription>{t('deleteGroup.body')}</DialogDescription>
      </DialogHeader>
      <form noValidate onSubmit={submit} className="flex flex-col gap-4">
        <FormField
          id="delete-group-name"
          label={t('deleteGroup.nameLabel')}
          autoComplete="off"
          value={typed}
          onChange={(e) => setTyped(e.target.value)}
        />
        <DialogFooter>
          <Button type="button" variant="ghost" className="h-11" onClick={onCancel}>
            {t('common.cancel')}
          </Button>
          <Button type="submit" variant="destructive" className="h-11" disabled={!matches}>
            {t('deleteGroup.submit')}
          </Button>
        </DialogFooter>
      </form>
    </>
  );
}
