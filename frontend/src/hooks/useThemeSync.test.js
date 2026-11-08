import { describe, expect, it } from 'vitest'
import { resolveTheme } from './useThemeSync.js'

describe('resolveTheme', () => {
  it('uses the chosen theme', () => {
    expect(resolveTheme('dark', false)).toBe('dark')
    expect(resolveTheme('light', true)).toBe('light')
  })

  it('follows the system preference', () => {
    expect(resolveTheme('system', true)).toBe('dark')
    expect(resolveTheme('system', false)).toBe('light')
  })
})
