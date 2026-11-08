import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import Dialog from './Dialog.jsx'
import Dropdown from './Dropdown.jsx'
import FormField from './FormField.jsx'
import Input from './Input.jsx'
import StatusBadge from './StatusBadge.jsx'
import Tabs from './Tabs.jsx'

describe('Dialog', () => {
  it('opens as a labelled modal and closes from its button', async () => {
    const onClose = vi.fn()
    render(
      <Dialog open onClose={onClose} title="Add hall">
        Body
      </Dialog>,
    )

    expect(screen.getByRole('dialog', { name: 'Add hall' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Close' }))
    expect(onClose).toHaveBeenCalled()
  })
})

describe('Dropdown', () => {
  const items = [
    { label: 'Edit', onSelect: vi.fn() },
    { label: 'Lock', onSelect: vi.fn() },
    { label: 'Delete', onSelect: vi.fn() },
  ]
  const item = (name) => screen.getByRole('menuitem', { name, hidden: true })

  // Arrow keys are tested in a real browser, because jsdom cannot focus inside a closed popover.
  it('runs the chosen item and returns focus to the trigger', async () => {
    render(<Dropdown label="Actions" items={items} />)

    fireEvent.click(item('Lock'))

    expect(items[1].onSelect).toHaveBeenCalled()
    expect(screen.getByRole('button', { name: 'Actions' })).toHaveFocus()
  })
})

describe('Tabs', () => {
  const tabs = [
    { id: 'one', label: 'One' },
    { id: 'two', label: 'Two' },
  ]

  it('marks the selected tab and links it to the panel', () => {
    render(
      <Tabs label="Sections" tabs={tabs} value="two" onChange={() => {}}>
        Content
      </Tabs>,
    )

    expect(screen.getByRole('tab', { name: 'Two' })).toHaveAttribute('aria-selected', 'true')
    expect(screen.getByRole('tabpanel', { name: 'Two' })).toHaveTextContent('Content')
  })

  it('changes tab with the arrow keys', async () => {
    const onChange = vi.fn()
    render(
      <Tabs label="Sections" tabs={tabs} value="one" onChange={onChange}>
        Content
      </Tabs>,
    )

    fireEvent.keyDown(screen.getByRole('tablist'), { key: 'ArrowRight' })
    expect(onChange).toHaveBeenCalledWith('two')

    fireEvent.keyDown(screen.getByRole('tablist'), { key: 'ArrowLeft' })
    expect(onChange).toHaveBeenLastCalledWith('two')
  })
})

describe('FormField', () => {
  it('ties the label, hint and error to the control', () => {
    render(
      <FormField label="Capacity" hint="Seats only" error="Must be above 0" required>
        <Input />
      </FormField>,
    )

    const input = screen.getByLabelText(/Capacity/)
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(input).toHaveAttribute('aria-required', 'true')
    expect(input).toHaveAccessibleDescription('Seats only Must be above 0')
  })
})

describe('StatusBadge', () => {
  it('always shows text, never colour alone', () => {
    render(<StatusBadge status="PUBLISHED" />)
    expect(screen.getByText('Published')).toBeInTheDocument()
  })

  it('shows the raw value for an unknown status', () => {
    render(<StatusBadge status="SOMETHING_NEW" />)
    expect(screen.getByText('SOMETHING_NEW')).toBeInTheDocument()
  })
})
