import Spinner from './Spinner.jsx'

export default function PageLoading() {
  return (
    <div className="grid min-h-dvh place-items-center text-ink-tertiary">
      <Spinner size={24} />
    </div>
  )
}
