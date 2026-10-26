import { Navigate, useLocation } from 'react-router';
import { useAuth } from '../auth.js';

/** Wraps pages that need an account: signed-out visitors go to the login page, then come back. */
export default function RequireAuth({ children }) {
  const session = useAuth();
  const location = useLocation();
  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return children;
}
