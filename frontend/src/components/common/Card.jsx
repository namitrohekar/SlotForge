export default function Card({ title, action, className = '', children }) {
  return (
    <section className={`border-t border-line ${className}`}>
      {(title || action) && (
        <header className="flex min-h-13 flex-wrap items-center justify-between gap-x-4 gap-y-2 border-b border-line py-3">
          <h2 className="text-title-3">{title}</h2>
          {action}
        </header>
      )}
      <div className="py-5">{children}</div>
    </section>
  )
}
