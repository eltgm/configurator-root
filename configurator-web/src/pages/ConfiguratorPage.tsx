import { Alert, Button, Group, Modal, Stack, Text, VisuallyHidden } from '@mantine/core';
import { IconAlertTriangle, IconInfoCircle } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { useComponentTypesQuery } from '@/features/component-types/api/component-types';
import { configuratorDraftMaxItems } from '@/features/configurator/model/configurator-draft';
import { useConfiguratorDraft } from '@/features/configurator/model/use-configurator-draft';
import { AvailableComponentBrowser } from '@/features/configurator/ui/AvailableComponentBrowser';
import { CurrentAssembly } from '@/features/configurator/ui/CurrentAssembly';
import classes from '@/features/configurator/ui/configurator-workspace.module.css';
import { useDomainContext } from '@/features/domains/model/domain-context';
import type { Component } from '@/shared/api';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { ErrorState, PageHeader } from '@/shared/ui';

function ConfiguratorWorkspace({ domainId }: { domainId: number }) {
  const { t } = useTranslation();
  const componentTypesQuery = useComponentTypesQuery(domainId);
  const draft = useConfiguratorDraft(domainId);
  const [replacement, setReplacement] = useState<Component>();
  const [clearRequested, setClearRequested] = useState(false);
  const [message, setMessage] = useState('');

  const selectComponent = (component: Component) => {
    const result = draft.add(component);
    switch (result.status) {
      case 'added':
        setMessage(t('configurator.feedback.added', { name: component.name }));
        break;
      case 'already-selected':
        setMessage(t('configurator.feedback.alreadySelected', { name: component.name }));
        break;
      case 'replacement-required':
        setReplacement(component);
        break;
      case 'limit-reached':
        setMessage(t('configurator.feedback.limitReached', { count: configuratorDraftMaxItems }));
        break;
    }
  };

  const componentTypes = componentTypesQuery.data ?? [];
  const replacedSlot = replacement
    ? draft.slots.find((slot) => slot.item.componentTypeId === replacement.componentTypeId)
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
          onRemove={(componentId) => {
            draft.remove(componentId);
            setMessage(t('configurator.feedback.removed'));
          }}
          onClear={() => setClearRequested(true)}
        />
        <AvailableComponentBrowser
          domainId={domainId}
          componentTypes={componentTypes}
          componentTypesLoading={componentTypesQuery.isPending}
          componentTypesUnavailable={Boolean(componentTypesQuery.error)}
          selectedItems={draft.items}
          onSelect={selectComponent}
        />
      </div>

      <Modal
        opened={Boolean(replacement)}
        onClose={() => setReplacement(undefined)}
        title={t('configurator.replace.title')}
        centered
      >
        <Stack>
          <Text>
            {t('configurator.replace.description', {
              current: replacedName,
              replacement: replacement?.name ?? '',
            })}
          </Text>
          <Text size="sm" c="dimmed">
            {t('configurator.replace.hint')}
          </Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => setReplacement(undefined)}>
              {t('common.cancel')}
            </Button>
            <Button
              onClick={() => {
                if (replacement) {
                  draft.replace(replacement);
                  setMessage(t('configurator.feedback.replaced', { name: replacement.name }));
                }
                setReplacement(undefined);
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
