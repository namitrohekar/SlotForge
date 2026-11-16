import { useState } from 'react'
import Alert from './Alert.jsx'
import Button from './Button.jsx'
import Dialog from './Dialog.jsx'
import FormField from './FormField.jsx'
import { toApiError } from '../../services/apiError.js'

const cell = (value) => (/[",\n]/.test(value) ? `"${value.replaceAll('"', '""')}"` : value)

// Upload a spreadsheet saved as CSV. Nothing is saved unless every row is good, and each bad row is named by its line.
export default function CsvImportDialog({ title, description, columns, sample, upload, onImported, onClose }) {
  const [file, setFile] = useState(null)
  const [busy, setBusy] = useState(false)
  const [failure, setFailure] = useState(null)
  const [report, setReport] = useState(null)

  const sampleFile = `data:text/csv;charset=utf-8,${encodeURIComponent([columns, ...sample].map((row) => row.map(cell).join(',')).join('\n'))}`

  const submit = async (event) => {
    event.preventDefault()
    if (!file) return setFailure('Choose a CSV file first')
    setBusy(true)
    setFailure(null)
    setReport(null)
    try {
      const result = await upload(file)
      setReport(result)
      if (result.errorCount === 0) onImported(result)
    } catch (problem) {
      setFailure(toApiError(problem).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <Dialog
      open
      onClose={onClose}
      title={title}
      description={description}
      footer={
        <>
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            {report?.errorCount === 0 ? 'Done' : 'Cancel'}
          </Button>
          <Button type="submit" form="csv-import" loading={busy}>
            Import
          </Button>
        </>
      }
    >
      <form id="csv-import" noValidate onSubmit={submit} className="space-y-5">
        {failure && <Alert tone="danger">{failure}</Alert>}
        {report?.errorCount === 0 && (
          <Alert title={`${report.imported} imported from ${report.rows} ${report.rows === 1 ? 'row' : 'rows'}`}>
            {report.imported < report.rows ? 'The rest were already there.' : null}
          </Alert>
        )}
        {report?.errorCount > 0 && (
          <Alert tone="danger" title={`Nothing was imported. ${report.errorCount} ${report.errorCount === 1 ? 'row needs' : 'rows need'} fixing`}>
            <ul className="mt-1 space-y-0.5">
              {report.errors.map((error) => (
                <li key={`${error.line}-${error.message}`}>
                  <span data-numeric>Line {error.line}</span>: {error.message}
                </li>
              ))}
            </ul>
            {report.errorCount > report.errors.length && <p className="mt-1">And {report.errorCount - report.errors.length} more.</p>}
          </Alert>
        )}
        <FormField label="CSV file" required hint={`Columns: ${columns.join(', ')}`}>
          <input
            type="file"
            accept=".csv,text/csv"
            onChange={(event) => setFile(event.target.files[0] ?? null)}
            className="block w-full rounded-md text-callout text-ink-secondary file:mr-3 file:h-control file:cursor-pointer file:rounded-md file:border file:border-solid file:border-line-strong file:bg-surface file:px-3 file:text-callout file:text-ink"
          />
        </FormField>
        <a href={sampleFile} download="sample.csv" className="inline-block rounded-sm text-callout text-ink underline decoration-line-strong underline-offset-2 hover:decoration-ink">
          Download a sample file
        </a>
      </form>
    </Dialog>
  )
}
