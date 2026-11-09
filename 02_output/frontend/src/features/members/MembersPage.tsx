import { useState } from 'react';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router';
import { errorMessage } from '@/api/errors';
import type { Role } from '@/api/groups';
import { changeMemberRole, listMembers, removeMember, type Member } from '@/api/members';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { ErrorState } from '@/components/ErrorState';
import { LoadingState } from '@/components/LoadingState';
import { Notice } from '@/components/Notice';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { useGroupRole } from '@/features/groups/useGroupRole';

const ROLES: Role[] = ['OWNER', 'EDITOR', 'VIEWER'];

/** Slovene has four plural forms; the catalog keeps one text for each. */
function countForm(count: number, language: string): 'one' | 'two' | 'few' | 'other' {
  const form = new Intl.PluralRules(language).select(count);
  return form === 'one' || form === 'two' || form === 'few' ? form : 'other';
}

/** SC-11 / WF-11: the Members of a Group; an Owner changes Roles and removes (R-015-01..05). */
export function MembersPage() {
  const { t, i18n } = useTranslation();
  const { groupId = '' } = useParams();
  const queryClient = useQueryClient();
  const role = useGroupRole(groupId);
  const isOwner = role === 'OWNER';
  const members = useQuery({
    queryKey: ['members', groupId],
    queryFn: () => listMembers(groupId),
    enabled: role !== undefined,
  });
  const [removing, setRemoving] = useState<Member | undefined>();
  const [done, setDone] = useState<'roleChanged' | 'removed' | undefined>();
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['members', groupId] });
  const change = useMutation({
    mutationFn: (v: { member: Member; role: Role }) => changeMemberRole(groupId, v.member.userId, v.role),
    onMutate: () => setDone(undefined),
    onSuccess: () => setDone('roleChanged'),
    onSettled: async () => {
      await refresh();
      await queryClient.invalidateQueries({ queryKey: ['groups'] });
    },
  });
  const remove = useMutation({
    mutationFn: (member: Member) => removeMember(groupId, member.userId),
    onMutate: () => setDone(undefined),
    onSuccess: () => setDone('removed'),
    onSettled: async () => {
      setRemoving(undefined);
      await refresh();
    },
  });

  if (role === undefined) return <LoadingState rows={4} />;
  const failure = change.error ?? remove.error;
  return (
    <>
      <PageHeader
        title={t('members.title')}
        action={
          isOwner ? (
            <Button asChild>
              <Link to={`/groups/${encodeURIComponent(groupId)}/invitations`}>{t('members.invite')}</Link>
            </Button>
          ) : undefined
        }
      />
      {members.data ? (
        <p className="-mt-4 mb-6 text-text-muted">
          {t(`members.count.${countForm(members.data.length, i18n.language)}`, {
            count: members.data.length,
          })}
        </p>
      ) : null}
      {failure ? (
        <div className="mb-4">
          <Notice tone="alert" title={errorMessage(t, failure)} />
        </div>
      ) : null}
      {done && !failure ? (
        <div className="mb-4">
          <Notice tone="status" title={t(`members.${done}`)} />
        </div>
      ) : null}
      {members.isPending ? (
        <LoadingState rows={3} />
      ) : members.isError ? (
        <ErrorState onRetry={() => void members.refetch()} />
      ) : (
        <MemberTable
          members={members.data}
          isOwner={isOwner}
          busy={change.isPending}
          onRole={(member, next) => change.mutate({ member, role: next })}
          onRemove={setRemoving}
        />
      )}
      <ConfirmDialog
        open={removing !== undefined}
        title={t('members.removeTitle', { name: removing?.displayName ?? '' })}
        description={t('members.removeBody')}
        confirmLabel={t('members.removeConfirm')}
        destructive
        busy={remove.isPending}
        onConfirm={() => removing && remove.mutate(removing)}
        onCancel={() => setRemoving(undefined)}
      />
      <p className="mt-6">
        <Link
          to={`/groups/${encodeURIComponent(groupId)}/contacts`}
          className="underline-offset-4 hover:underline"
        >
          {t('contacts.back')}
        </Link>
      </p>
    </>
  );
}

interface MemberTableProps {
  members: Member[];
  isOwner: boolean;
  busy: boolean;
  onRole: (member: Member, role: Role) => void;
  onRemove: (member: Member) => void;
}

function MemberTable({ members, isOwner, busy, onRole, onRemove }: MemberTableProps) {
  const { t } = useTranslation();
  const roleCell = (m: Member) =>
    isOwner ? (
      <select
        aria-label={t('members.roleLabel')}
        className="h-11 min-w-[150px] rounded-md border border-input bg-transparent px-3"
        value={m.role}
        disabled={busy}
        onChange={(e) => onRole(m, e.target.value as Role)}
      >
        {ROLES.map((r) => (
          <option key={r} value={r}>
            {t(`role.${r}`)}
          </option>
        ))}
      </select>
    ) : (
      t(`role.${m.role}`)
    );
  const removeButton = (m: Member) => (
    <Button
      variant="ghost"
      className="h-11"
      aria-label={t('members.removeFor', { name: m.displayName })}
      onClick={() => onRemove(m)}
    >
      {t('members.remove')}
    </Button>
  );
  return (
    <>
      <table className="hidden w-full table-fixed border-collapse md:table">
        <caption className="sr-only">{t('members.tableCaption')}</caption>
        <thead>
          <tr className="text-left">
            <th scope="col" className="border-b p-3 font-semibold">
              {t('members.nameColumn')}
            </th>
            <th scope="col" className="border-b p-3 font-semibold">
              {t('members.emailColumn')}
            </th>
            <th scope="col" className="border-b p-3 font-semibold">
              {t('members.roleColumn')}
            </th>
            {isOwner ? (
              <th scope="col" className="w-40 border-b p-3 text-right font-semibold">
                {t('members.actionsColumn')}
              </th>
            ) : null}
          </tr>
        </thead>
        <tbody>
          {members.map((m) => (
            <tr key={m.userId}>
              <td className="truncate border-b p-3" title={m.displayName}>
                {m.displayName}
              </td>
              <td className="truncate border-b p-3" title={m.email}>
                {m.email}
              </td>
              <td className="border-b p-3">{roleCell(m)}</td>
              {isOwner ? <td className="border-b p-3 text-right">{removeButton(m)}</td> : null}
            </tr>
          ))}
        </tbody>
      </table>
      <ul className="flex flex-col gap-3 md:hidden" aria-label={t('members.listLabel')}>
        {members.map((m) => (
          <li key={m.userId} className="flex flex-col gap-2 rounded-[12px] border bg-surface-raised p-4">
            <p className="min-w-0 truncate font-semibold">{m.displayName}</p>
            <p className="min-w-0 truncate text-text-muted">{m.email}</p>
            <div>{roleCell(m)}</div>
            {isOwner ? removeButton(m) : null}
          </li>
        ))}
      </ul>
    </>
  );
}
