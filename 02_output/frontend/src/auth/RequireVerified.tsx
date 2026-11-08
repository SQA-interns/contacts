import { Navigate, Outlet } from 'react-router';
import { LoadingState } from '@/components/LoadingState';
import { useAuth } from './AuthProvider';

/** Pages behind it need a signed-in User with a verified e-mail (R-001-03, R-001-04, R-001-10). */
export function RequireVerified() {
  const { status, account } = useAuth();
  if (status === 'loading') return <LoadingState />;
  if (!account) return <Navigate to="/sign-in" replace />;
  if (!account.emailVerified) return <Navigate to="/verify-email" replace />;
  return <Outlet />;
}
