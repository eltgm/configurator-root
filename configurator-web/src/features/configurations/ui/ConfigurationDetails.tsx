import { Badge, Button, Group, Paper, SimpleGrid, Stack, Text, Title } from '@mantine/core';
import { IconArchive, IconArrowLeft, IconEdit } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import type { Configuration } from '@/shared/api';
import { PageHeader } from '@/shared/ui';

interface ConfigurationDetailsProps {
  configuration: Configuration;
  domainName: string;
}

export function ConfigurationDetails({ configuration, domainName }: ConfigurationDetailsProps) {
  const { t, i18n } = useTranslation();
  const createdAt = new Intl.DateTimeFormat(i18n.resolvedLanguage, {
    dateStyle: 'long',
    timeStyle: 'short',
  }).format(new Date(configuration.createdAt));

  return (
    <Stack gap="xl">
      <PageHeader
        title={configuration.name}
        description={t('configurations.detail.subtitle', { domain: domainName })}
        actions={
          <Group gap="sm">
            <Button
              component={Link}
              to="/configurations"
              variant="default"
              leftSection={<IconArrowLeft size={16} aria-hidden="true" />}
            >
              {t('configurations.actions.backToList')}
            </Button>
            <Button
              component={Link}
              to={`/configurations/${configuration.id}/edit`}
              leftSection={<IconEdit size={16} aria-hidden="true" />}
            >
              {t('configurations.actions.edit')}
            </Button>
          </Group>
        }
      />

      <SimpleGrid cols={{ base: 1, md: 2 }} spacing="md">
        <Paper p="lg" withBorder>
          <Stack gap="xs">
            <Title order={2} size="h3">
              {t('configurations.detail.about')}
            </Title>
            <Text size="sm" c="dimmed">
              {t('configurations.card.createdAt', { date: createdAt })}
            </Text>
            {configuration.description ? (
              <Text>{configuration.description}</Text>
            ) : (
              <Text c="dimmed">{t('configurations.card.noDescription')}</Text>
            )}
          </Stack>
        </Paper>

        <Paper p="lg" withBorder>
          <Stack gap="md">
            <Title order={2} size="h3">
              {t('configurations.form.composition')}
            </Title>
            <Text size="sm" c="dimmed">
              {t('configurations.components.count', {
                count: configuration.components.length,
              })}
            </Text>
            {configuration.components.map((component) => (
              <Paper key={component.id} p="sm" bg="var(--mantine-color-default-hover)">
                <Group justify="space-between" align="flex-start" wrap="nowrap">
                  <Stack gap={2} miw={0}>
                    <Text component={Link} to={`/components/${component.id}`} fw={600}>
                      {component.name}
                    </Text>
                    <Text size="sm" c="dimmed">
                      {[component.componentTypeName, component.brand].filter(Boolean).join(' · ')}
                    </Text>
                  </Stack>
                  {component.archived ? (
                    <Badge color="gray" leftSection={<IconArchive size={12} aria-hidden="true" />}>
                      {t('configurations.components.archived')}
                    </Badge>
                  ) : null}
                </Group>
              </Paper>
            ))}
          </Stack>
        </Paper>
      </SimpleGrid>
    </Stack>
  );
}
