import { useCallback, useState } from 'react'
import HeavyBadge from '../../../components/common/HeavyBadge.jsx'
import Button from '../../../components/common/Button.jsx'
import Checkbox from '../../../components/common/Checkbox.jsx'
import ChoiceList from '../../../components/common/ChoiceList.jsx'
import Dialog from '../../../components/common/Dialog.jsx'
import SearchInput from '../../../components/common/SearchInput.jsx'
import Skeleton from '../../../components/common/Skeleton.jsx'
import { useApi } from '../../../hooks/useApi.js'
import { listSubjects } from '../services/academicsApi.js'

// Search, choose one subject, say whether it is an elective. Subjects already placed are left out.
export default function PlacementDialog({ semester, placedIds, onClose, onAdd }) {
  const [q, setQ] = useState('')
  const [chosen, setChosen] = useState(null)
  const [elective, setElective] = useState(false)
  const load = useCallback(() => listSubjects({ q, size: 8, sort: 'code,asc' }), [q])
  const { data, error, loading } = useApi(load)
  const options = (data?.items ?? []).filter((subject) => !placedIds.includes(subject.id))

  return (
    <Dialog
      open
      onClose={onClose}
      title={`Add to semester ${semester}`}
      description="Search by code or name. A subject appears once in a curriculum."
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button disabled={!chosen} onClick={() => onAdd(chosen, elective)}>
            Add subject
          </Button>
        </>
      }
    >
      <div className="space-y-4">
        <SearchInput label="Search subjects" value={q} onSearch={setQ} />
        <fieldset>
          <legend className="sr-only">Subject</legend>
          {loading && !data && <Skeleton className="h-40 w-full rounded-md" />}
          {error && <p className="text-callout text-danger">{error.message}</p>}
          {data && options.length === 0 && (
            <p className="py-6 text-center text-callout text-ink-tertiary">
              No subject matches. Add it under Subjects first.
            </p>
          )}
          {options.length > 0 && (
            <ChoiceList
              type="radio"
              name="subject"
              options={options.map((subject) => ({
                value: subject.id,
                label: subject.name,
                detail: subject.code,
                accessory: subject.heavy ? <HeavyBadge /> : null,
              }))}
              isChosen={(id) => chosen?.id === id}
              onToggle={(id) => setChosen(options.find((subject) => subject.id === id))}
            />
          )}
        </fieldset>
        <Checkbox
          label="Elective, so only students who choose it sit the paper"
          checked={elective}
          onChange={(event) => setElective(event.target.checked)}
        />
      </div>
    </Dialog>
  )
}
