import { test } from '@playwright/test'
import { stubApi } from './support.js'

const screens = [
  ['design', '/design'],
  ['system', '/system'],
  ['login', '/login'],
  ['users', '/users'],
  ['departments', '/academics/departments'],
  ['programmes', '/academics/courses'],
  ['curriculum', '/academics/curricula/1'],
  ['timetable', '/sessions/1/timetable'],
]
const sizes = [
  ['desktop', 1440, 900],
  ['phone', 390, 844],
]

for (const [name, route] of screens) {
  for (const [size, width, height] of sizes) {
    for (const colorScheme of ['light', 'dark']) {
      test(`${name} ${size} ${colorScheme}`, async ({ page }) => {
        await stubApi(page, { signedIn: name !== 'login' })
        // Reduced motion, so the shot shows the settled page rather than its fade-in.
        await page.emulateMedia({ colorScheme, reducedMotion: 'reduce' })
        await page.setViewportSize({ width, height })
        await page.goto(route)
        await page.locator('h1').waitFor()
        await page.waitForLoadState('networkidle')
        await page.screenshot({ path: `test-results/screenshots/${name}-${size}-${colorScheme}.png`, fullPage: true })
      })
    }
  }
}
