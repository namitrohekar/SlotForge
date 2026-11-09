import LinkButton from '../../../components/common/LinkButton.jsx'

export default function NextStepBanner({ next, base = '.' }) {
  return (
    <div className="mb-6 flex flex-wrap items-center justify-between gap-x-4 gap-y-2 border-b border-line pb-4">
      <p className="text-callout text-ink">{next.message}</p>
      <LinkButton to={`${base}/${next.to}`}>{next.action}</LinkButton>
    </div>
  )
}
