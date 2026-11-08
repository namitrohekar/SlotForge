import { useState } from 'react'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { wholeNumberBetween } from '../../utils/validators.js'
import NumberInput from './NumberInput.jsx'

function Field() {
  const [value, setValue] = useState('')
  return <NumberInput aria-label="Seats" value={value} onChange={(event) => setValue(event.target.value)} />
}

describe('NumberInput', () => {
  it('accepts digits only, so a minus sign or exponent cannot be typed', async () => {
    render(<Field />)

    await userEvent.type(screen.getByLabelText('Seats'), '-4e2.5')

    expect(screen.getByLabelText('Seats')).toHaveValue('425')
  })

  it('checks limits and rejects an empty value', () => {
    expect(wholeNumberBetween('4', 1, 4)).toBeUndefined()
    expect(wholeNumberBetween('5', 1, 4)).toBe('Enter a whole number from 1 to 4')
    expect(wholeNumberBetween('', 0, 14)).toBeDefined()
  })
})
