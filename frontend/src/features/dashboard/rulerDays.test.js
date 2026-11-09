import { describe, expect, it } from 'vitest'
import { rulerDays } from './rulerDays.js'

describe('rulerDays', () => {
  it('folds a weekend into one gap', () => {
    // 2026-11-13 is a Friday
    expect(rulerDays('2026-11-13', '2026-11-16', {})).toEqual(['2026-11-13', null, '2026-11-16'])
  })

  it('keeps a weekend day that has a paper', () => {
    expect(rulerDays('2026-11-13', '2026-11-16', { '2026-11-14': 1 })).toEqual(['2026-11-13', '2026-11-14', null, '2026-11-16'])
  })

  it('shows only days with papers when the session is long', () => {
    expect(rulerDays('2026-11-02', '2026-11-30', { '2026-11-05': 2, '2026-11-20': 1 })).toEqual(['2026-11-05', '2026-11-20'])
  })
})
