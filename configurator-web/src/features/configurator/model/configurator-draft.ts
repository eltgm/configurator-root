import { configuratorDraftStorageKey } from '@/shared/config/preferences';

export const configuratorDraftVersion = 1 as const;
export const configuratorDraftMaxItems = 50;

export interface ConfiguratorDraftItem {
  componentId: number;
  componentTypeId: number;
}

export interface ConfiguratorDraft {
  items: ConfiguratorDraftItem[];
  updatedAt: string | null;
}

interface StoredConfiguratorDraftV1 {
  version: typeof configuratorDraftVersion;
  updatedAt: string;
  items: ConfiguratorDraftItem[];
}

export type ConfiguratorDraftReadStatus = 'empty' | 'restored' | 'invalid' | 'unavailable';

export interface ConfiguratorDraftReadResult {
  draft: ConfiguratorDraft;
  status: ConfiguratorDraftReadStatus;
}

export type ConfiguratorDraftAddResult =
  | { status: 'added'; items: ConfiguratorDraftItem[] }
  | { status: 'already-selected'; items: ConfiguratorDraftItem[] }
  | {
      status: 'replacement-required';
      items: ConfiguratorDraftItem[];
      replacedItem: ConfiguratorDraftItem;
    }
  | { status: 'limit-reached'; items: ConfiguratorDraftItem[] };

export function emptyConfiguratorDraft(): ConfiguratorDraft {
  return { items: [], updatedAt: null };
}

function isPositiveInteger(value: unknown): value is number {
  return typeof value === 'number' && Number.isInteger(value) && value > 0;
}

function isValidStoredDraft(value: unknown): value is StoredConfiguratorDraftV1 {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const candidate = value as Partial<StoredConfiguratorDraftV1>;
  if (
    candidate.version !== configuratorDraftVersion ||
    typeof candidate.updatedAt !== 'string' ||
    Number.isNaN(Date.parse(candidate.updatedAt)) ||
    !Array.isArray(candidate.items) ||
    candidate.items.length > configuratorDraftMaxItems
  ) {
    return false;
  }
  const componentIds = new Set<number>();
  const componentTypeIds = new Set<number>();
  return candidate.items.every((item) => {
    if (
      !item ||
      typeof item !== 'object' ||
      !isPositiveInteger(item.componentId) ||
      !isPositiveInteger(item.componentTypeId) ||
      componentIds.has(item.componentId) ||
      componentTypeIds.has(item.componentTypeId)
    ) {
      return false;
    }
    componentIds.add(item.componentId);
    componentTypeIds.add(item.componentTypeId);
    return true;
  });
}

export function readConfiguratorDraft(
  domainId: number,
  storage?: Pick<Storage, 'getItem'>,
): ConfiguratorDraftReadResult {
  let raw: string | null;
  try {
    raw = (storage ?? window.localStorage).getItem(configuratorDraftStorageKey(domainId));
  } catch {
    return { draft: emptyConfiguratorDraft(), status: 'unavailable' };
  }
  if (raw === null) {
    return { draft: emptyConfiguratorDraft(), status: 'empty' };
  }
  try {
    const parsed: unknown = JSON.parse(raw);
    if (!isValidStoredDraft(parsed)) {
      return { draft: emptyConfiguratorDraft(), status: 'invalid' };
    }
    return {
      draft: {
        items: parsed.items.map((item) => ({ ...item })),
        updatedAt: parsed.updatedAt,
      },
      status: 'restored',
    };
  } catch {
    return { draft: emptyConfiguratorDraft(), status: 'invalid' };
  }
}

export function writeConfiguratorDraft(
  domainId: number,
  items: ReadonlyArray<ConfiguratorDraftItem>,
  storage?: Pick<Storage, 'setItem'>,
  now: () => Date = () => new Date(),
) {
  const updatedAt = now().toISOString();
  const stored: StoredConfiguratorDraftV1 = {
    version: configuratorDraftVersion,
    updatedAt,
    items: items.map((item) => ({ ...item })),
  };
  try {
    (storage ?? window.localStorage).setItem(
      configuratorDraftStorageKey(domainId),
      JSON.stringify(stored),
    );
    return { persisted: true as const, updatedAt };
  } catch {
    return { persisted: false as const, updatedAt: null };
  }
}

export function addConfiguratorDraftItem(
  items: ReadonlyArray<ConfiguratorDraftItem>,
  item: ConfiguratorDraftItem,
): ConfiguratorDraftAddResult {
  if (items.some((candidate) => candidate.componentId === item.componentId)) {
    return { status: 'already-selected', items: [...items] };
  }
  const replacedItem = items.find(
    (candidate) => candidate.componentTypeId === item.componentTypeId,
  );
  if (replacedItem) {
    return { status: 'replacement-required', items: [...items], replacedItem };
  }
  if (items.length >= configuratorDraftMaxItems) {
    return { status: 'limit-reached', items: [...items] };
  }
  return { status: 'added', items: [...items, item] };
}

export function replaceConfiguratorDraftItem(
  items: ReadonlyArray<ConfiguratorDraftItem>,
  item: ConfiguratorDraftItem,
) {
  return items.map((candidate) =>
    candidate.componentTypeId === item.componentTypeId ? item : candidate,
  );
}

export function removeConfiguratorDraftItem(
  items: ReadonlyArray<ConfiguratorDraftItem>,
  componentId: number,
) {
  return items.filter((item) => item.componentId !== componentId);
}
