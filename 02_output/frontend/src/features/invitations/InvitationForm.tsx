import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { ApiError, errorKey } from '@/api/errors';
import type { Role } from '@/api/groups';
import { createInvitation } from '@/api/invitations';
import { FormField } from '@/components/FormField';
import { Notice } from '@/components/Notice';
import { Button } from '@/components/ui/button';
import { Label } from '@/components/ui/label';

const ROLES: Role[] = ['OWNER', 'EDITOR', 'VIEWER'];
/** One `@`, a local part and a dotted domain without spaces; the server decides (R-014-02). */
function hasEmailFormat(email: string): boolean {
  const at = email.indexOf('@');
  const domain = email.slice(at + 1);
  return (
    at > 0 &&
    at === email.lastIndexOf('@') &&
    !/\s/.test(email) &&
    domain.includes('.') &&
    !domain.startsWith('.') &&
    !domain.endsWith('.') &&
    !domain.includes('..')
  );
}

/** The code of the error shown next to the e-mail field: R-014-02, R-014-04 or R-014-03. */
function fieldErrorCode(error: unknown): string | null {
  if (!(error instanceof ApiError)) return null;
  if (error.code === 'invitation.exists' || error.code === 'invitation.limit') return error.code;
  return error.fieldErrors.find((f) => f.field === 'email')?.code ?? null;
}

/** WF-12 form "Povabi osebo": e-mail address and Role (R-014-01..04, R-014-12). */
export function InvitationForm({ groupId }: { groupId: string }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [email, setEmail] = useState('');
  const [role, setRole] = useState<Role>('EDITOR');
  const [formatError, setFormatError] = useState(false);
  const create = useMutation({
    mutationFn: () => createInvitation(groupId, email.trim(), role),
    onSuccess: async () => {
      setEmail('');
      await queryClient.invalidateQueries({ queryKey: ['invitations', groupId] });
    },
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    create.reset();
    const invalid = !hasEmailFormat(email.trim());
    setFormatError(invalid);
    if (!invalid) create.mutate();
  }

  const code = formatError ? 'validation.email_invalid' : fieldErrorCode(create.error);
  const busy = create.isPending;
  return (
    <section className="rounded-[12px] border bg-surface-raised p-6" aria-labelledby="invite-heading">
      <h2 id="invite-heading" className="mb-4 text-lg font-semibold">
        {t('invitations.formTitle')}
      </h2>
      {create.isSuccess ? (
        <div className="mb-4">
          <Notice tone="status" title={t('invitations.created')} />
        </div>
      ) : null}
      {create.isError && !code ? <Notice tone="alert" title={t('error.generic')} /> : null}
      <form noValidate onSubmit={submit} className="flex flex-col gap-4">
        <FormField
          id="invite-email"
          type="email"
          label={t('invitations.emailLabel')}
          required
          autoComplete="off"
          value={email}
          error={code ? t(errorKey(code)) : undefined}
          onChange={(e) => {
            setEmail(e.target.value);
            setFormatError(false);
          }}
        />
        <div className="flex flex-col gap-1.5">
          <Label htmlFor="invite-role">
            {t('invitations.roleLabel')}
            <span className="font-normal text-text-muted">{t('form.required')}</span>
          </Label>
          <select
            id="invite-role"
            className="h-11 rounded-md border border-input bg-transparent px-3"
            value={role}
            onChange={(e) => setRole(e.target.value as Role)}
          >
            {ROLES.map((r) => (
              <option key={r} value={r}>
                {t(`role.${r}`)}
              </option>
            ))}
          </select>
        </div>
        <p className="text-sm text-text-muted">{t('invitations.hint')}</p>
        <div className="flex justify-end">
          <Button type="submit" className="h-11" disabled={busy} aria-busy={busy || undefined}>
            {t('invitations.submit')}
          </Button>
        </div>
      </form>
    </section>
  );
}
