import { ActionIcon, Group, Paper, Stack, Table, Text, Tooltip } from '@mantine/core';
import { IconTrash } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';

import type { ManualCompatibilityLinkView } from '@/features/compatibility/model/manual-compatibility';
import type { GraphNode } from '@/shared/api';

import classes from './manual-compatibility-list.module.css';

interface ManualCompatibilityListProps {
  links: ReadonlyArray<ManualCompatibilityLinkView>;
  onDelete: (link: ManualCompatibilityLinkView) => void;
}

function ComponentSummary({ component }: { component: GraphNode }) {
  const { t } = useTranslation();
  return (
    <Stack gap={1} miw={0}>
      <Text fw={600}>{component.name}</Text>
      <Text size="xs" c="dimmed">
        {component.brand
          ? t('manualCompatibility.list.typeAndBrand', {
              type: component.componentTypeName,
              brand: component.brand,
            })
          : component.componentTypeName}
      </Text>
    </Stack>
  );
}

function DeleteAction({
  link,
  onDelete,
}: {
  link: ManualCompatibilityLinkView;
  onDelete: (link: ManualCompatibilityLinkView) => void;
}) {
  const { t } = useTranslation();
  const label = t('manualCompatibility.actions.deleteNamed', {
    first: link.componentA.name,
    second: link.componentB.name,
  });
  return (
    <Tooltip label={t('manualCompatibility.actions.delete')}>
      <ActionIcon variant="subtle" color="red" aria-label={label} onClick={() => onDelete(link)}>
        <IconTrash size={18} />
      </ActionIcon>
    </Tooltip>
  );
}

export function ManualCompatibilityList({ links, onDelete }: ManualCompatibilityListProps) {
  const { t } = useTranslation();
  return (
    <>
      <Paper
        className={classes['desktop-table']}
        data-testid="desktop-manual-compatibility-table"
        withBorder
      >
        <Table.ScrollContainer minWidth={720}>
          <Table verticalSpacing="sm" horizontalSpacing="md" highlightOnHover>
            <Table.Thead>
              <Table.Tr>
                <Table.Th>{t('manualCompatibility.table.component')}</Table.Th>
                <Table.Th>{t('manualCompatibility.table.target')}</Table.Th>
                <Table.Th>{t('manualCompatibility.table.comment')}</Table.Th>
                <Table.Th ta="right">{t('manualCompatibility.table.actions')}</Table.Th>
              </Table.Tr>
            </Table.Thead>
            <Table.Tbody>
              {links.map((link) => (
                <Table.Tr key={link.edge.id}>
                  <Table.Td>
                    <ComponentSummary component={link.componentA} />
                  </Table.Td>
                  <Table.Td>
                    <ComponentSummary component={link.componentB} />
                  </Table.Td>
                  <Table.Td>
                    <Text size="sm" {...(link.edge.comment ? {} : { c: 'dimmed' })}>
                      {link.edge.comment || t('manualCompatibility.list.noComment')}
                    </Text>
                  </Table.Td>
                  <Table.Td>
                    <Group justify="flex-end">
                      <DeleteAction link={link} onDelete={onDelete} />
                    </Group>
                  </Table.Td>
                </Table.Tr>
              ))}
            </Table.Tbody>
          </Table>
        </Table.ScrollContainer>
      </Paper>

      <Stack
        className={classes['mobile-list']}
        data-testid="mobile-manual-compatibility-list"
        gap="sm"
      >
        {links.map((link) => (
          <Paper key={link.edge.id} p="md" withBorder>
            <Stack gap="sm">
              <Group justify="space-between" align="flex-start" wrap="nowrap">
                <Stack gap="xs" miw={0}>
                  <ComponentSummary component={link.componentA} />
                  <Text size="xs" c="dimmed" fw={600}>
                    {t('manualCompatibility.list.compatibleWith')}
                  </Text>
                  <ComponentSummary component={link.componentB} />
                </Stack>
                <DeleteAction link={link} onDelete={onDelete} />
              </Group>
              {link.edge.comment ? (
                <Text size="sm">{link.edge.comment}</Text>
              ) : (
                <Text size="sm" c="dimmed">
                  {t('manualCompatibility.list.noComment')}
                </Text>
              )}
            </Stack>
          </Paper>
        ))}
      </Stack>
    </>
  );
}
