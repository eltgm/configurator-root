import {
  Accordion,
  Alert,
  Button,
  Checkbox,
  Group,
  Modal,
  Paper,
  Stack,
  Switch,
  Text,
  VisuallyHidden,
} from '@mantine/core';
import { IconAlertTriangle, IconInfoCircle } from '@tabler/icons-react';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router-dom';

import { useComponentTypesQuery } from '@/features/component-types/api/component-types';
import { useAssemblyCandidatesQuery } from '@/features/configurator/api/configurator-compatibility';
import { configuratorDraftMaxItems } from '@/features/configurator/model/configurator-draft';
import {
  replacementBaseComponentIds,
  type ConfiguratorComponentSelection,
  validationFromAssemblyResponse,
} from '@/features/configurator/model/configurator-compatibility';
import { useConfiguratorDraft } from '@/features/configurator/model/use-configurator-draft';
import { AvailableComponentBrowser } from '@/features/configurator/ui/AvailableComponentBrowser';
import { CurrentAssembly } from '@/features/configurator/ui/CurrentAssembly';
import classes from '@/features/configurator/ui/configurator-workspace.module.css';
import {
  getConfigurationSaveEligibility,
  type ConfigurationSaveBlockReason,
} from '@/features/configurations/model/configuration-create';
import {
  CreateConfigurationModal,
  type ConfigurationSummaryItem,
} from '@/features/configurations/ui/CreateConfigurationModal';
import { useDomainContext } from '@/features/domains/model/domain-context';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { ErrorState, PageHeader } from '@/shared/ui';

function ConfiguratorWorkspace({ domainId }: { domainId: number }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const componentTypesQuery = useComponentTypesQuery(domainId);
  const draft = useConfiguratorDraft(domainId);
  const [replacementTarget, setReplacementTarget] = useState<ConfiguratorComponentSelection>();
  const [clearRequested, setClearRequested] = useState(false);
  const [message, setMessage] = useState('');
  const [includeTransitive, setIncludeTransitive] = useState(false);
  const [saveSnapshot, setSaveSnapshot] = useState<{
    componentItems: Array<{ componentId: number; quantity: number }>;
    components: Array<ConfigurationSummaryItem>;
  }>();
  const componentIds = draft.items.map((item) => item.componentId);
  const hydratedDraftReady =
    draft.slots.length === draft.items.length &&
    draft.slots.every(
      (slot) => slot.status === 'ready' && slot.component && !slot.component.archived,
    );
  const assemblyQuery = useAssemblyCandidatesQuery(
    domainId,
    componentIds,
    hydratedDraftReady && componentIds.length > 0,
  );
  const validation = assemblyQuery.data
    ? validationFromAssemblyResponse(assemblyQuery.data)
    : undefined;
  const compatibilityState =
    componentIds.length === 0
      ? ('empty' as const)
      : !hydratedDraftReady
        ? ('blocked' as const)
        : assemblyQuery.isPending
          ? ('pending' as const)
          : assemblyQuery.error
            ? ('error' as const)
            : validation?.assemblyStatus === 'VALID'
              ? ('valid' as const)
              : validation?.assemblyStatus === 'BLOCKED'
                ? ('conflict' as const)
                : ('disconnected' as const);
  const baseComponentIds = replacementBaseComponentIds(componentIds, replacementTarget?.id ?? null);
  const baseComponentNames = new Map(
    draft.slots.map((slot) => [
      slot.item.componentId,
      slot.component?.name ??
        t('configurator.explanations.path.unknownComponent', { id: slot.item.componentId }),
    ]),
  );
  const replacementBasesReady = replacementTarget
    ? draft.slots
        .filter((slot) => slot.item.componentId !== replacementTarget.id)
        .every((slot) => slot.status === 'ready' && slot.component && !slot.component.archived)
    : false;
  const compatibilityBlocked = replacementTarget ? !replacementBasesReady : !hydratedDraftReady;
  const inventoryValid =
    !draft.trackInventory ||
    draft.slots.every(
      (slot) =>
        slot.status !== 'ready' ||
        !slot.component ||
        slot.item.quantity <= slot.component.availableQuantity,
    );
  const saveEligibility = getConfigurationSaveEligibility(
    componentIds.length,
    compatibilityState,
    inventoryValid,
  );

  const getSaveUnavailableReason = (
    reason: Exclude<ConfigurationSaveBlockReason, 'empty'>,
  ): string => {
    switch (reason) {
      case 'pending':
        return t('configurations.save.unavailable.pending');
      case 'conflict':
        return t('configurations.save.unavailable.conflict');
      case 'disconnected':
        return t('configurations.save.unavailable.disconnected');
      case 'blocked':
        return t('configurations.save.unavailable.blocked');
      case 'inventory':
        return t('configurations.save.unavailable.inventory');
      case 'error':
        return t('configurations.save.unavailable.error');
    }
  };

  const selectComponent = (component: ConfiguratorComponentSelection) => {
    if (replacementTarget) {
      draft.replace(replacementTarget.id, component);
      setReplacementTarget(undefined);
      setMessage(t('configurator.feedback.replaced', { name: component.name }));
      return;
    }
    const result = draft.add(component);
    switch (result.status) {
      case 'added':
        setMessage(t('configurator.feedback.added', { name: component.name }));
        break;
      case 'quantity-increased':
        setMessage(t('configurator.feedback.quantityIncreased', { name: component.name }));
        break;
      case 'limit-reached':
        setMessage(t('configurator.feedback.limitReached', { count: configuratorDraftMaxItems }));
        break;
    }
  };

  const componentTypes = componentTypesQuery.data ?? [];
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
      <Paper p="md" withBorder>
        <Stack gap="md">
          <Checkbox
            checked={draft.trackInventory}
            label={t('configurator.inventory.track')}
            description={t('configurator.inventory.trackDescription')}
            onChange={(event) => {
              draft.setTrackInventory(event.currentTarget.checked);
              setMessage(
                t(
                  event.currentTarget.checked
                    ? 'configurator.inventory.enabledAnnouncement'
                    : 'configurator.inventory.disabledAnnouncement',
                ),
              );
            }}
          />
          <Accordion variant="default">
            <Accordion.Item value="advanced">
              <Accordion.Control>{t('ux.advanced')}</Accordion.Control>
              <Accordion.Panel>
                <Switch
                  checked={includeTransitive}
                  label={t('configurator.transitiveMode.label')}
                  description={t('configurator.transitiveMode.description')}
                  onChange={(event) => {
                    const enabled = event.currentTarget.checked;
                    setIncludeTransitive(enabled);
                    setMessage(
                      t(
                        enabled
                          ? 'configurator.transitiveMode.enabledAnnouncement'
                          : 'configurator.transitiveMode.disabledAnnouncement',
                      ),
                    );
                  }}
                />
              </Accordion.Panel>
            </Accordion.Item>
          </Accordion>
        </Stack>
      </Paper>
      <Group hiddenFrom="lg">
        <Button
          variant="light"
          onClick={() => {
            const heading = document.getElementById('available-components-title');
            heading?.scrollIntoView({ block: 'start' });
            heading?.focus({ preventScroll: true });
          }}
        >
          {t('ux.choose')}
        </Button>
        <Button
          variant="default"
          onClick={() => {
            const heading = document.getElementById('current-assembly-title');
            heading?.scrollIntoView({ block: 'start' });
            heading?.focus({ preventScroll: true });
          }}
        >
          {t('ux.assembly', { count: draft.items.length })}
        </Button>
      </Group>
      <div className={classes.workspace}>
        <CurrentAssembly
          domainId={domainId}
          slots={draft.slots}
          componentTypes={componentTypes}
          compatibilityState={compatibilityState}
          conflictComponentIds={validation?.conflictComponentIds ?? new Set()}
          conflictCount={validation?.conflictPairs.length ?? 0}
          pairResults={validation?.pairs ?? []}
          trackInventory={draft.trackInventory}
          onRetryCompatibility={() => void assemblyQuery.refetch()}
          onReplace={(slot) => {
            if (slot.component) {
              setReplacementTarget(slot.component);
            }
          }}
          onQuantityChange={draft.setQuantity}
          onRemove={(componentId) => {
            draft.remove(componentId);
            if (replacementTarget?.id === componentId) {
              setReplacementTarget(undefined);
            }
            setMessage(t('configurator.feedback.removed'));
          }}
          onClear={() => setClearRequested(true)}
          canSave={saveEligibility.allowed}
          {...(saveEligibility.allowed || saveEligibility.reason === 'empty'
            ? {}
            : { saveUnavailableReason: getSaveUnavailableReason(saveEligibility.reason) })}
          onSave={() => {
            if (!saveEligibility.allowed) {
              return;
            }
            const typeNames = new Map(componentTypes.map((type) => [type.id, type.name]));
            setSaveSnapshot({
              componentItems: draft.items.map((item) => ({
                componentId: item.componentId,
                quantity: item.quantity,
              })),
              components: draft.slots.flatMap((slot) =>
                slot.component
                  ? [
                      {
                        id: slot.component.id,
                        name: slot.component.name,
                        typeName:
                          typeNames.get(slot.item.componentTypeId) ??
                          t('configurator.assembly.unknownType', {
                            id: slot.item.componentTypeId,
                          }),
                        ...(slot.component.brand ? { brand: slot.component.brand } : {}),
                        quantity: slot.item.quantity,
                        totalQuantity: slot.component.totalQuantity,
                        allocatedQuantity: slot.component.allocatedQuantity,
                        availableQuantity: slot.component.availableQuantity,
                      },
                    ]
                  : [],
              ),
            });
          }}
        />
        <AvailableComponentBrowser
          key={domainId}
          domainId={domainId}
          componentTypes={componentTypes}
          componentTypesLoading={componentTypesQuery.isPending}
          componentTypesUnavailable={Boolean(componentTypesQuery.error)}
          selectedItems={draft.items}
          baseComponentIds={baseComponentIds}
          baseComponentNames={baseComponentNames}
          includeTransitive={includeTransitive}
          trackInventory={draft.trackInventory}
          compatibilityBlocked={compatibilityBlocked}
          {...(replacementTarget ? { replacementTarget } : {})}
          onCancelReplacement={() => {
            setReplacementTarget(undefined);
          }}
          onSelect={selectComponent}
        />
      </div>

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
                setMessage(t('configurator.feedback.cleared'));
                setClearRequested(false);
              }}
            >
              {t('configurator.clear.confirm')}
            </Button>
          </Group>
        </Stack>
      </Modal>

      {saveSnapshot ? (
        <CreateConfigurationModal
          opened
          domainId={domainId}
          componentItems={saveSnapshot.componentItems}
          components={saveSnapshot.components}
          trackInventory={draft.trackInventory}
          onClose={() => setSaveSnapshot(undefined)}
          onSaved={() => {
            draft.clear();
            setReplacementTarget(undefined);
            setSaveSnapshot(undefined);
            void navigate('/configurations');
          }}
        />
      ) : null}
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
        {t('ux.draft')}
      </Alert>
      {selectedDomainId === null ? null : (
        <ConfiguratorWorkspace key={selectedDomainId} domainId={selectedDomainId} />
      )}
    </Stack>
  );
}
