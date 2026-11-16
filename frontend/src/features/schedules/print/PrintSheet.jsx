import { useEffect } from 'react'
import { ArrowLeft, Printer } from 'lucide-react'
import { Link } from 'react-router'
import Button from '../../../components/common/Button.jsx'

const WIDTH = 'mx-auto max-w-[52rem] px-4'

// A sheet of paper on screen. Always light, so it prints the same from either theme.
export default function PrintSheet({ title, session, note, back, children }) {
  useEffect(() => {
    document.title = `${title} · SlotForge`
  }, [title])

  return (
    <div data-theme="light" className="min-h-dvh bg-raised text-ink">
      <div className={`${WIDTH} flex items-center justify-between gap-4 py-4 print:hidden`}>
        <Link to={back} className="inline-flex items-center gap-1.5 rounded-sm text-callout text-ink-secondary hover:text-ink">
          <ArrowLeft size={16} aria-hidden="true" />
          Back
        </Link>
        <Button icon={Printer} onClick={() => window.print()}>
          Print
        </Button>
      </div>
      <main className={`${WIDTH} pb-16 print:max-w-none print:px-0 print:pb-0`}>
        <header className="mb-6 border-b border-ink pb-3">
          <p className="text-footnote text-ink-secondary">Veermata Jijabai Technological Institute, Mumbai</p>
          <h1 className="mt-1 text-title-1">{session}</h1>
          <p className="mt-0.5 text-callout font-medium text-ink">{title}</p>
          {note && <p className="mt-0.5 text-footnote text-ink-secondary">{note}</p>}
        </header>
        {children}
      </main>
    </div>
  )
}

// One ruled table for every printed sheet. A row is never split across two pages.
export function PrintTable({ caption, headers, children }) {
  return (
    <table className="w-full border-collapse text-left text-footnote">
      <caption className="sr-only">{caption}</caption>
      <thead>
        <tr className="border-b border-ink text-caption text-ink-secondary">
          {headers.map((header) => (
            <th key={header} scope="col" className="py-1.5 pr-3 font-normal last:pr-0">
              {header}
            </th>
          ))}
        </tr>
      </thead>
      <tbody className="[&>tr]:break-inside-avoid [&>tr]:border-b [&>tr]:border-line [&_td]:py-1.5 [&_td]:pr-3 [&_td]:align-top">{children}</tbody>
    </table>
  )
}
