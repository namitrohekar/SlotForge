import { readFileSync } from 'node:fs'
import { afterEach, describe, expect, it, vi } from 'vitest'

const script = readFileSync('public/theme-init.js', 'utf8')

function themeAfterRunning({ stored, systemDark = false }) {
  vi.stubGlobal('matchMedia', () => ({ matches: systemDark }))
  vi.stubGlobal('localStorage', { getItem: stored })
  new Function(script)()
  return document.documentElement.dataset.theme
}

const saved = (theme) => () => JSON.stringify({ state: { theme }, version: 0 })

describe('theme script loaded by index.html', () => {
  it('is loaded before the app, as a file the security policy allows', () => {
    const html = readFileSync('index.html', 'utf8')
    expect(html).not.toMatch(/<script>/)
    expect(html.indexOf('/theme-init.js')).toBeLessThan(html.indexOf('/src/main.jsx'))
  })

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
