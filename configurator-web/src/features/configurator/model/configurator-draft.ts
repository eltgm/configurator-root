import { configuratorDraftStorageKey } from '@/shared/config/preferences';

export const configuratorDraftVersion = 3 as const;
export const configuratorDraftMaxItems = 50;
export const configuratorDraftMaxQuantity = 999_999;

export interface ConfiguratorDraftItem {
  componentId: number;
  componentTypeId: number;
  quantity: number;
}

export interface ConfiguratorDraft {
  items: ConfiguratorDraftItem[];
  trackInventory: boolean;
  updatedAt: string | null;
}

interface StoredConfiguratorDraftV1 {
  version: 1;
  updatedAt: string;
  items: Array<Omit<ConfiguratorDraftItem, 'quantity'>>;
}

interface StoredConfiguratorDraftV2 {
  version: 2;
  updatedAt: string;
  items: ConfiguratorDraftItem[];
}

interface StoredConfiguratorDraftV3 {
  version: typeof configuratorDraftVersion;
  updatedAt: string;
  items: ConfiguratorDraftItem[];
  trackInventory: boolean;
}

export type ConfiguratorDraftReadStatus = 'empty' | 'restored' | 'invalid' | 'unavailable';

export interface ConfiguratorDraftReadResult {
  draft: ConfiguratorDraft;
  status: ConfiguratorDraftReadStatus;
}

export type ConfiguratorDraftAddResult =
  | { status: 'added'; items: ConfiguratorDraftItem[] }
  | { status: 'quantity-increased'; items: ConfiguratorDraftItem[] }
  | { status: 'limit-reached'; items: ConfiguratorDraftItem[] };

export function emptyConfiguratorDraft(): ConfiguratorDraft {
  return { items: [], trackInventory: false, updatedAt: null };
}

function isPositiveInteger(value: unknown): value is number {
  return typeof value === 'number' && Number.isInteger(value) && value > 0;
}

function isValidStoredDraft(
  value: unknown,
): value is StoredConfiguratorDraftV1 | StoredConfiguratorDraftV2 | StoredConfiguratorDraftV3 {
  if (!value || typeof value !== 'object') {
    return false;
  }
  const candidate = value as Partial<
    StoredConfiguratorDraftV1 | StoredConfiguratorDraftV2 | StoredConfiguratorDraftV3
  >;
  if (
    (candidate.version !== 1 && candidate.version !== 2 && candidate.version !== 3) ||
    typeof candidate.updatedAt !== 'string' ||
    Number.isNaN(Date.parse(candidate.updatedAt)) ||
    !Array.isArray(candidate.items) ||
    candidate.items.length > configuratorDraftMaxItems
  ) {
    return false;
  }
  if (candidate.version === 3 && typeof candidate.trackInventory !== 'boolean') {
    return false;
  }
  const componentIds = new Set<number>();
  return candidate.items.every((item) => {
    if (
      !item ||
      typeof item !== 'object' ||
      !isPositiveInteger(item.componentId) ||
      !isPositiveInteger(item.componentTypeId) ||
      componentIds.has(item.componentId) ||
      (candidate.version !== 1 &&
        (!isPositiveInteger((item as ConfiguratorDraftItem).quantity) ||
          (item as ConfiguratorDraftItem).quantity > configuratorDraftMaxQuantity))
    ) {
      return false;
    }
    componentIds.add(item.componentId);
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
        items: parsed.items.map((item) => ({
          componentId: item.componentId,
          componentTypeId: item.componentTypeId,
          quantity: parsed.version === 1 ? 1 : (item as ConfiguratorDraftItem).quantity,
        })),
        trackInventory: parsed.version === 3 ? parsed.trackInventory : false,
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
  trackInventory: boolean,
  storage?: Pick<Storage, 'setItem'>,
  now: () => Date = () => new Date(),
) {
  const updatedAt = now().toISOString();
  const stored: StoredConfiguratorDraftV3 = {
    version: configuratorDraftVersion,
    updatedAt,
    items: items.map((item) => ({ ...item })),
    trackInventory,
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
  item: Omit<ConfiguratorDraftItem, 'quantity'>,
): ConfiguratorDraftAddResult {
  const existing = items.find((candidate) => candidate.componentId === item.componentId);
  if (existing) {
    return {
      status: 'quantity-increased',
      items: items.map((candidate) =>
        candidate.componentId === item.componentId
          ? {
              ...candidate,
              quantity: Math.min(candidate.quantity + 1, configuratorDraftMaxQuantity),
            }
          : candidate,
      ),
    };
  }
  if (items.length >= configuratorDraftMaxItems) {
    return { status: 'limit-reached', items: [...items] };
  }
  return { status: 'added', items: [...items, { ...item, quantity: 1 }] };
}

export function replaceConfiguratorDraftItem(
  items: ReadonlyArray<ConfiguratorDraftItem>,
  replacedComponentId: number,
  item: Omit<ConfiguratorDraftItem, 'quantity'>,
) {
  return items.map((candidate) =>
    candidate.componentId === replacedComponentId
      ? { ...item, quantity: candidate.quantity }
      : candidate,
  );
}

export function setConfiguratorDraftItemQuantity(
  items: ReadonlyArray<ConfiguratorDraftItem>,
  componentId: number,
  quantity: number,
) {
  const normalizedQuantity = Math.max(1, Math.min(quantity, configuratorDraftMaxQuantity));
  return items.map((item) =>
    item.componentId === componentId ? { ...item, quantity: normalizedQuantity } : item,
  );
}

export function removeConfiguratorDraftItem(
  items: ReadonlyArray<ConfiguratorDraftItem>,
  componentId: number,
) {
  return items.filter((item) => item.componentId !== componentId);
}
