import { useState } from 'react'
import { useNavigate } from 'react-router'
import Button from '../../../components/common/Button.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { toApiError } from '../../../services/apiError.js'
import { useAuthStore } from '../../../store/authStore.js'
import { useToastStore } from '../../../store/toastStore.js'
import { fetchAuthOptions, fetchDemoUsers } from '../../auth/services/authApi.js'

const loadDemoUsers = async () => ((await fetchAuthOptions()).demoLoginEnabled ? fetchDemoUsers() : [])

// Demo builds only: become whoever the flow is waiting for. The server still refuses this outside the local profile.
export default function ContinueAsDemo({ match, to }) {
  const signIn = useAuthStore((state) => state.signIn)
  const toast = useToastStore((state) => state.push)
  const navigate = useNavigate()
  const { data: users } = useApi(loadDemoUsers)
  const [busy, setBusy] = useState(false)
  const target = users?.find(match)

  if (!target) return null

  const switchUser = async () => {
    setBusy(true)
    try {
      await signIn(target.id)
      navigate(to ?? 0) // Without a new page, reload: everything on screen was loaded for the previous person's role.
    } catch (failure) {
      toast({ tone: 'danger', message: toApiError(failure).message })
      setBusy(false)
    }
  }

  return (
    <Button variant="secondary" loading={busy} onClick={switchUser}>
      Continue as {target.fullName}
    </Button>
  )
}
