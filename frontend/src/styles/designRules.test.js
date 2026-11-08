import { globSync, readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const files = globSync('src/{components,features}/**/*.jsx').filter((file) => !file.endsWith('.test.jsx'))

const rules = [
  ['raw hex colours', /#[0-9a-fA-F]{3,8}\b/],
  ['dark: variants, because tokens already switch by theme', /\bdark:/],
  ['gradients', /\bbg-(gradient|linear|radial|conic)-/],
  ['glass effects', /\bbackdrop-blur/],
]

describe('design rules', () => {
  it('finds the components to check', () => {
    expect(files.length).toBeGreaterThan(20)
  })

  it.each(rules)('no component uses %s', (_, pattern) => {
    const offenders = files.filter((file) => pattern.test(readFileSync(file, 'utf8')))
    expect(offenders).toEqual([])
  })
})
