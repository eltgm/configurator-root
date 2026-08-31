import { Badge, Button, Group, Paper, Stack, Text } from '@mantine/core';
import { IconInfoCircle, IconPlus } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import { ComponentThumbnail } from '@/features/components/ui/ComponentThumbnail';
import type {
  ConfiguratorCandidate,
  ConfiguratorComponentSelection,
} from '@/features/configurator/model/configurator-compatibility';
import type { Component } from '@/shared/api';

import classes from './configurator-workspace.module.css';

export interface ConfiguratorBrowserCardComponent extends ConfiguratorComponentSelection {
  componentTypeName?: string;
  primaryImage?: Component['primaryImage'];
  relation?: ConfiguratorCandidate['relation'];
  compatibilityByBase?: ConfiguratorCandidate['compatibilityByBase'];
  explanations?: ConfiguratorCandidate['explanations'];
}

interface ConfiguratorCandidateCardProps {
  component: ConfiguratorBrowserCardComponent;
  componentTypeName?: string;
  catalogMode: boolean;
  replacementMode: boolean;
  onSelect: (component: ConfiguratorComponentSelection) => void;
  onExplain?: (component: ConfiguratorCandidate) => void;
}

export function ConfiguratorCandidateCard({
  component,
  componentTypeName,
  catalogMode,
  replacementMode,
  onSelect,
  onExplain,
}: ConfiguratorCandidateCardProps) {
  const { t } = useTranslation();

  return (
    <Paper className={classes['browser-card']} p="sm" withBorder>
      <Group align="stretch" wrap="nowrap">
        <div className={classes.preview}>
          <ComponentThumbnail image={component.primaryImage} />
        </div>
        <Stack gap={7} flex={1} miw={0}>
          <Stack gap={2}>
            <Text component={Link} to={`/components/${component.id}`} fw={650} truncate>
              {component.name}
            </Text>
            <Text size="xs" c="dimmed" truncate>
              {[component.brand, component.componentTypeName ?? componentTypeName]
                .filter(Boolean)
                .join(' · ')}
            </Text>
          </Stack>
          <Group justify="space-between" align="center" mt="auto" wrap="wrap">
            {!catalogMode ? (
              <Group gap={5}>
                <Badge
                  color={component.relation === 'transitive' ? 'violet' : 'green'}
                  variant="light"
                >
                  {component.relation === 'transitive'
                    ? t('configurator.browser.transitive')
                    : t('configurator.browser.direct')}
                </Badge>
                {[...new Set(component.explanations?.map((item) => item.source) ?? [])].map(
                  (source) => (
                    <Badge key={source} size="xs" variant="outline">
                      {t(`configurator.explanations.sources.${source}`)}
                    </Badge>
                  ),
                )}
              </Group>
            ) : (
              <span />
            )}
            <Group gap="xs">
              {!catalogMode && component.compatibilityByBase && onExplain ? (
                <Button
                  size="xs"
                  variant="subtle"
                  leftSection={<IconInfoCircle size={14} />}
                  onClick={() => onExplain(component as ConfiguratorCandidate)}
                >
                  {t('configurator.explanations.whyCompatible')}
                </Button>
              ) : null}
              <Button
                size="xs"
                leftSection={<IconPlus size={14} />}
                aria-label={
                  replacementMode
                    ? t('configurator.browser.selectReplacementNamed', { name: component.name })
                    : undefined
                }
                onClick={() => onSelect(component)}
              >
                {replacementMode
                  ? t('configurator.browser.selectReplacement')
                  : t('configurator.browser.add')}
              </Button>
            </Group>
          </Group>
        </Stack>
      </Group>
    </Paper>
  );
}
