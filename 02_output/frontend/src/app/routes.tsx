import type { RouteObject } from 'react-router';
import { RequireVerified } from '@/auth/RequireVerified';
import { SignInPage } from '@/features/auth/SignInPage';
import { GroupListPage } from '@/features/groups/GroupListPage';
import { HomePage } from '@/features/home/HomePage';
import { NotFoundPage } from '@/features/home/NotFoundPage';
import { Layout } from './Layout';

/** Route table (ADR-12). Feature routes are added by their stories. */
export const routes: RouteObject[] = [
  {
    path: '/',
    element: <Layout />,
    children: [
      { index: true, element: <HomePage /> },
      { path: 'sign-in', element: <SignInPage /> },
      { element: <RequireVerified />, children: [{ path: 'groups', element: <GroupListPage /> }] },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
