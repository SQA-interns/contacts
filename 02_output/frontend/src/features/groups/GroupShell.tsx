import { useQuery } from '@tanstack/react-query';
import { ChevronDown } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { Link, NavLink, Outlet, useLocation, useNavigate, useParams } from 'react-router';
import { listGroups, type Role } from '@/api/groups';
import { Button } from '@/components/ui/button';
import { GroupOptionsMenu } from './GroupOptionsMenu';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';

interface GroupTab {
  /** Path below `/groups/:groupId`. */
  path: string;
  /** Key below `groupNav` in the message catalog. */
  key: string;
  /** Roles that may open the screen (R-006-01, R-012-08, R-014-10, R-015-01, R-017-08). */
  roles: Role[];
}

const TABS: GroupTab[] = [
  { path: 'contacts', key: 'contacts', roles: ['OWNER', 'EDITOR', 'VIEWER'] },
  { path: 'tags', key: 'tags', roles: ['OWNER', 'EDITOR'] },
  { path: 'trash', key: 'trash', roles: ['OWNER', 'EDITOR'] },
  { path: 'activity', key: 'activity', roles: ['OWNER', 'EDITOR'] },
  { path: 'members', key: 'members', roles: ['OWNER', 'EDITOR', 'VIEWER'] },
  { path: 'invitations', key: 'invitations', roles: ['OWNER'] },
];

/**
 * Shell of every Group screen: the Group bar with name and Role, the tab bar of the Group screens and, on a phone, a
 * menu in its place (UI-22, UI-23; WF-06..WF-13). The screens of the Group are its children.
 */
export function GroupShell() {
  const { t } = useTranslation();
  const { groupId = '' } = useParams();
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const groups = useQuery({ queryKey: ['groups'], queryFn: listGroups });
  const group = groups.data?.find((g) => g.id === groupId);
  const base = `/groups/${encodeURIComponent(groupId)}`;
  const tabs = group ? TABS.filter((tab) => tab.roles.includes(group.role)) : [];
  const isCurrent = (tab: GroupTab) => pathname.startsWith(`${base}/${tab.path}`);
  const current = tabs.find(isCurrent);

  return (
    <>
      {group ? (
        <>
          <div className="mb-3 flex flex-wrap items-center gap-x-4 gap-y-2">
            <span className="text-xl font-semibold break-words">{group.name}</span>
            <span className="inline-flex min-h-6 items-center rounded-full border bg-surface px-2 text-xs font-semibold">
              {t(`role.${group.role}`)}
            </span>
            {group.role === 'OWNER' ? (
              <div className="ml-auto">
                <GroupOptionsMenu
                  group={group}
                  onDeleted={() => void navigate('/groups')}
                  trigger={
                    <Button variant="ghost" className="h-11">
                      {t('groups.options')}
                      <ChevronDown aria-hidden="true" />
                    </Button>
                  }
                />
              </div>
            ) : null}
          </div>
          <nav
            aria-label={t('groupNav.label')}
            className="mb-6 hidden gap-1 overflow-x-auto border-b md:flex"
          >
            {tabs.map((tab) => (
              <NavLink
                key={tab.key}
                to={`${base}/${tab.path}`}
                aria-current={isCurrent(tab) ? 'page' : undefined}
                className="min-h-11 content-center rounded-t-[8px] border border-b-0 border-transparent px-4 whitespace-nowrap aria-[current=page]:border-border aria-[current=page]:bg-surface aria-[current=page]:font-semibold"
              >
                {t(`groupNav.${tab.key}`)}
              </NavLink>
            ))}
          </nav>
          <div className="mb-4 md:hidden">
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" className="h-11 w-full justify-between">
                  {t('groupNav.menu', { current: current ? t(`groupNav.${current.key}`) : group.name })}
                  <ChevronDown aria-hidden="true" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="start" className="min-w-(--radix-dropdown-menu-trigger-width)">
                {tabs.map((tab) => (
                  <DropdownMenuItem key={tab.key} asChild className="min-h-11">
                    <Link to={`${base}/${tab.path}`}>{t(`groupNav.${tab.key}`)}</Link>
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </>
      ) : null}
      <Outlet />
    </>
  );
}
