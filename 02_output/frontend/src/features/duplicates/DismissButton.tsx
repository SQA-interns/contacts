import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { toast } from 'sonner';
import { dismissDuplicate, type DuplicateSet } from '@/api/duplicates';
import { errorMessage } from '@/api/errors';
import { Button } from '@/components/ui/button';

/**
 * The button "Ni podvojeno" of WF-17 (R-025-01): stores the set as dismissed and drops it from the
 * list. A set that is gone (404) is reported and the list is searched again.
 */
export function DismissButton({ groupId, set }: { groupId: string; set: DuplicateSet }) {
  const { t } = useTranslation();
  const queryClient = useQueryClient();
  const listKey = ['duplicates', groupId];
  const dismiss = useMutation({
    mutationFn: () =>
      dismissDuplicate(
        groupId,
        set.contacts.map((contact) => contact.id),
      ),
    onSuccess: async () => {
      queryClient.setQueryData<DuplicateSet[]>(listKey, (sets) =>
        sets?.filter((other) => other.key !== set.key),
      );
      toast.success(t('duplicates.dismissed'));
      await queryClient.invalidateQueries({ queryKey: listKey });
    },
    onError: async (error) => {
      toast.error(errorMessage(t, error));
      await queryClient.invalidateQueries({ queryKey: listKey });
    },
  });

  return (
    <Button variant="ghost" className="h-11" disabled={dismiss.isPending} onClick={() => dismiss.mutate()}>
      {dismiss.isPending ? t('duplicates.dismissBusy') : t('duplicates.dismiss')}
    </Button>
  );
}
