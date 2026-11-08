import { useEffect } from 'react'
import { RouterProvider } from 'react-router'
import Toaster from './components/common/Toaster.jsx'
import { useThemeSync } from './hooks/useThemeSync.js'
import { router } from './routes/router.jsx'
import { useAuthStore } from './store/authStore.js'

function App() {
  useThemeSync()

  useEffect(() => {
    useAuthStore.getState().load()
  }, [])

  return (
    <>
      <RouterProvider router={router} />
      <Toaster />
    </>
  )
}

export default App
