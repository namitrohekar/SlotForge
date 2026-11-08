import AxeBuilder from '@axe-core/playwright'
import { expect, test } from '@playwright/test'
import { routes, stubApi } from './support.js'

test.beforeEach(async ({ page }) => stubApi(page))

const savedTheme = (theme) => JSON.stringify({ state: { theme, sidebarCollapsed: false }, version: 0 })

for (const colorScheme of ['light', 'dark']) {
  for (const route of routes) {
    test(`${route} has no serious accessibility issues in ${colorScheme}`, async ({ page }) => {
      // Axe must measure finished colours, not a page halfway through its fade-in.
      await page.emulateMedia({ colorScheme, reducedMotion: 'reduce' })
      await page.goto(route)
      await page.locator('h1').waitFor()

      const results = await new AxeBuilder({ page })
        .withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa'])
        .analyze()

      const blocking = results.violations.filter(({ impact }) => ['serious', 'critical'].includes(impact))
      expect(blocking.map(({ id, nodes }) => `${id}: ${nodes[0].target}`)).toEqual([])
    })
  }
}

test('the saved theme applies before the app loads, so nothing flashes', async ({ page }) => {
  await page.addInitScript((value) => localStorage.setItem('slotforge-preferences', value), savedTheme('dark'))
  await page.emulateMedia({ colorScheme: 'light' })
  await page.route('**/src/main.jsx*', (route) => route.abort())

  await page.goto('/')

  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
})

test('choosing a theme applies it and survives a reload', async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'light' })
  await page.goto('/')
  await page.locator('h1').waitFor()

  await page.getByRole('radio', { name: 'Dark theme' }).check({ force: true })
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')

  await page.reload()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
})

test('system theme follows the operating system live', async ({ page }) => {
  await page.emulateMedia({ colorScheme: 'light' })
  await page.goto('/')
  await page.locator('h1').waitFor()
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'light')

  await page.emulateMedia({ colorScheme: 'dark' })
  await expect(page.locator('html')).toHaveAttribute('data-theme', 'dark')
})

test('the first Tab reaches the skip link', async ({ page }) => {
  await page.goto('/')
  await page.locator('h1').waitFor()

  await page.keyboard.press('Tab')

  await expect(page.getByRole('link', { name: 'Skip to main content' })).toBeFocused()
})

test('a dialog traps focus, closes with Escape and returns focus', async ({ page }) => {
  await page.goto('/design')
  const opener = page.getByRole('button', { name: 'Open dialog' })

  await opener.click()
  await expect(page.getByRole('dialog', { name: 'Add hall' })).toBeVisible()

  await page.keyboard.press('Escape')
  await expect(page.getByRole('dialog', { name: 'Add hall' })).toBeHidden()
  await expect(opener).toBeFocused()
})

test('a menu opens, works by keyboard and closes with Escape', async ({ page }) => {
  await page.goto('/design')
  await page.getByRole('button', { name: 'Row actions' }).click()

  const menu = page.getByRole('menu', { name: 'Row actions' })
  await expect(menu).toBeVisible()
  await expect(page.getByRole('menuitem', { name: 'Edit' })).toBeFocused()

  await page.keyboard.press('ArrowDown')
  await expect(page.getByRole('menuitem', { name: 'Lock' })).toBeFocused()

  await page.keyboard.press('End')
  await expect(page.getByRole('menuitem', { name: 'Delete' })).toBeFocused()

  await page.keyboard.press('ArrowDown')
  await expect(page.getByRole('menuitem', { name: 'Edit' })).toBeFocused()

  await page.keyboard.press('Escape')
  await expect(menu).toBeHidden()
})

test('reduced motion removes animation time', async ({ page }) => {
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.goto('/')
  await page.locator('h1').waitFor()

  const duration = await page.evaluate(() =>
    getComputedStyle(document.documentElement).getPropertyValue('--duration-base').trim(),
  )
  expect(duration).toBe('0ms')
})

test('a signed-out visitor is sent to sign-in, and lands where they were going', async ({ page }) => {
  await page.unrouteAll()
  await stubApi(page, { signedIn: false })
  await page.emulateMedia({ reducedMotion: 'reduce' })
  await page.route('**/api/auth/demo-login', (route) => route.fulfill({ json: { id: 1, fullName: 'Demo Admin', email: 'admin@demo.slotforge.test', roles: ['ADMIN'] } }))
  await page.goto('/system')

  await expect(page).toHaveURL(/\/login$/)
  const axe = await new AxeBuilder({ page }).withTags(['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa', 'wcag22aa']).analyze()
  expect(axe.violations.filter(({ impact }) => ['serious', 'critical'].includes(impact)).map(({ id }) => id)).toEqual([])

  await page.getByRole('button', { name: /Administrator/ }).click()

  await expect(page).toHaveURL(/\/system$/)
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
})
