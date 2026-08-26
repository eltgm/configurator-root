import {
  Alert,
  Button,
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
  const [replacementCandidate, setReplacementCandidate] =
    useState<ConfiguratorComponentSelection>();
  const [clearRequested, setClearRequested] = useState(false);
  const [message, setMessage] = useState('');
  const [includeTransitive, setIncludeTransitive] = useState(false);
  const [saveSnapshot, setSaveSnapshot] = useState<{
    componentIds: Array<number>;
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
  const saveEligibility = getConfigurationSaveEligibility(componentIds.length, compatibilityState);

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
      case 'error':
        return t('configurations.save.unavailable.error');
    }
  };

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
      <Paper p="md" withBorder>
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
      </Paper>
      <div className={classes.workspace}>
        <CurrentAssembly
          domainId={domainId}
          slots={draft.slots}
          componentTypes={componentTypes}
          compatibilityState={compatibilityState}
          conflictComponentIds={validation?.conflictComponentIds ?? new Set()}
          conflictCount={validation?.conflictPairs.length ?? 0}
          pairResults={validation?.pairs ?? []}
          onRetryCompatibility={() => void assemblyQuery.refetch()}
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
              componentIds: [...componentIds],
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
                      },
                    ]
                  : [],
              ),
            });
          }}
        />
        <AvailableComponentBrowser
          key={`${domainId}:${includeTransitive}:${replacementTarget?.id ?? 'default'}:${componentIds.join(',')}`}
          domainId={domainId}
          componentTypes={componentTypes}
          componentTypesLoading={componentTypesQuery.isPending}
          componentTypesUnavailable={Boolean(componentTypesQuery.error)}
          selectedItems={draft.items}
          baseComponentIds={baseComponentIds}
          baseComponentNames={baseComponentNames}
          includeTransitive={includeTransitive}
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

      {saveSnapshot ? (
        <CreateConfigurationModal
          opened
          domainId={domainId}
          componentIds={saveSnapshot.componentIds}
          components={saveSnapshot.components}
          onClose={() => setSaveSnapshot(undefined)}
          onSaved={() => {
            draft.clear();
            setReplacementTarget(undefined);
            setReplacementCandidate(undefined);
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
        {t('configurator.page.scopeNotice')}
      </Alert>
      {selectedDomainId === null ? null : (
        <ConfiguratorWorkspace key={selectedDomainId} domainId={selectedDomainId} />
      )}
    </Stack>
  );
}
