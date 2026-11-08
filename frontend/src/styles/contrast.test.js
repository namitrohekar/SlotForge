import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const css = readFileSync('src/styles/tokens.css', 'utf8')

function tokensIn(selector) {
  const start = css.indexOf(`${selector} {`)
  const body = css.slice(start, css.indexOf('}', start))
  return Object.fromEntries(
    [...body.matchAll(/--sf-([\w-]+):\s*(#[0-9A-Fa-f]{6});/g)].map(([, name, value]) => [name, value]),
  )
}

function luminance(hex) {
  const [r, g, b] = [1, 3, 5]
    .map((start) => parseInt(hex.slice(start, start + 2), 16) / 255)
    .map((channel) => (channel <= 0.03928 ? channel / 12.92 : ((channel + 0.055) / 1.055) ** 2.4))
  return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

function contrast(first, second) {
  const [lighter, darker] = [luminance(first), luminance(second)].sort((a, b) => b - a)
  return (lighter + 0.05) / (darker + 0.05)
}

const TEXT = 4.5
const CONTROL = 3

const pairs = [
  ...['canvas', 'surface', 'raised'].map((background) => ['ink', background, TEXT]),
  ...['canvas', 'surface', 'raised', 'sunken'].flatMap((background) => [
    ['ink-secondary', background, TEXT],
    ['ink-tertiary', background, TEXT],
  ]),
  ['accent', 'surface', TEXT],
  ['accent', 'canvas', TEXT],
  ['accent', 'raised', TEXT],
  ['accent', 'accent-subtle', TEXT],
  ['on-accent', 'accent', TEXT],
  ['on-accent', 'accent-hover', TEXT],
  ['on-danger', 'danger', TEXT],
  ['on-danger', 'danger-hover', TEXT],
  ['line-strong', 'surface', CONTROL],
  ['line-strong', 'canvas', CONTROL],
  ['line-strong', 'raised', CONTROL],
  ['focus', 'surface', CONTROL],
  ['focus', 'canvas', CONTROL],
  ['success', 'success-subtle', TEXT],
  ['warning', 'warning-subtle', TEXT],
  ['danger', 'danger-subtle', TEXT],
  ['info', 'info-subtle', TEXT],
  ['success', 'surface', TEXT],
  ['warning', 'surface', TEXT],
  ['danger', 'surface', TEXT],
]

describe.each([
  ['light', "[data-theme='light']"],
  ['dark', "[data-theme='dark']"],
])('%s theme', (_, selector) => {
  const tokens = tokensIn(selector)

  it.each(pairs)('%s on %s meets %s:1', (foreground, background, minimum) => {
    expect(contrast(tokens[foreground], tokens[background])).toBeGreaterThanOrEqual(minimum)
  })
})
