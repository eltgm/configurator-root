import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './e2e/delivery',
  fullyParallel: false,
  forbidOnly: true,
  retries: 0,
  workers: 1,
  timeout: 30_000,
  outputDir: 'test-results/delivery',
  reporter: process.env.CI
    ? [['line'], ['github']]
    : [['list'], ['html', { open: 'never', outputFolder: 'playwright-report/delivery' }]],
  use: {
    baseURL: process.env.CONFIGURATOR_DELIVERY_BASE_URL ?? 'http://127.0.0.1:8080',
    locale: 'ru-RU',
    timezoneId: 'Europe/Moscow',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    {
      name: 'delivery-chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],
});
