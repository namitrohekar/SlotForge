import { useEffect, useState } from 'react'
import { Navigate, useLocation, useSearchParams } from 'react-router'
import Alert from '../../../components/common/Alert.jsx'
import ErrorState from '../../../components/common/ErrorState.jsx'
import LinkButton from '../../../components/common/LinkButton.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import ThemeToggle from '../../../components/common/ThemeToggle.jsx'
import Brand from '../../../components/layout/Brand.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { toApiError } from '../../../services/apiError.js'
import { useAuthStore } from '../../../store/authStore.js'
import DemoUserPicker from '../components/DemoUserPicker.jsx'
import { fetchAuthOptions, fetchDemoUsers } from '../services/authApi.js'

// Options first: that call also gives the browser its CSRF cookie before any sign-in POST.
async function loadSignIn() {
  const options = await fetchAuthOptions()
  const users = options.demoLoginEnabled ? await fetchDemoUsers() : []
  return { options, users }
}

const notices = {
  expired: ['info', 'Your session ended. Sign in again to continue.'],
  deactivated: ['danger', 'This account has been deactivated. Ask an administrator for help.'],
}

const GOOGLE_FAILED = 'Google sign-in did not finish. Try again.'
const googleRefusals = {
  DOMAIN_NOT_ALLOWED: 'Use your VJTI Google account. Other accounts cannot sign in.',
  EMAIL_NOT_VERIFIED: 'Google has not verified this email address',
  NOT_REGISTERED: 'This account is not registered in SlotForge. Ask the exam office to add you.',
  ACCOUNT_DEACTIVATED: 'This account has been deactivated. Ask an administrator for help.',
  ACCOUNT_CONFLICT: 'This email is linked to a different Google account. Ask an administrator for help.',
}

export default function LoginPage() {
  const status = useAuthStore((state) => state.status)
  const reason = useAuthStore((state) => state.reason)
  const signIn = useAuthStore((state) => state.signIn)
  const { state } = useLocation()
  const [params] = useSearchParams()
  const googleError = params.get('error')
  const { data, error, loading, reload } = useApi(loadSignIn)
  const [pendingId, setPendingId] = useState(null)
  const [signInError, setSignInError] = useState(null)

  useEffect(() => {
    document.title = 'Sign in · SlotForge'
  }, [])

  if (status === 'signedIn') return <Navigate to={state?.from ?? '/'} replace />

  const pick = async (userId) => {
    setPendingId(userId)
    setSignInError(null)
    try {
      await signIn(userId)
    } catch (failure) {
      setSignInError(toApiError(failure).message)
      setPendingId(null)
    }
  }

  const notice = notices[reason]

  return (
    <div className="flex min-h-dvh flex-col px-4 pt-[max(1rem,env(safe-area-inset-top))] pb-8">
      <header className="flex items-center justify-between">
        <Brand />
        <ThemeToggle />
      </header>

      <main className="sf-page mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-12">
        <h1 className="text-display">Sign in</h1>
        <p className="mt-2 text-body text-ink-secondary">Exam scheduling for VJTI.</p>

        <div className="mt-8 space-y-4">
          {notice && <Alert tone={notice[0]}>{notice[1]}</Alert>}
          {signInError && <Alert tone="danger">{signInError}</Alert>}
          {googleError && <Alert tone="danger">{googleRefusals[googleError] ?? GOOGLE_FAILED}</Alert>}

          {loading && <Skeleton className="h-90 w-full rounded-lg" />}
          {error && <ErrorState title="Sign-in is not available" message={error.message} onRetry={reload} />}
          {data?.options.googleEnabled && (
            <LinkButton to="/oauth2/authorization/google" reloadDocument className="w-full">
              Continue with Google
            </LinkButton>
          )}
          {data && data.users.length > 0 && (
            <>
              <h2 className="text-footnote text-ink-tertiary">Choose a demo account to explore SlotForge as that role</h2>
              <DemoUserPicker users={data.users} pendingId={pendingId} onPick={pick} />
            </>
          )}
          {data && data.users.length === 0 && !data.options.googleEnabled && (
            <Alert tone="warning" title="No sign-in method is set up">
              Start the backend with the local profile to use demo accounts
            </Alert>
          )}
        </div>
      </main>
    </div>
  )
}
