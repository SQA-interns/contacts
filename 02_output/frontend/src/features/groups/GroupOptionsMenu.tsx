import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { DeleteGroupDialog } from './DeleteGroupDialog';

interface GroupOptionsMenuProps {
  group: { id: string; name: string };
  /** The button that opens the menu. */
  trigger: ReactNode;
  /** Items above "Izbriši skupino". */
  children?: ReactNode;
  onDeleted?: () => void;
}

/** The "Možnosti skupine" menu of an Owner: delete the Group (WF-05 card menu, WF-06; R-020-01, R-020-06). */
export function GroupOptionsMenu({ group, trigger, children, onDeleted }: GroupOptionsMenuProps) {
  const { t } = useTranslation();
  const [deleting, setDeleting] = useState(false);
  return (
    <>
      {/* Not modal: a modal menu hides the page from assistive technology while the page stays focusable, and a dialog opened from it would leave the page blocked (D-62). */}
      <DropdownMenu modal={false}>
        <DropdownMenuTrigger asChild>{trigger}</DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          {children}
          {children ? <DropdownMenuSeparator /> : null}
          <DropdownMenuItem className="min-h-11" onSelect={() => setDeleting(true)}>
            {t('deleteGroup.submit')}
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
      <DeleteGroupDialog
        group={deleting ? group : undefined}
        onClose={() => setDeleting(false)}
        onDeleted={onDeleted}
      />
    </>
  );
}
