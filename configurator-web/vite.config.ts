import { fileURLToPath, URL } from 'node:url';

import react from '@vitejs/plugin-react';
import { defineConfig } from 'vitest/config';

export default defineConfig({
  plugins: [react()],
  build: {
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              name: 'react-vendor',
              test: /node_modules[\\/](react|react-dom|react-router|react-router-dom)[\\/]/,
              priority: 10,
            },
            {
              name: 'mantine-vendor',
              test: /node_modules[\\/](@mantine|@floating-ui)[\\/]/,
              priority: 9,
            },
            {
              name: 'query-vendor',
              test: /node_modules[\\/]@tanstack[\\/]/,
              priority: 8,
            },
            {
              name: 'i18n-vendor',
              test: /node_modules[\\/](i18next|react-i18next)[\\/]/,
              priority: 7,
            },
            {
              name: 'icons-vendor',
              test: /node_modules[\\/]@tabler[\\/]/,
              priority: 6,
            },
          ],
        },
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: false,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  preview: {
    host: '127.0.0.1',
    port: 4173,
    strictPort: true,
    proxy: {},
  },
  test: {
    include: ['src/**/*.test.{ts,tsx}'],
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: true,
    maxWorkers: 2,
    testTimeout: 20_000,
    coverage: {
      provider: 'v8',
      exclude: ['src/shared/api/generated/**'],
      reporter: ['text', 'html', 'lcov'],
      reportsDirectory: './coverage',
      thresholds: {
        lines: 90,
        statements: 90,
        functions: 85,
        branches: 80,
      },
    },
  },
});
