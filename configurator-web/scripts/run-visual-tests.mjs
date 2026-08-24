import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const frontendDirectory = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const repositoryDirectory = resolve(frontendDirectory, '..');
const packageJson = JSON.parse(readFileSync(resolve(frontendDirectory, 'package.json'), 'utf8'));
const playwrightVersion = packageJson.devDependencies?.['@playwright/test'];

if (!/^\d+\.\d+\.\d+$/.test(playwrightVersion)) {
  throw new Error('The @playwright/test dependency must use an exact semantic version.');
}

const supportedArguments = new Set(['--update-snapshots']);
for (const argument of process.argv.slice(2)) {
  if (!supportedArguments.has(argument)) {
    throw new Error(`Unsupported visual test argument: ${argument}`);
  }
}

const image = `mcr.microsoft.com/playwright:v${playwrightVersion}-noble`;
const containerArguments = [
  'run',
  '--rm',
  '--ipc=host',
  '--env',
  'CI=1',
  '--env',
  'HOME=/tmp',
  '--volume',
  `${repositoryDirectory}:/workspace`,
  '--volume',
  '/workspace/configurator-web/node_modules',
  '--workdir',
  '/workspace/configurator-web',
  image,
  'bash',
  '-lc',
  `npm ci && npm run test:visual:container${process.argv.includes('--update-snapshots') ? ' -- --update-snapshots' : ''}`,
];

console.log(`Running visual regression in ${image}`);
execFileSync('docker', containerArguments, { stdio: 'inherit' });
