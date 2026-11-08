import { readFileSync } from 'node:fs'
import { afterEach, describe, expect, it, vi } from 'vitest'

const html = readFileSync('index.html', 'utf8')
const script = html.match(/<script>([\s\S]*?)<\/script>/)[1]

function themeAfterRunning({ stored, systemDark = false }) {
  vi.stubGlobal('matchMedia', () => ({ matches: systemDark }))
  vi.stubGlobal('localStorage', { getItem: stored })
  new Function(script)()
  return document.documentElement.dataset.theme
}

const saved = (theme) => () => JSON.stringify({ state: { theme }, version: 0 })

describe('theme script in index.html', () => {
  afterEach(() => vi.unstubAllGlobals())

  it('uses the saved theme', () => {
    expect(themeAfterRunning({ stored: saved('dark') })).toBe('dark')
    expect(themeAfterRunning({ stored: saved('light'), systemDark: true })).toBe('light')
  })

  it('follows the system when set to system', () => {
    expect(themeAfterRunning({ stored: saved('system'), systemDark: true })).toBe('dark')
    expect(themeAfterRunning({ stored: saved('system') })).toBe('light')
  })

  it('follows the system when nothing is saved', () => {
    expect(themeAfterRunning({ stored: () => null, systemDark: true })).toBe('dark')
  })

  it('survives corrupt storage', () => {
    expect(themeAfterRunning({ stored: () => '{broken', systemDark: true })).toBe('dark')
  })

  it('survives blocked storage', () => {
    const blocked = () => {
      throw new Error('blocked')
    }
    expect(themeAfterRunning({ stored: blocked })).toBe('light')
  })
})
