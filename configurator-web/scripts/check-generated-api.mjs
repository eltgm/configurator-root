import { spawnSync } from 'node:child_process';
import { mkdtemp, readFile, readdir, rm } from 'node:fs/promises';
import { tmpdir } from 'node:os';
import { dirname, join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const scriptDirectory = dirname(fileURLToPath(import.meta.url));
const projectDirectory = join(scriptDirectory, '..');
const committedDirectory = join(projectDirectory, 'src/shared/api/generated');
const temporaryDirectory = await mkdtemp(join(tmpdir(), 'configurator-openapi-'));

async function collectFiles(directory, currentDirectory = directory) {
  const files = new Map();

  for (const entry of await readdir(currentDirectory, { withFileTypes: true })) {
    const absolutePath = join(currentDirectory, entry.name);

    if (entry.isDirectory()) {
      const nestedFiles = await collectFiles(directory, absolutePath);
      for (const [path, content] of nestedFiles) {
        files.set(path, content);
      }
    } else if (entry.isFile()) {
      files.set(relative(directory, absolutePath), await readFile(absolutePath));
    }
  }

  return files;
}

try {
  const generatorPath = join(projectDirectory, 'node_modules/@hey-api/openapi-ts/bin/run.js');
  const generation = spawnSync(process.execPath, [generatorPath], {
    cwd: projectDirectory,
    env: {
      ...process.env,
      CONFIGURATOR_OPENAPI_OUTPUT: temporaryDirectory,
    },
    stdio: 'inherit',
  });

  if (generation.status !== 0) {
    process.exitCode = generation.status ?? 1;
  } else {
    const committedFiles = await collectFiles(committedDirectory);
    const generatedFiles = await collectFiles(temporaryDirectory);
    const allPaths = new Set([...committedFiles.keys(), ...generatedFiles.keys()]);
    const differences = [...allPaths].sort().filter((path) => {
      const committedFile = committedFiles.get(path);
      const generatedFile = generatedFiles.get(path);
      return !committedFile || !generatedFile || !committedFile.equals(generatedFile);
    });

    if (differences.length > 0) {
      console.error('Generated API client is out of date:');
      for (const path of differences) {
        console.error(`- ${path}`);
      }
      console.error('Run "npm run api:generate" and commit the generated changes.');
      process.exitCode = 1;
    } else {
      console.log('Generated API client is up to date.');
    }
  }
} finally {
  await rm(temporaryDirectory, { recursive: true, force: true });
}
