import { useMediaQuery } from '@mantine/hooks';
import {
  Alert,
  Badge,
  Button,
  Collapse,
  Group,
  NumberInput,
  Paper,
  Skeleton,
  Stack,
  Text,
  Title,
} from '@mantine/core';
import { IconAlertTriangle, IconArrowsExchange, IconRefresh, IconTrash } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { useState } from 'react';

import { ComponentThumbnail } from '@/features/components/ui/ComponentThumbnail';
import type { ConfiguratorDraftSlot } from '@/features/configurator/model/use-configurator-draft';
import type { ConfiguratorPairResult } from '@/features/configurator/model/configurator-compatibility';
import {
  AssemblyCompatibilityStatus,
  type AssemblyCompatibilityState,
} from '@/features/configurator/ui/AssemblyCompatibilityStatus';
import {
  CompatibilityExplanationDrawer,
  type CompatibilityExplanationGroup,
} from '@/features/configurator/ui/CompatibilityExplanationDrawer';
import type { ComponentType } from '@/shared/api';
import { EmptyState } from '@/shared/ui';

import classes from './configurator-workspace.module.css';

interface CurrentAssemblyProps {
  domainId: number;
  slots: ReadonlyArray<ConfiguratorDraftSlot>;
  componentTypes: ReadonlyArray<ComponentType>;
  compatibilityState: AssemblyCompatibilityState;
  conflictComponentIds: ReadonlySet<number>;
  conflictCount: number;
  pairResults: ReadonlyArray<ConfiguratorPairResult>;
  trackInventory: boolean;
  onRetryCompatibility: () => void;
  onReplace: (slot: ConfiguratorDraftSlot) => void;
  onQuantityChange: (componentId: number, quantity: number) => void;
  onRemove: (componentId: number) => void;
  onClear: () => void;
  canSave: boolean;
  saveUnavailableReason?: string;
  onSave: () => void;
}

export function CurrentAssembly({
  domainId,
  slots,
  componentTypes,
  compatibilityState,
  conflictComponentIds,
  conflictCount,
  pairResults,
  trackInventory,
  onRetryCompatibility,
  onReplace,
  onQuantityChange,
  onRemove,
  onClear,
  canSave,
  saveUnavailableReason,
  onSave,
}: CurrentAssemblyProps) {
  const { t } = useTranslation();
  const mobile = useMediaQuery('(max-width: 62em)');
  const [expanded, setExpanded] = useState(false);
  const [detailsOpened, setDetailsOpened] = useState(false);
  const typeNames = new Map(componentTypes.map((type) => [type.id, type.name]));
  const componentNames = new Map(
    slots.map((slot) => [
      slot.item.componentId,
      slot.component?.name ??
        t('configurator.explanations.path.unknownComponent', { id: slot.item.componentId }),
    ]),
  );
  const explanationGroups: CompatibilityExplanationGroup[] = pairResults.map((pair) => ({
    key: `${pair.leftComponentId}:${pair.rightComponentId}`,
    title: t('configurator.explanations.pairTitle', {
      left: componentNames.get(pair.leftComponentId),
      right: componentNames.get(pair.rightComponentId),
    }),
    relation: pair.relation,
    explanations: pair.explanations,
    ...(pair.blockingRules ? { blockingRules: pair.blockingRules } : {}),
  }));

  return (
    <>
      <Paper
        component="section"
        aria-labelledby="current-assembly-title"
        className={classes.assembly}
        p="lg"
        withBorder
      >
        <Stack gap="lg">
          <Group justify="space-between" align="flex-start" wrap="wrap">
            <Stack gap={4}>
              <Title id="current-assembly-title" tabIndex={-1} order={2} size="h3">
                {t('configurator.assembly.title')}
              </Title>
              <Text size="sm" c="dimmed">
                {t('configurator.assembly.count', { count: slots.length })}
              </Text>
            </Stack>
            {slots.length > 0 ? (
              <Group gap="xs" justify="flex-end">
                <Button size="xs" disabled={!canSave} onClick={onSave}>
                  {t('configurations.actions.save')}
                </Button>
                <Button size="xs" variant="subtle" color="red" onClick={onClear}>
                  {t('configurator.assembly.clear')}
                </Button>
              </Group>
            ) : null}
          </Group>

          {slots.length > 0 && saveUnavailableReason ? (
            <Text size="xs" c="dimmed" ta="right">
              {saveUnavailableReason}
            </Text>
          ) : null}

          <AssemblyCompatibilityStatus
            state={compatibilityState}
            conflictCount={conflictCount}
            onRetry={onRetryCompatibility}
            {...(pairResults.length > 0 ? { onShowDetails: () => setDetailsOpened(true) } : {})}
          />

          {mobile && slots.length > 0 ? (
            <Button
              variant="default"
              aria-expanded={expanded}
              aria-controls="assembly-items"
              onClick={() => setExpanded((value) => !value)}
            >
              {expanded ? t('ux.less') : t('ux.assembly', { count: slots.length })}
            </Button>
          ) : null}
          <Collapse id="assembly-items" expanded={!mobile || expanded || slots.length === 0}>
            {slots.length === 0 ? (
              <EmptyState
                title={t('configurator.assembly.emptyTitle')}
                description={t('configurator.assembly.emptyDescription')}
              />
            ) : (
              <Stack gap="sm">
                {slots.map((slot) => {
                  const typeName =
                    typeNames.get(slot.item.componentTypeId) ??
                    t('configurator.assembly.unknownType', { id: slot.item.componentTypeId });
                  if (slot.status === 'loading') {
                    return (
                      <Paper key={slot.item.componentId} p="sm" withBorder>
                        <Group wrap="nowrap">
                          <Skeleton height={56} width={56} radius="md" />
                          <Stack gap={6} flex={1}>
                            <Skeleton height={14} width="45%" />
                            <Skeleton height={12} width="70%" />
                          </Stack>
                        </Group>
                      </Paper>
                    );
                  }
                  if (slot.status === 'error' || !slot.component) {
                    return (
                      <Alert
                        key={slot.item.componentId}
                        color="orange"
                        icon={<IconAlertTriangle aria-hidden="true" />}
                        title={typeName}
                      >
                        <Stack gap="sm">
                          <Text size="sm">
                            {t('configurator.assembly.unavailable', { id: slot.item.componentId })}
                          </Text>
                          <Group gap="xs">
                            <Button
                              size="xs"
                              variant="light"
                              leftSection={<IconRefresh size={14} />}
                              onClick={slot.retry}
                            >
                              {t('configurator.assembly.retry')}
                            </Button>
                            <Button
                              size="xs"
                              variant="subtle"
                              color="red"
                              onClick={() => {
                                onRemove(slot.item.componentId);
                                requestAnimationFrame(() =>
                                  document.getElementById('current-assembly-title')?.focus(),
                                );
                              }}
                            >
                              {t('configurator.assembly.remove')}
                            </Button>
                          </Group>
                        </Stack>
                      </Alert>
                    );
                  }
                  const component = slot.component;
                  const inventoryUnavailable =
                    trackInventory && slot.item.quantity > component.availableQuantity;
                  return (
                    <Paper
                      key={component.id}
                      className={classes['assembly-card']}
                      p="sm"
                      withBorder
                    >
                      <Group align="center" wrap="nowrap">
                        <div className={classes.preview}>
                          <ComponentThumbnail image={component.primaryImage} />
                        </div>
                        <Stack gap={3} flex={1} miw={0}>
                          <Group gap="xs" wrap="nowrap">
                            <Text
                              component={Link}
                              to={`/components/${component.id}`}
                              fw={650}
                              truncate
                            >
                              {component.name}
                            </Text>
                            {component.archived ? (
                              <Badge color="gray" size="xs">
                                {t('configurator.assembly.archived')}
                              </Badge>
                            ) : null}
                            {conflictComponentIds.has(component.id) ? (
                              <Badge
                                color={compatibilityState === 'disconnected' ? 'orange' : 'red'}
                                size="xs"
                              >
                                {t(
                                  compatibilityState === 'disconnected'
                                    ? 'configurator.assembly.disconnected'
                                    : 'configurator.assembly.conflict',
                                )}
                              </Badge>
                            ) : null}
                          </Group>
                          <Text size="xs" c="dimmed" truncate>
                            {[component.brand, typeName].filter(Boolean).join(' · ')}
                          </Text>
                          {trackInventory ? (
                            <Text size="xs" c={inventoryUnavailable ? 'red' : 'teal'} fw={500}>
                              {t('configurator.inventory.selectedAvailability', {
                                available: component.availableQuantity,
                                selected: slot.item.quantity,
                              })}
                            </Text>
                          ) : null}
                        </Stack>
                        {trackInventory ? (
                          <NumberInput
                            aria-label={t('configurator.assembly.quantityNamed', {
                              name: component.name,
                            })}
                            min={1}
                            max={Math.max(1, component.availableQuantity)}
                            allowDecimal={false}
                            allowNegative={false}
                            value={slot.item.quantity}
                            error={inventoryUnavailable}
                            disabled={component.availableQuantity === 0}
                            w={92}
                            size="xs"
                            onChange={(value) => {
                              if (typeof value === 'number' && Number.isInteger(value)) {
                                onQuantityChange(component.id, value);
                              }
                            }}
                          />
                        ) : null}
                        <Group gap={2} wrap="nowrap">
                          {!component.archived ? (
                            <Button
                              size="compact-sm"
                              variant="subtle"
                              aria-label={t('configurator.assembly.replaceNamed', {
                                name: component.name,
                              })}
                              onClick={() => onReplace(slot)}
                            >
                              <IconArrowsExchange size={17} aria-hidden="true" />
                            </Button>
                          ) : null}
                          <Button
                            size="compact-sm"
                            variant="subtle"
                            color="red"
                            aria-label={t('configurator.assembly.removeNamed', {
                              name: component.name,
                            })}
                            onClick={() => {
                              onRemove(component.id);
                              requestAnimationFrame(() =>
                                document.getElementById('current-assembly-title')?.focus(),
                              );
                            }}
                          >
                            <IconTrash size={17} aria-hidden="true" />
                          </Button>
                        </Group>
                      </Group>
                    </Paper>
                  );
                })}
              </Stack>
            )}
          </Collapse>
        </Stack>
      </Paper>
      <CompatibilityExplanationDrawer
        opened={detailsOpened}
        onClose={() => setDetailsOpened(false)}
        domainId={domainId}
        title={t('configurator.explanations.assemblyTitle')}
        groups={explanationGroups}
      />
    </>
  );
}
