import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate, useParams } from 'react-router';
import { toast } from 'sonner';
import { createContact, type AttributePair } from '@/api/contacts';
import { ApiError } from '@/api/errors';
import { PageHeader } from '@/components/PageHeader';
import { ContactForm, type ContactValues } from './ContactForm';

/** SC-08 / WF-08: a new Contact in the Group (R-003-01..05). */
export function NewContactPage() {
  const { t } = useTranslation();
  const { groupId = '' } = useParams();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const create = useMutation({
    mutationFn: (input: { values: ContactValues; attributes: AttributePair[]; tags: string[] }) =>
      createContact(groupId, { ...input.values, attributes: input.attributes, tags: input.tags }),
    onSuccess: async (contact) => {
      toast.success(t('contacts.created'));
      await queryClient.invalidateQueries({ queryKey: ['contacts', groupId] });
      await navigate(`/groups/${groupId}/contacts/${contact.id}`);
    },
  });
  const error =
    create.error instanceof ApiError
      ? create.error
      : create.isError
        ? new ApiError(0, 'server.error', '')
        : null;

  return (
    <>
      <Link
        to={`/groups/${groupId}/contacts`}
        className="mb-2 inline-block underline-offset-4 hover:underline"
      >
        {t('contacts.back')}
      </Link>
      <PageHeader title={t('contacts.newTitle')} />
      <ContactForm
        groupId={groupId}
        submitLabel={t('contacts.save')}
        busyLabel={t('contacts.saving')}
        busy={create.isPending}
        error={error}
        onSubmit={(values, attributes, tags) => create.mutate({ values, attributes, tags })}
        onCancel={() => void navigate(`/groups/${groupId}/contacts`)}
      />
    </>
  );
}
