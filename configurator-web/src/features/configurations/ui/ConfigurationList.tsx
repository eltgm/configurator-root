import { Badge, Group, Paper, SimpleGrid, Stack, Text, Title } from '@mantine/core';
import { IconArchive } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import type { Configuration } from '@/shared/api';

interface ConfigurationListProps {
  configurations: ReadonlyArray<Configuration>;
}

export function ConfigurationList({ configurations }: ConfigurationListProps) {
  const { t, i18n } = useTranslation();
  const dateFormatter = new Intl.DateTimeFormat(i18n.resolvedLanguage, {
    dateStyle: 'long',
    timeStyle: 'short',
  });

  return (
    <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
      {configurations.map((configuration) => (
        <Paper component="article" key={configuration.id} p="lg" withBorder>
          <Stack gap="md">
            <Stack gap={4}>
              <Title order={2} size="h3">
                {configuration.name}
              </Title>
              <Text size="xs" c="dimmed">
                {t('configurations.card.createdAt', {
                  date: dateFormatter.format(new Date(configuration.createdAt)),
                })}
              </Text>
              {configuration.description ? (
                <Text size="sm">{configuration.description}</Text>
              ) : (
                <Text size="sm" c="dimmed">
                  {t('configurations.card.noDescription')}
                </Text>
              )}
            </Stack>
            <Stack gap="xs">
              <Text size="sm" fw={600}>
                {t('configurations.components.count', { count: configuration.components.length })}
              </Text>
              {configuration.components.map((component) => (
                <Paper key={component.id} p="xs" bg="var(--mantine-color-default-hover)">
                  <Group justify="space-between" align="flex-start" wrap="nowrap">
                    <Stack gap={2} miw={0}>
                      <Text component={Link} to={`/components/${component.id}`} size="sm" fw={600}>
                        {component.name}
                      </Text>
                      <Text size="xs" c="dimmed">
                        {[component.componentTypeName, component.brand].filter(Boolean).join(' · ')}
                      </Text>
                    </Stack>
                    {component.archived ? (
                      <Badge
                        color="gray"
                        size="sm"
                        leftSection={<IconArchive size={12} aria-hidden="true" />}
                      >
                        {t('configurations.components.archived')}
                      </Badge>
                    ) : null}
                  </Group>
                </Paper>
              ))}
            </Stack>
          </Stack>
        </Paper>
      ))}
    </SimpleGrid>
  );
}
