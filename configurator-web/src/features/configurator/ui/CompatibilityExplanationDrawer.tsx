import {
  Alert,
  Badge,
  Button,
  Divider,
  Drawer,
  Group,
  List,
  Loader,
  Paper,
  Stack,
  Text,
} from '@mantine/core';
import { IconAlertTriangle, IconArrowRight } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';

import { useCompatibilityGraphQuery } from '@/features/compatibility/api/compatibility-graph';
import type { CompatibilityRelation } from '@/features/configurator/model/configurator-compatibility';
import type { CompatibilityExplanation } from '@/shared/api';

export interface CompatibilityExplanationGroup {
  key: string;
  title: string;
  relation: CompatibilityRelation;
  explanations: ReadonlyArray<CompatibilityExplanation>;
}

interface CompatibilityExplanationDrawerProps {
  opened: boolean;
  onClose: () => void;
  domainId: number;
  title: string;
  groups: ReadonlyArray<CompatibilityExplanationGroup>;
}

function relationColor(relation: CompatibilityRelation) {
  if (relation === 'direct') return 'green';
  if (relation === 'transitive') return 'violet';
  return 'red';
}

export function CompatibilityExplanationDrawer({
  opened,
  onClose,
  domainId,
  title,
  groups,
}: CompatibilityExplanationDrawerProps) {
  const { t } = useTranslation();
  const hasTransitivePath = groups.some((group) =>
    group.explanations.some((explanation) => explanation.source === 'TRANSITIVE'),
  );
  const graphQuery = useCompatibilityGraphQuery(domainId, opened && hasTransitivePath);
  const nodeNames = new Map(graphQuery.data?.nodes.map((node) => [node.id, node.name]) ?? []);

  return (
    <Drawer
      opened={opened}
      onClose={onClose}
      position="right"
      size="lg"
      title={title}
      overlayProps={{ backgroundOpacity: 0.45, blur: 2 }}
    >
      <Stack gap="lg">
        <Text size="sm" c="dimmed">
          {t('configurator.explanations.description')}
        </Text>
        {groups.map((group) => (
          <Paper key={group.key} p="md" withBorder>
            <Stack gap="md">
              <Group justify="space-between" align="flex-start">
                <Text fw={650}>{group.title}</Text>
                <Badge color={relationColor(group.relation)} variant="light">
                  {t(`configurator.explanations.relations.${group.relation}`)}
                </Badge>
              </Group>
              {group.explanations.length === 0 ? (
                <Text size="sm" c="dimmed">
                  {t('configurator.explanations.noEvidence')}
                </Text>
              ) : (
                group.explanations.map((explanation, index) => (
                  <Stack key={`${explanation.source}:${index}`} gap="xs">
                    {index > 0 ? <Divider /> : null}
                    <Badge
                      color={explanation.source === 'TRANSITIVE' ? 'violet' : 'blue'}
                      variant="outline"
                      w="fit-content"
                    >
                      {t(`configurator.explanations.sources.${explanation.source}`)}
                    </Badge>
                    {explanation.source === 'MANUAL' ? (
                      <Stack gap={3}>
                        <Text size="sm">
                          {explanation.comment || t('configurator.explanations.manual.noComment')}
                        </Text>
                        {explanation.linkId ? (
                          <Text size="xs" c="dimmed">
                            {t('configurator.explanations.manual.linkId', {
                              id: explanation.linkId,
                            })}
                          </Text>
                        ) : null}
                      </Stack>
                    ) : null}
                    {explanation.source === 'AUTOMATIC' ? (
                      <Stack gap="xs">
                        <Text size="sm" fw={600}>
                          {explanation.ruleSetName ||
                            t('configurator.explanations.automatic.unknownRule', {
                              id: explanation.ruleSetId ?? '',
                            })}
                        </Text>
                        {explanation.conditions && explanation.conditions.length > 0 ? (
                          <List size="sm" spacing="xs">
                            {explanation.conditions.map((condition, conditionIndex) => (
                              <List.Item
                                key={`${condition.leftAttributeDefinitionId}:${conditionIndex}`}
                              >
                                {t('configurator.explanations.automatic.condition', {
                                  leftAttribute: condition.leftAttributeName,
                                  leftValue: condition.leftValue,
                                  operator: t(
                                    `compatibilityRules.form.operators.${condition.operator}`,
                                  ),
                                  rightAttribute: condition.rightAttributeName,
                                  rightValue: condition.rightValue,
                                })}
                              </List.Item>
                            ))}
                          </List>
                        ) : (
                          <Text size="sm" c="dimmed">
                            {t('configurator.explanations.automatic.noConditions')}
                          </Text>
                        )}
                      </Stack>
                    ) : null}
                    {explanation.source === 'TRANSITIVE' ? (
                      <Stack gap="xs">
                        {graphQuery.isPending ? (
                          <Group gap="xs">
                            <Loader size="xs" />
                            <Text size="sm">{t('configurator.explanations.path.loading')}</Text>
                          </Group>
                        ) : null}
                        {graphQuery.error ? (
                          <Alert color="orange" icon={<IconAlertTriangle aria-hidden="true" />}>
                            <Stack gap="xs">
                              <Text size="sm">{t('configurator.explanations.path.error')}</Text>
                              <Button
                                size="xs"
                                variant="light"
                                w="fit-content"
                                onClick={() => void graphQuery.refetch()}
                              >
                                {t('configurator.explanations.path.retry')}
                              </Button>
                            </Stack>
                          </Alert>
                        ) : null}
                        <Group
                          gap="xs"
                          role="list"
                          aria-label={t('configurator.explanations.path.label')}
                        >
                          {(explanation.pathComponentIds ?? []).map(
                            (componentId, pathIndex, path) => (
                              <Group
                                key={`${componentId}:${pathIndex}`}
                                gap="xs"
                                wrap="nowrap"
                                role="listitem"
                              >
                                <Text
                                  size="sm"
                                  fw={pathIndex === 0 || pathIndex === path.length - 1 ? 650 : 400}
                                >
                                  {nodeNames.get(componentId) ??
                                    t('configurator.explanations.path.unknownComponent', {
                                      id: componentId,
                                    })}
                                </Text>
                                {pathIndex < path.length - 1 ? (
                                  <IconArrowRight size={15} aria-hidden="true" />
                                ) : null}
                              </Group>
                            ),
                          )}
                        </Group>
                      </Stack>
                    ) : null}
                  </Stack>
                ))
              )}
            </Stack>
          </Paper>
        ))}
      </Stack>
    </Drawer>
  );
}
