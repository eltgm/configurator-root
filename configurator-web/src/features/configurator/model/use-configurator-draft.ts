import { useState } from 'react';

import { useComponentDetailsQueries } from '@/features/components/api/components';
import {
  addConfiguratorDraftItem,
  type ConfiguratorDraftAddResult,
  type ConfiguratorDraftItem,
  type ConfiguratorDraftReadStatus,
  readConfiguratorDraft,
  removeConfiguratorDraftItem,
  replaceConfiguratorDraftItem,
  writeConfiguratorDraft,
} from '@/features/configurator/model/configurator-draft';
import type { Component } from '@/shared/api';

export interface ConfiguratorDraftSelection {
  id: number;
  componentTypeId: number;
}

export interface ConfiguratorDraftSlot {
  item: ConfiguratorDraftItem;
  component: Component | null;
  status: 'loading' | 'ready' | 'error';
  retry: () => void;
}

export function useConfiguratorDraft(domainId: number) {
  const [initialDraft] = useState(() => readConfiguratorDraft(domainId));
  const [items, setItems] = useState(initialDraft.draft.items);
  const [updatedAt, setUpdatedAt] = useState(initialDraft.draft.updatedAt);
  const [readStatus] = useState<ConfiguratorDraftReadStatus>(initialDraft.status);
  const [persistenceAvailable, setPersistenceAvailable] = useState(
    initialDraft.status !== 'unavailable',
  );
  const componentQueries = useComponentDetailsQueries(
    domainId,
    items.map((item) => item.componentId),
  );

  const commit = (nextItems: ConfiguratorDraftItem[]) => {
    setItems(nextItems);
    const result = writeConfiguratorDraft(domainId, nextItems);
    setPersistenceAvailable(result.persisted);
    if (result.persisted) {
      setUpdatedAt(result.updatedAt);
    }
  };

  const add = (component: ConfiguratorDraftSelection): ConfiguratorDraftAddResult => {
    const result = addConfiguratorDraftItem(items, {
      componentId: component.id,
      componentTypeId: component.componentTypeId,
    });
    if (result.status === 'added') {
      commit(result.items);
    }
    return result;
  };

  const replace = (component: ConfiguratorDraftSelection) => {
    commit(
      replaceConfiguratorDraftItem(items, {
        componentId: component.id,
        componentTypeId: component.componentTypeId,
      }),
    );
  };

  const remove = (componentId: number) => {
    commit(removeConfiguratorDraftItem(items, componentId));
  };

  const clear = () => {
    commit([]);
  };

  const slots: ConfiguratorDraftSlot[] = items.map((item, index) => {
    const query = componentQueries[index];
    return {
      item,
      component: query?.data ?? null,
      status: query?.isPending ? 'loading' : query?.error ? 'error' : 'ready',
      retry: () => {
        void query?.refetch();
      },
    };
  });

  return {
    items,
    slots,
    updatedAt,
    readStatus,
    persistenceAvailable,
    add,
    replace,
    remove,
    clear,
  };
}
