import { Alert, Button, Group, Modal, Stack, Text, VisuallyHidden } from '@mantine/core';
import { IconAlertTriangle, IconInfoCircle } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { useComponentTypesQuery } from '@/features/component-types/api/component-types';
import { useBatchCompatibilityQuery } from '@/features/configurator/api/configurator-compatibility';
import { configuratorDraftMaxItems } from '@/features/configurator/model/configurator-draft';
import {
  replacementBaseComponentIds,
  type ConfiguratorComponentSelection,
  validateConfiguratorAssembly,
} from '@/features/configurator/model/configurator-compatibility';
import { useConfiguratorDraft } from '@/features/configurator/model/use-configurator-draft';
import { AvailableComponentBrowser } from '@/features/configurator/ui/AvailableComponentBrowser';
import { CurrentAssembly } from '@/features/configurator/ui/CurrentAssembly';
import classes from '@/features/configurator/ui/configurator-workspace.module.css';
import { useDomainContext } from '@/features/domains/model/domain-context';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { ErrorState, PageHeader } from '@/shared/ui';

function ConfiguratorWorkspace({ domainId }: { domainId: number }) {
  const { t } = useTranslation();
  const componentTypesQuery = useComponentTypesQuery(domainId);
  const draft = useConfiguratorDraft(domainId);
  const [replacementTarget, setReplacementTarget] = useState<ConfiguratorComponentSelection>();
  const [replacementCandidate, setReplacementCandidate] =
    useState<ConfiguratorComponentSelection>();
  const [clearRequested, setClearRequested] = useState(false);
  const [message, setMessage] = useState('');
  const componentIds = draft.items.map((item) => item.componentId);
  const hydratedDraftReady =
    draft.slots.length === draft.items.length &&
    draft.slots.every(
      (slot) => slot.status === 'ready' && slot.component && !slot.component.archived,
    );
  const batchQuery = useBatchCompatibilityQuery(
    domainId,
    componentIds,
    hydratedDraftReady && componentIds.length > 1,
  );
  const validation = batchQuery.data
    ? validateConfiguratorAssembly(componentIds, batchQuery.data)
    : undefined;
  const compatibilityState =
    componentIds.length === 0
      ? ('empty' as const)
      : !hydratedDraftReady
        ? ('blocked' as const)
        : componentIds.length === 1
          ? ('valid' as const)
          : batchQuery.isPending
            ? ('pending' as const)
            : batchQuery.error
              ? ('error' as const)
              : validation?.compatible
                ? ('valid' as const)
                : ('conflict' as const);
  const baseComponentIds = replacementBaseComponentIds(componentIds, replacementTarget?.id ?? null);
  const replacementBasesReady = replacementTarget
    ? draft.slots
        .filter((slot) => slot.item.componentId !== replacementTarget.id)
        .every((slot) => slot.status === 'ready' && slot.component && !slot.component.archived)
    : false;
  const compatibilityBlocked = replacementTarget
    ? !replacementBasesReady
    : compatibilityState === 'pending' ||
      compatibilityState === 'conflict' ||
      compatibilityState === 'blocked' ||
      compatibilityState === 'error';

  const selectComponent = (component: ConfiguratorComponentSelection) => {
    const result = draft.add(component);
    switch (result.status) {
      case 'added':
        setMessage(t('configurator.feedback.added', { name: component.name }));
        break;
      case 'already-selected':
        setMessage(t('configurator.feedback.alreadySelected', { name: component.name }));
        break;
      case 'replacement-required':
        setReplacementCandidate(component);
        break;
      case 'limit-reached':
        setMessage(t('configurator.feedback.limitReached', { count: configuratorDraftMaxItems }));
        break;
    }
  };

  const componentTypes = componentTypesQuery.data ?? [];
  const replacedSlot = replacementCandidate
    ? draft.slots.find((slot) => slot.item.componentTypeId === replacementCandidate.componentTypeId)
    : undefined;
  const replacedName =
    replacedSlot?.component?.name ??
    t('configurator.replace.unknownComponent', {
      id: replacedSlot?.item.componentId ?? '',
    });

  return (
    <>
      {draft.readStatus === 'invalid' ? (
        <Alert
          color="orange"
          icon={<IconAlertTriangle aria-hidden="true" />}
          title={t('configurator.storage.invalidTitle')}
        >
          {t('configurator.storage.invalidDescription')}
        </Alert>
      ) : null}
      {componentTypesQuery.error ? (
        <ErrorState
          error={componentTypesQuery.error}
          onRetry={() => void componentTypesQuery.refetch()}
        />
      ) : null}
      {!draft.persistenceAvailable ? (
        <Alert
          color="orange"
          icon={<IconAlertTriangle aria-hidden="true" />}
          title={t('configurator.storage.unavailableTitle')}
        >
          {t('configurator.storage.unavailableDescription')}
        </Alert>
      ) : null}
      {draft.readStatus === 'restored' ? (
        <Alert color="blue" icon={<IconInfoCircle aria-hidden="true" />}>
          {t('configurator.storage.restored', { count: draft.items.length })}
        </Alert>
      ) : null}
      <VisuallyHidden aria-live="polite">{message}</VisuallyHidden>
      <div className={classes.workspace}>
        <CurrentAssembly
          slots={draft.slots}
          componentTypes={componentTypes}
          compatibilityState={compatibilityState}
          conflictComponentIds={validation?.conflictComponentIds ?? new Set()}
          conflictCount={validation?.conflictPairs.length ?? 0}
          onRetryCompatibility={() => void batchQuery.refetch()}
          onReplace={(slot) => {
            if (slot.component) {
              setReplacementTarget(slot.component);
              setReplacementCandidate(undefined);
            }
          }}
          onRemove={(componentId) => {
            draft.remove(componentId);
            if (replacementTarget?.id === componentId) {
              setReplacementTarget(undefined);
              setReplacementCandidate(undefined);
            }
            setMessage(t('configurator.feedback.removed'));
          }}
          onClear={() => setClearRequested(true)}
        />
        <AvailableComponentBrowser
          key={`${domainId}:${replacementTarget?.id ?? 'default'}:${componentIds.join(',')}`}
          domainId={domainId}
          componentTypes={componentTypes}
          componentTypesLoading={componentTypesQuery.isPending}
          componentTypesUnavailable={Boolean(componentTypesQuery.error)}
          selectedItems={draft.items}
          baseComponentIds={baseComponentIds}
          compatibilityBlocked={compatibilityBlocked}
          {...(replacementTarget ? { replacementTarget } : {})}
          onCancelReplacement={() => {
            setReplacementTarget(undefined);
            setReplacementCandidate(undefined);
          }}
          onSelect={selectComponent}
        />
      </div>

      <Modal
        opened={Boolean(replacementCandidate)}
        onClose={() => setReplacementCandidate(undefined)}
        title={t('configurator.replace.title')}
        centered
      >
        <Stack>
          <Text>
            {t('configurator.replace.description', {
              current: replacedName,
              replacement: replacementCandidate?.name ?? '',
            })}
          </Text>
          <Text size="sm" c="dimmed">
            {t('configurator.replace.hint')}
          </Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setReplacementCandidate(undefined)}>
              {t('common.cancel')}
            </Button>
            <Button
              onClick={() => {
                if (replacementCandidate) {
                  draft.replace(replacementCandidate);
                  setMessage(
                    t('configurator.feedback.replaced', { name: replacementCandidate.name }),
                  );
                }
                setReplacementCandidate(undefined);
                setReplacementTarget(undefined);
              }}
            >
              {t('configurator.replace.confirm')}
            </Button>
          </Group>
        </Stack>
      </Modal>

      <Modal
        opened={clearRequested}
        onClose={() => setClearRequested(false)}
        title={t('configurator.clear.title')}
        centered
      >
        <Stack>
          <Text>{t('configurator.clear.description')}</Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setClearRequested(false)}>
              {t('common.cancel')}
            </Button>
            <Button
              color="red"
              onClick={() => {
                draft.clear();
                setReplacementTarget(undefined);
                setReplacementCandidate(undefined);
                setMessage(t('configurator.feedback.cleared'));
                setClearRequested(false);
              }}
            >
              {t('configurator.clear.confirm')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </>
  );
}

export function ConfiguratorPage() {
  const { t } = useTranslation();
  const { selectedDomain, selectedDomainId } = useDomainContext();
  const title = t('configurator.page.title');
  useDocumentTitle(title, t('app.name'));

  return (
    <Stack gap="xl">
      <PageHeader
        title={title}
        description={t('configurator.page.description', { domain: selectedDomain?.name ?? '' })}
      />
      <Alert color="blue" variant="light" icon={<IconInfoCircle aria-hidden="true" />}>
        {t('configurator.page.scopeNotice')}
      </Alert>
      {selectedDomainId === null ? null : (
        <ConfiguratorWorkspace key={selectedDomainId} domainId={selectedDomainId} />
      )}
    </Stack>
  );
}
