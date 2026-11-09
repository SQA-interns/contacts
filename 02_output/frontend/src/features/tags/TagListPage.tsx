import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router';
import { toast } from 'sonner';
import { ApiError, errorKey, errorMessage } from '@/api/errors';
import { deleteTag, listTags, renameTag, type TagCount } from '@/api/tags';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { EmptyState } from '@/components/EmptyState';
import { ErrorState } from '@/components/ErrorState';
import { FormField } from '@/components/FormField';
import { LoadingState } from '@/components/LoadingState';
import { Notice } from '@/components/Notice';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog';
import { useGroupRole } from '@/features/groups/useGroupRole';

const TAG_NAME = /^[\p{L}\p{N}_-]{1,50}$/u;

/** Slovene has four plural forms; the catalog keeps one text for each (`one`, `two`, `few`, `other`). */
function useCountText() {
  const { t, i18n } = useTranslation();
  return (count: number) => {
    const form = new Intl.PluralRules(i18n.language).select(count);
    const key = form === 'one' || form === 'two' || form === 'few' ? form : 'other';
    return t(`tags.contactCount.${key}`, { count });
  };
}

/** The error code a failed rename shows next to the field; `null` for any other failure. */
function renameErrorCode(error: unknown): string | null {
  if (!(error instanceof ApiError)) return null;
  if (error.code === 'tag.name_taken') return error.code;
  return error.fieldErrors.find((f) => f.field === 'name')?.code ?? null;
}

/** SC-09 / WF-09: the Tags of the Group with Contact counts, rename and delete (R-006-01..08). */
export function TagListPage() {
  const { t } = useTranslation();
  const { groupId = '' } = useParams();
  const role = useGroupRole(groupId);
  const tags = useQuery({
    queryKey: ['tags', groupId],
    queryFn: () => listTags(groupId),
    enabled: role !== 'VIEWER',
  });
  const [renaming, setRenaming] = useState<TagCount | null>(null);
  const [deleting, setDeleting] = useState<TagCount | null>(null);
  const countText = useCountText();

  if (role === 'VIEWER') {
    return (
      <>
        <PageHeader title={t('tags.title')} />
        <Notice tone="alert" title={t('tags.noPermission')} />
      </>
    );
  }
  return (
    <>
      <PageHeader title={t('tags.title')} />
      <p className="-mt-4 mb-6 text-text-muted">{t('tags.intro')}</p>
      {tags.isPending ? (
        <LoadingState rows={4} />
      ) : tags.isError ? (
        <ErrorState onRetry={() => void tags.refetch()} />
      ) : tags.data.length === 0 ? (
        <EmptyState title={t('tags.emptyTitle')} body={t('tags.emptyBody')} />
      ) : (
        <TagTable tags={tags.data} countText={countText} onRename={setRenaming} onDelete={setDeleting} />
      )}
      <RenameDialog key={renaming?.id} groupId={groupId} tag={renaming} onClose={() => setRenaming(null)} />
      <DeleteDialog
        groupId={groupId}
        tag={deleting}
        countText={countText}
        onClose={() => setDeleting(null)}
      />
    </>
  );
}

interface TagTableProps {
  tags: TagCount[];
  countText: (count: number) => string;
  onRename: (tag: TagCount) => void;
  onDelete: (tag: TagCount) => void;
}

function TagTable({ tags, onRename, onDelete }: TagTableProps) {
  const { t } = useTranslation();
  return (
    <>
      <table className="hidden w-full table-fixed border-collapse md:table">
        <caption className="sr-only">{t('tags.tableCaption')}</caption>
        <thead>
          <tr className="text-left">
            <th scope="col" className="border-b p-3 font-semibold">
              {t('tags.nameColumn')}
            </th>
            <th scope="col" className="w-32 border-b p-3 text-right font-semibold">
              {t('tags.countColumn')}
            </th>
            <th scope="col" className="w-64 border-b p-3 text-right font-semibold">
              {t('tags.actionsColumn')}
            </th>
          </tr>
        </thead>
        <tbody>
          {tags.map((tag) => (
            <tr key={tag.id}>
              <td className="truncate border-b p-3" title={tag.name}>
                {tag.name}
              </td>
              <td className="border-b p-3 text-right">{tag.contactCount}</td>
              <td className="border-b p-3 text-right">
                <TagActions tag={tag} onRename={onRename} onDelete={onDelete} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <ul className="flex flex-col gap-3 md:hidden" aria-label={t('tags.tableCaption')}>
        {tags.map((tag) => (
          <li key={tag.id} className="flex flex-col gap-3 rounded-[12px] border bg-surface-raised p-4">
            <p className="min-w-0 truncate font-semibold" title={tag.name}>
              {tag.name}
            </p>
            <p className="text-text-muted">{t('tags.countLine', { contacts: tag.contactCount })}</p>
            <TagActions tag={tag} onRename={onRename} onDelete={onDelete} />
          </li>
        ))}
      </ul>
    </>
  );
}

function TagActions({
  tag,
  onRename,
  onDelete,
}: Pick<TagTableProps, 'onRename' | 'onDelete'> & { tag: TagCount }) {
  const { t } = useTranslation();
  return (
    <span className="inline-flex gap-2">
      <Button
        variant="secondary"
        className="h-11"
        aria-label={t('tags.renameFor', { tag: tag.name })}
        onClick={() => onRename(tag)}
      >
        {t('tags.rename')}
      </Button>
      <Button
        variant="ghost"
        className="h-11"
        aria-label={t('tags.deleteFor', { tag: tag.name })}
        onClick={() => onDelete(tag)}
      >
        {t('tags.delete')}
      </Button>
    </span>
  );
}

function RenameDialog({
  groupId,
  tag,
  onClose,
}: {
  groupId: string;
  tag: TagCount | null;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const [name, setName] = useState(tag?.name ?? '');
  const [invalid, setInvalid] = useState(false);
  const rename = useMutation({
    mutationFn: (next: string) => renameTag(groupId, (tag as TagCount).id, next),
    onSuccess: async () => {
      toast.success(t('tags.renamed'));
      close();
      await queryClient.invalidateQueries({ queryKey: ['tags', groupId] });
    },
  });

  function close() {
    setInvalid(false);
    rename.reset();
    onClose();
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    const trimmed = name.trim();
    const broken = !TAG_NAME.test(trimmed);
    setInvalid(broken);
    if (!broken) rename.mutate(trimmed);
  }

  const serverCode = renameErrorCode(rename.error);
  const fieldCode = invalid ? 'validation.tag_invalid' : serverCode;
  const busy = rename.isPending;
  return (
    <Dialog open={tag !== null} onOpenChange={(open) => (open ? undefined : close())}>
      <DialogContent aria-describedby={undefined}>
        <DialogHeader>
          <DialogTitle>{t('tags.renameTitle')}</DialogTitle>
        </DialogHeader>
        {rename.isError && !serverCode ? <Notice tone="alert" title={errorMessage(t, rename.error)} /> : null}
        <form noValidate onSubmit={submit} className="flex flex-col gap-4">
          <FormField
            id="tag-name"
            label={t('tags.newName')}
            hint={t('tags.newNameHint')}
            required
            autoComplete="off"
            value={name}
            error={fieldCode ? t(errorKey(fieldCode)) : undefined}
            onChange={(e) => setName(e.target.value)}
          />
          <DialogFooter>
            <Button type="button" variant="ghost" className="h-11" onClick={close} disabled={busy}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" className="h-11" disabled={busy} aria-busy={busy || undefined}>
              {busy ? t('tags.renameBusy') : t('tags.rename')}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}

interface DeleteDialogProps {
  groupId: string;
  tag: TagCount | null;
  countText: (count: number) => string;
  onClose: () => void;
}

function DeleteDialog({ groupId, tag, countText, onClose }: DeleteDialogProps) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const remove = useMutation({
    mutationFn: () => deleteTag(groupId, (tag as TagCount).id),
    onSuccess: async () => {
      toast.success(t('tags.deleted'));
      onClose();
      await queryClient.invalidateQueries({ queryKey: ['tags', groupId] });
    },
    onError: () => toast.error(t('error.generic')),
  });
  return (
    <ConfirmDialog
      open={tag !== null}
      title={t('tags.deleteTitle', { tag: tag?.name ?? '' })}
      description={t('tags.deleteBody', { contacts: countText(tag?.contactCount ?? 0) })}
      confirmLabel={remove.isPending ? t('tags.deleteBusy') : t('tags.deleteConfirm')}
      destructive
      busy={remove.isPending}
      onConfirm={() => remove.mutate()}
      onCancel={onClose}
    />
  );
}
