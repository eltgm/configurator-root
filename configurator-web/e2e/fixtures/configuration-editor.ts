import type { Page } from '@playwright/test';

import type {
  SavedConfiguration,
  ConfiguratorCandidatesResponse,
} from '../../src/shared/api/generated/types.gen';
import { frontendApiBaseUrl } from './mock-api';

export async function openConfigurationEditor(page: Page, componentCount = 2) {
  let configuration: SavedConfiguration = {
    id: 990,
    domainId: 101,
    name: 'Тестовая конфигурация',
    description: 'Сохранённое описание',
    createdAt: '2026-08-31T10:00:00Z',
    components: Array.from({ length: componentCount }, (_, index) => ({
      id: 1000 + index,
      name: `Компонент ${index + 1}`,
      componentTypeId: 2000 + index,
      componentTypeName: `Тип ${index + 1}`,
      archived: false,
    })),
  };
  const componentTypes = configuration.components.map((component, index) => ({
    id: component.componentTypeId,
    domainId: 101,
    name: component.componentTypeName,
    orderIndex: index,
  }));
  const candidates = componentTypes.flatMap((type, index) =>
    Array.from({ length: index === 0 ? 13 : 1 }, (_, candidateIndex) => ({
      id: 10000 + index * 100 + candidateIndex,
      name: `Замена ${index + 1}.${candidateIndex + 1}`,
      componentTypeId: type.id,
      componentTypeName: type.name,
      archived: false,
    })),
  );
  await page.route(`${frontendApiBaseUrl}/domains/101/component-types`, (route) =>
    route.fulfill({ json: componentTypes }),
  );
  await page.route(`${frontendApiBaseUrl}/domains/101/configurator/candidates`, async (route) => {
    const { componentIds } = route.request().postDataJSON() as { componentIds: number[] };
    const response: ConfiguratorCandidatesResponse = {
      componentIds,
      assemblyStatus: 'VALID',
      assemblyDecisions: componentIds.slice(1).map((id) => ({
        leftComponentId: componentIds[0],
        rightComponentId: id,
        status: 'ALLOWED',
        explanations: [],
        blockingRules: [],
      })),
      candidatesByType: componentTypes.map((type) => ({
        componentTypeId: type.id,
        componentTypeName: type.name,
        components: candidates
          .filter((candidate) => candidate.componentTypeId === type.id)
          .map((candidate) => ({
            ...candidate,
            status: 'AVAILABLE',
            compatibilityByBase: componentIds.map((baseComponentId) => ({
              baseComponentId,
              status: 'ALLOWED',
              explanations: [],
              blockingRules: [],
            })),
          })),
      })),
    };
    await route.fulfill({ json: response });
  });
  await page.route(`${frontendApiBaseUrl}/configurations/990`, async (route) => {
    if (route.request().method() === 'PUT') {
      const body = route.request().postDataJSON() as {
        name: string;
        description: string;
        componentIds: number[];
      };
      const components = [...configuration.components, ...candidates];
      configuration = {
        ...configuration,
        name: body.name,
        description: body.description,
        components: body.componentIds.map((id) => components.find((item) => item.id === id)!),
      };
    }
    await route.fulfill({ json: configuration });
  });
  await page.goto('/configurations/990/edit');
}
