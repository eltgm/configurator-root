import { ActionIcon, Badge, Card, Group, Stack, Switch, Table, Text, Tooltip } from '@mantine/core';
import { IconArrowRight, IconPencil, IconTrash } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import { getCompatibilityTypeLabel } from '@/features/compatibility/model/compatibility-rules';
import type { CompatibilityRuleSet, ComponentType } from '@/shared/api';

import classes from './compatibility-rule-list.module.css';

interface CompatibilityRuleListProps {
  rules: CompatibilityRuleSet[];
  componentTypes: ComponentType[];
  togglingRuleId: number | undefined;
  onToggle: (rule: CompatibilityRuleSet, enabled: boolean) => void;
  onDelete: (rule: CompatibilityRuleSet) => void;
}

export function CompatibilityRuleList({
  rules,
  componentTypes,
  togglingRuleId,
  onToggle,
  onDelete,
}: CompatibilityRuleListProps) {
  const { t } = useTranslation();
  const typeLabel = (typeId: number) =>
    getCompatibilityTypeLabel(componentTypes, typeId, (id) =>
      t('compatibilityRules.list.unknownType', { id }),
    );

  return (
    <>
      <Table.ScrollContainer
        className={classes['desktop-list']}
        minWidth={760}
        data-testid="desktop-compatibility-rule-table"
      >
        <Table verticalSpacing="sm" horizontalSpacing="md" highlightOnHover withTableBorder>
          <Table.Thead>
            <Table.Tr>
              <Table.Th>{t('compatibilityRules.table.name')}</Table.Th>
              <Table.Th>{t('compatibilityRules.table.types')}</Table.Th>
              <Table.Th>{t('compatibilityRules.table.conditions')}</Table.Th>
              <Table.Th>{t('compatibilityRules.table.status')}</Table.Th>
              <Table.Th ta="right">{t('compatibilityRules.table.actions')}</Table.Th>
            </Table.Tr>
          </Table.Thead>
          <Table.Tbody>
            {rules.map((rule) => (
              <Table.Tr key={rule.id}>
                <Table.Td>
                  <Text fw={650}>{rule.name}</Text>
                </Table.Td>
                <Table.Td>
                  <Group gap="xs" wrap="nowrap">
                    <Text size="sm">{typeLabel(rule.componentTypeAId)}</Text>
                    <IconArrowRight size={15} aria-hidden="true" />
                    <Text size="sm">{typeLabel(rule.componentTypeBId)}</Text>
                  </Group>
                </Table.Td>
                <Table.Td>
                  <Badge variant="light">
                    {t('compatibilityRules.list.conditions', { count: rule.conditions.length })}
                  </Badge>
                </Table.Td>
                <Table.Td>
                  <Switch
                    checked={rule.enabled}
                    disabled={togglingRuleId === rule.id}
                    aria-label={t(
                      rule.enabled
                        ? 'compatibilityRules.actions.disableNamed'
                        : 'compatibilityRules.actions.enableNamed',
                      { name: rule.name },
                    )}
                    onChange={(event) => onToggle(rule, event.currentTarget.checked)}
                  />
                </Table.Td>
                <Table.Td>
                  <Group justify="flex-end" gap={4} wrap="nowrap">
                    <Tooltip label={t('compatibilityRules.actions.edit')}>
                      <ActionIcon
                        component={Link}
                        to={`/settings/compatibility/rules/${rule.id}/edit`}
                        variant="subtle"
                        aria-label={t('compatibilityRules.actions.editNamed', { name: rule.name })}
                      >
                        <IconPencil size={18} />
                      </ActionIcon>
                    </Tooltip>
                    <Tooltip label={t('compatibilityRules.actions.delete')}>
                      <ActionIcon
                        variant="subtle"
                        color="red"
                        aria-label={t('compatibilityRules.actions.deleteNamed', {
                          name: rule.name,
                        })}
                        onClick={() => onDelete(rule)}
                      >
                        <IconTrash size={18} />
                      </ActionIcon>
                    </Tooltip>
                  </Group>
                </Table.Td>
              </Table.Tr>
            ))}
          </Table.Tbody>
        </Table>
      </Table.ScrollContainer>

      <Stack
        className={classes['mobile-list']}
        gap="sm"
        data-testid="mobile-compatibility-rule-list"
      >
        {rules.map((rule) => (
          <Card key={rule.id} padding="md" radius="md" withBorder>
            <Stack gap="sm">
              <Group justify="space-between" align="flex-start" wrap="nowrap">
                <Stack gap={3} miw={0}>
                  <Text fw={700}>{rule.name}</Text>
                  <Text size="sm" c="dimmed">
                    {typeLabel(rule.componentTypeAId)} → {typeLabel(rule.componentTypeBId)}
                  </Text>
                </Stack>
                <Switch
                  checked={rule.enabled}
                  disabled={togglingRuleId === rule.id}
                  aria-label={t(
                    rule.enabled
                      ? 'compatibilityRules.actions.disableNamed'
                      : 'compatibilityRules.actions.enableNamed',
                    { name: rule.name },
                  )}
                  onChange={(event) => onToggle(rule, event.currentTarget.checked)}
                />
              </Group>
              <Group justify="space-between">
                <Badge variant="light">
                  {t('compatibilityRules.list.conditions', { count: rule.conditions.length })}
                </Badge>
                <Group gap={4}>
                  <ActionIcon
                    component={Link}
                    to={`/settings/compatibility/rules/${rule.id}/edit`}
                    variant="subtle"
                    aria-label={t('compatibilityRules.actions.editNamed', { name: rule.name })}
                  >
                    <IconPencil size={18} />
                  </ActionIcon>
                  <ActionIcon
                    variant="subtle"
                    color="red"
                    aria-label={t('compatibilityRules.actions.deleteNamed', { name: rule.name })}
                    onClick={() => onDelete(rule)}
                  >
                    <IconTrash size={18} />
                  </ActionIcon>
                </Group>
              </Group>
            </Stack>
          </Card>
        ))}
      </Stack>
    </>
  );
}
