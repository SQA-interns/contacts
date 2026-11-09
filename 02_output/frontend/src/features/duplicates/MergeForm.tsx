import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router';
import type { Contact, ContactField } from '@/api/contacts';
import { mergeContacts } from '@/api/duplicates';
import { ApiError, errorKey, errorMessage } from '@/api/errors';
import i18n from '@/i18n';
import { ConfirmDialog } from '@/components/ConfirmDialog';
import { Notice } from '@/components/Notice';
import { Button } from '@/components/ui/button';
import { MergeTable, type Choices } from './MergeTable';
import {
  attributeRows,
  combinedTags,
  fieldRows,
  mergeRequest,
  missingChoices,
  orderForMerge,
} from './preselect';

/** The form of the Merge screen: nothing is merged until the user has chosen every value and confirmed (R-026-05, R-026-10). */
export function MergeForm({ groupId, contacts }: { groupId: string; contacts: Contact[] }) {
  const { t, i18n: active } = useTranslation();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const ordered = useMemo(() => orderForMerge(contacts), [contacts]);
  const fields = useMemo(() => fieldRows(ordered), [ordered]);
  const attributes = useMemo(() => attributeRows(ordered), [ordered]);
  const [choices, setChoices] = useState<Choices>(() => ({
    fields: Object.fromEntries(fields.map((row) => [row.field, row.preselected])),
    attributes: {},
  }));
  const [missing, setMissing] = useState<string[]>([]);
  const [confirming, setConfirming] = useState(false);
  const duplicatesPath = `/groups/${encodeURIComponent(groupId)}/duplicates`;
  const contactsPath = `/groups/${encodeURIComponent(groupId)}/contacts`;

  const merge = useMutation({
    mutationFn: () =>
      mergeContacts(groupId, mergeRequest(ordered, choices.fields, attributes, choices.attributes)),
    onSuccess: async () => {
      await Promise.all(
        ['duplicates', 'contacts', 'trash', 'tags', 'facets'].map((key) =>
          queryClient.invalidateQueries({ queryKey: [key, groupId] }),
        ),
      );
    },
    onError: () => setConfirming(false),
  });

  const ask = () => {
    const open = missingChoices(attributes, choices.attributes);
    setMissing(open);
    if (open.length === 0) setConfirming(true);
  };
  const choose = (field: ContactField, value: string | null) =>
    setChoices((before) => ({ ...before, fields: { ...before.fields, [field]: value } }));
  const chooseAttribute = (key: string, value: string) => {
    setChoices((before) => ({ ...before, attributes: { ...before.attributes, [key.toLowerCase()]: value } }));
    setMissing((before) => before.filter((open) => open !== key));
  };
  if (merge.isSuccess) {
    return (
      <Notice tone="status" title={t('merge.merged')}>
        <div className="flex flex-wrap gap-3">
          <Button
            className="h-11"
            onClick={() => void navigate(`${contactsPath}/${encodeURIComponent(merge.data.id)}`)}
          >
            {t('merge.openContact')}
          </Button>
          <Link to={duplicatesPath} className="inline-flex min-h-11 items-center rounded-[8px] border px-4">
            {t('merge.back')}
          </Link>
        </div>
      </Notice>
    );
  }
  const count = ordered.length;
  const form = new Intl.PluralRules(active.language).select(count);
  const plural = form === 'one' || form === 'two' || form === 'few' ? form : 'other';

  return (
    <div className="flex flex-col gap-4">
      {merge.error ? <Refusal error={merge.error} /> : null}
      <MergeTable
        contacts={ordered}
        fieldRows={fields}
        attributeRows={attributes}
        tags={ordered.map((contact) => combinedTagsOf(contact))}
        choices={choices}
        missing={missing}
        onField={choose}
        onAttribute={chooseAttribute}
      />
      <p className="text-sm text-text-muted">{t('merge.note', { tags: combinedTags(ordered).length })}</p>
      <div className="sticky bottom-0 flex justify-end gap-3 border-t bg-surface py-3">
        <Button variant="ghost" className="h-11" onClick={() => void navigate(duplicatesPath)}>
          {t('common.cancel')}
        </Button>
        <Button className="h-11" onClick={ask} disabled={merge.isPending}>
          {t('merge.submit')}
        </Button>
      </div>
      <ConfirmDialog
        open={confirming}
        title={t(`merge.confirmTitle.${plural}`, { count })}
        description={t(`merge.confirmBody.${plural}`)}
        confirmLabel={merge.isPending ? t('merge.busy') : t('merge.submit')}
        busy={merge.isPending}
        onConfirm={() => merge.mutate()}
        onCancel={() => setConfirming(false)}
      />
    </div>
  );
}

const combinedTagsOf = (contact: Contact) => combinedTags([contact]);

/** Why the merge was refused (R-026-06, R-026-08); nothing was merged. */
function Refusal({ error }: { error: unknown }) {
  const { t } = useTranslation();
  if (!(error instanceof ApiError)) return <Notice tone="alert" title={errorMessage(t, error)} />;
  if (error.code === 'validation.failed') {
    const reasons = [...new Set(error.fieldErrors.map((fieldError) => fieldError.code))].filter((code) =>
      i18n.exists(errorKey(code)),
    );
    return (
      <Notice tone="alert" title={t('merge.invalidTitle')}>
        {reasons.map((code) => (
          <p key={code}>{t(errorKey(code))}</p>
        ))}
      </Notice>
    );
  }
  return (
    <Notice tone="alert" title={errorMessage(t, error)}>
      {error.code === 'contact.changed_by_someone_else' ? <p>{t('merge.changedNote')}</p> : null}
    </Notice>
  );
}
