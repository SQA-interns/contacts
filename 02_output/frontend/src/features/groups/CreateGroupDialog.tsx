import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { createGroup } from '@/api/groups';
import { ApiError, errorKey } from '@/api/errors';
import { FormField } from '@/components/FormField';
import { Notice } from '@/components/Notice';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';

const MAX_NAME_LENGTH = 100;

/** The field error the server reports for the name: R-002-02 or R-002-03; `null` for any other failure. */
function nameErrorCode(error: unknown): string | null {
  if (!(error instanceof ApiError)) return null;
  if (error.code === 'group.name_taken') return error.code;
  return error.fieldErrors.find((f) => f.field === 'name')?.code ?? null;
}

interface CreateGroupDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

/** WF-05 create dialog: one field and one click (R-002-01..03). */
export function CreateGroupDialog({ open, onOpenChange }: CreateGroupDialogProps) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [name, setName] = useState('');
  const [tooShortOrLong, setTooShortOrLong] = useState(false);
  const create = useMutation({
    mutationFn: createGroup,
    onSuccess: async () => {
      toast.success(t('groups.created'));
      close(false);
      await queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });

  function close(next: boolean) {
    if (!next) {
      setName('');
      setTooShortOrLong(false);
      create.reset();
    }
    onOpenChange(next);
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    const trimmed = name.trim();
    const invalid = trimmed.length === 0 || trimmed.length > MAX_NAME_LENGTH;
    setTooShortOrLong(invalid);
    if (!invalid) create.mutate(trimmed);
  }

  const serverCode = nameErrorCode(create.error);
  const fieldCode = tooShortOrLong ? 'validation.name_invalid' : serverCode;
  const busy = create.isPending;

  return (
    <Dialog open={open} onOpenChange={close}>
      <DialogContent aria-describedby={undefined}>
        <DialogHeader>
          <DialogTitle>{t('groups.newTitle')}</DialogTitle>
        </DialogHeader>
        {create.isError && !serverCode ? <Notice tone="alert" title={t('error.generic')} /> : null}
        <form noValidate onSubmit={submit} className="flex flex-col gap-4">
          <FormField
            id="group-name"
            label={t('groups.nameLabel')}
            hint={t('groups.nameHint')}
            required
            autoComplete="off"
            value={name}
            error={fieldCode ? t(errorKey(fieldCode)) : undefined}
            onChange={(e) => setName(e.target.value)}
          />
          <DialogFooter>
            <Button
              type="button"
              variant="ghost"
              className="h-11"
              onClick={() => close(false)}
              disabled={busy}
            >
              {t('common.cancel')}
            </Button>
            <Button type="submit" className="h-11" disabled={busy} aria-busy={busy || undefined}>
              {busy ? t('groups.busy') : t('groups.submit')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
