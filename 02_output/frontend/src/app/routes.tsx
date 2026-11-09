import { Navigate, type RouteObject } from 'react-router';
import { RequireVerified } from '@/auth/RequireVerified';
import { RegisterPage } from '@/features/auth/RegisterPage';
import { ResetPasswordPage } from '@/features/auth/ResetPasswordPage';
import { SignInPage } from '@/features/auth/SignInPage';
import { VerifyEmailPage } from '@/features/auth/VerifyEmailPage';
import { ContactPage } from '@/features/contacts/ContactPage';
import { NewContactPage } from '@/features/contacts/NewContactPage';
import { GroupListPage } from '@/features/groups/GroupListPage';
import { GroupShell } from '@/features/groups/GroupShell';
import { GroupTrashPage } from '@/features/groups/GroupTrashPage';
import { ContactListPage } from '@/features/contacts/ContactListPage';
import { ActivityPage } from '@/features/activity/ActivityPage';
import { MembersPage } from '@/features/members/MembersPage';
import { InvitationsPage } from '@/features/invitations/InvitationsPage';
import { TrashPage } from '@/features/trash/TrashPage';
import { TagListPage } from '@/features/tags/TagListPage';
import { NotFoundPage } from '@/features/home/NotFoundPage';
import { Layout } from './Layout';

/** Route table (ADR-12, screen inventory of the specification). Feature routes are added by their stories. */
export const routes: RouteObject[] = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <Navigate to="/groups" replace /> },
      { path: 'sign-in', element: <SignInPage /> },
      { path: 'register', element: <RegisterPage /> },
      { path: 'reset-password', element: <ResetPasswordPage /> },
      { path: 'verify-email', element: <VerifyEmailPage /> },
      {
        element: <RequireVerified />,
        children: [
          { path: 'groups', element: <GroupListPage /> },
          { path: 'group-trash', element: <GroupTrashPage /> },
          {
            path: 'groups/:groupId',
            element: <GroupShell />,
            children: [
              { path: 'contacts', element: <ContactListPage /> },
              { path: 'tags', element: <TagListPage /> },
              { path: 'trash', element: <TrashPage /> },
              { path: 'invitations', element: <InvitationsPage /> },
              { path: 'members', element: <MembersPage /> },
              { path: 'activity', element: <ActivityPage /> },
              { path: 'contacts/new', element: <NewContactPage /> },
              { path: 'contacts/:contactId', element: <ContactPage /> },
            ],
          },
        ],
      },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
