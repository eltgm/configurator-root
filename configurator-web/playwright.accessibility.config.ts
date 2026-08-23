import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e/accessibility',
  fullyParallel: true,
  forbidOnly: Boolean(process.env.CI),
  retries: process.env.CI ? 2 : 0,
  workers: process.env.CI ? 1 : undefined,
  reporter: process.env.CI
    ? [['github'], ['html', { open: 'never', outputFolder: 'playwright-report/accessibility' }]]
    : [['list'], ['html', { open: 'never', outputFolder: 'playwright-report/accessibility' }]],
  outputDir: 'test-results/accessibility',
  use: {
    baseURL: 'http://127.0.0.1:5173',
    locale: 'ru-RU',
    timezoneId: 'Europe/Moscow',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'a11y-desktop',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1440, height: 900 } },
    },
    {
      name: 'a11y-mobile',
      use: { ...devices['Pixel 7'], viewport: { width: 390, height: 844 } },
    },
  ],
  webServer: {
    command: 'npm run dev',
    url: 'http://127.0.0.1:5173',
    reuseExistingServer: !process.env.CI,
    timeout: 120_000,
  },
});
