import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  fullyParallel: true,
  reporter: 'line',
  use: { baseURL: 'http://localhost:5173' },
  webServer: { command: 'npm run dev', url: 'http://localhost:5173', reuseExistingServer: true },
  projects: [
    // Stubbed API data, so these never need the backend or PostgreSQL.
    { name: 'stubbed', testMatch: /(design-system|responsive)\.spec\.js/, use: { ...devices['Desktop Chrome'] } },
    // Screens for human review, saved to test-results/screenshots.
    { name: 'screenshots', testMatch: /screenshots\.spec\.js/, use: { ...devices['Desktop Chrome'] } },
  ],
})
