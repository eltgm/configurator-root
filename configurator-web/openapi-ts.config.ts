import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: '../specs/configurator-api.yaml',
  output: {
    path: process.env.CONFIGURATOR_OPENAPI_OUTPUT ?? 'src/shared/api/generated',
    clean: true,
    header: ({ defaultValue }) => [...defaultValue, '// @ts-nocheck'],
    postProcess: [
      {
        command: 'prettier',
        args: [
          '--config',
          './.prettierrc.json',
          '--ignore-path',
          './.prettierignore',
          '--ignore-unknown',
          '--write',
          '{{path}}',
        ],
      },
    ],
  },
  plugins: [
    '@hey-api/typescript',
    {
      name: '@hey-api/client-fetch',
      baseUrl: '/api',
    },
    '@hey-api/sdk',
  ],
});
