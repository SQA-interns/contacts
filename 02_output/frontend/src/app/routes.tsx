import { Navigate, type RouteObject } from 'react-router';
import { RequireVerified } from '@/auth/RequireVerified';
import { RegisterPage } from '@/features/auth/RegisterPage';
import { ResetPasswordPage } from '@/features/auth/ResetPasswordPage';
import { SignInPage } from '@/features/auth/SignInPage';
import { VerifyEmailPage } from '@/features/auth/VerifyEmailPage';
import { GroupListPage } from '@/features/groups/GroupListPage';
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
      { element: <RequireVerified />, children: [{ path: 'groups', element: <GroupListPage /> }] },
      { path: '*', element: <NotFoundPage /> },
    ],
  },
];
