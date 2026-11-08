import { Navigate, Outlet, useLocation } from 'react-router'
import PageLoading from '../components/common/PageLoading.jsx'
import ForbiddenPage from '../features/auth/pages/ForbiddenPage.jsx'
import { useAuthStore } from '../store/authStore.js'
import { hasAnyRole } from '../utils/roles.js'

export default function ProtectedRoute() {
  const status = useAuthStore((state) => state.status)
  const reason = useAuthStore((state) => state.reason)
  const { pathname, search } = useLocation()

  if (status === 'loading') return <PageLoading />
  if (status !== 'signedIn') {
    // After a deliberate sign-out the next person starts fresh, not on the last person's page.
    const state = reason === 'signedOut' ? undefined : { from: pathname + search }
    return <Navigate to="/login" replace state={state} />
  }
  return <Outlet />
}

// Keeps the URL, so the user can see what they asked for and why it is refused.
export function RequireRoles({ roles }) {
  const user = useAuthStore((state) => state.user)
  return hasAnyRole(user, roles) ? <Outlet /> : <ForbiddenPage />
}
