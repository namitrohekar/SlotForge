// A ruled figure, not a card: the number carries the weight.
export default function StatTile({ label, value, context }) {
  return (
    <div className="min-w-0 border-l border-line pl-4">
      <p className="text-footnote text-ink-secondary">{label}</p>
      <p data-numeric className="mt-1 font-serif text-title-1 font-medium">
        {value}
      </p>
      {context && <p className="mt-0.5 text-footnote text-ink-tertiary">{context}</p>}
    </div>
  )
}
