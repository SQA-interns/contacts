import { useQuery } from '@tanstack/react-query';
import { listGroups, type Role } from '@/api/groups';

/** The Role of the signed-in User in a Group, from the Group list; `undefined` while it loads. */
export function useGroupRole(groupId: string | undefined): Role | undefined {
  const groups = useQuery({ queryKey: ['groups'], queryFn: listGroups });
  return groups.data?.find((group) => group.id === groupId)?.role;
}
