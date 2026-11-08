import { expect, test } from '@playwright/test'
import { routes, stubApi } from './support.js'

test.beforeEach(async ({ page }) => stubApi(page))

const widths = [320, 375, 768, 1024, 1440]

for (const colorScheme of ['light', 'dark']) {
  for (const width of widths) {
    for (const route of routes) {
      test(`${route} fits ${width}px in ${colorScheme}`, async ({ page }) => {
        await page.emulateMedia({ colorScheme })
        await page.setViewportSize({ width, height: 800 })
        await page.goto(route)
        await page.locator('h1').waitFor()

        const overflow = await page.evaluate(
          () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
        )
        expect(overflow).toBeLessThanOrEqual(0)
      })
    }
  }
}

test('phones use the tab bar and larger screens use the sidebar', async ({ page }) => {
  await page.setViewportSize({ width: 375, height: 800 })
  await page.goto('/')
  await page.locator('h1').waitFor()
  await expect(page.locator('nav[aria-label="Main"]:visible')).toHaveClass(/bottom-0/)

  await page.setViewportSize({ width: 1280, height: 800 })
  await expect(page.locator('aside')).toBeVisible()
})

test('touch controls are at least 44px tall', async ({ browser }) => {
  const context = await browser.newContext({ hasTouch: true, isMobile: true, viewport: { width: 375, height: 800 } })
  const page = await context.newPage()
  await stubApi(page)
  await page.goto('/design')
  await page.locator('h1').waitFor()

  const box = await page.getByRole('button', { name: 'Create exam' }).boundingBox()
  expect(box.height).toBeGreaterThanOrEqual(44)
  await context.close()
})

for (const width of widths) {
  test(`/login fits ${width}px`, async ({ page }) => {
    await page.unrouteAll()
    await stubApi(page, { signedIn: false })
    await page.setViewportSize({ width, height: 800 })
    await page.goto('/login')
    await page.getByRole('button', { name: /Student/ }).waitFor()

    const overflow = await page.evaluate(
      () => document.documentElement.scrollWidth - document.documentElement.clientWidth,
    )
    expect(overflow).toBeLessThanOrEqual(0)
  })
}
