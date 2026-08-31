import { Alert, Badge, Button, Group, Paper, Stack, Text, Title } from '@mantine/core';
import { IconAlertTriangle, IconArchive, IconRefresh, IconTrash } from '@tabler/icons-react';
import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';

import type {
  ConfigurationEditorBlockReason,
  ConfigurationEditorComponent,
  ConfigurationEditorEligibility,
} from '@/features/configurations/model/configuration-editor';

import classes from './configuration-editor.module.css';

interface ConfigurationAssemblyEditorProps {
  components: ReadonlyArray<ConfigurationEditorComponent>;
  eligibility: ConfigurationEditorEligibility;
  replacementComponentId: number | null;
  onRemove: (componentId: number) => void;
  onReplace: (componentId: number) => void;
  onReplaceButtonRef: (componentId: number, element: HTMLButtonElement | null) => void;
  actions: ReactNode;
}

function eligibilityKey(reason: ConfigurationEditorBlockReason) {
  return `configurations.editor.validation.${reason}`;
}

export function ConfigurationAssemblyEditor({
  components,
  eligibility,
  replacementComponentId,
  onRemove,
  onReplace,
  onReplaceButtonRef,
  actions,
}: ConfigurationAssemblyEditorProps) {
  const { t } = useTranslation();

  return (
    <Paper
      component="section"
      aria-labelledby="configuration-composition-title"
      className={classes.assembly}
      p="lg"
      withBorder
    >
      <Stack gap="md" className={classes['assembly-header']}>
        <Stack gap={2}>
          <Title id="configuration-composition-title" order={2} size="h3">
            {t('configurations.editor.compositionTitle')}
          </Title>
          <Text size="sm" c="dimmed">
            {t('configurations.components.count', { count: components.length })}
          </Text>
        </Stack>

        {eligibility.allowed ? (
          <Alert color="green" role="status">
            {t('configurations.editor.validation.ready')}
          </Alert>
        ) : (
          <Alert
            color={eligibility.reason === 'pending' ? 'blue' : 'orange'}
            icon={<IconAlertTriangle aria-hidden="true" />}
            role="status"
          >
            {t(eligibilityKey(eligibility.reason))}
          </Alert>
        )}
      </Stack>

      <Stack gap="sm" className={classes['component-list']}>
        {components.length === 0 ? (
          <Text c="dimmed">{t('configurations.editor.emptyComposition')}</Text>
        ) : null}

        {components.map((component) => (
          <Paper
            key={component.id}
            className={classes.component}
            p="sm"
            withBorder
            data-replacement-target={replacementComponentId === component.id || undefined}
          >
            <Group justify="space-between" align="flex-start" wrap="wrap">
              <Stack gap={2} miw={0}>
                <Group gap="xs">
                  <Text component={Link} to={`/components/${component.id}`} fw={600}>
                    {component.name}
                  </Text>
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
                <Text size="sm" c="dimmed">
                  {[component.componentTypeName, component.brand].filter(Boolean).join(' · ')}
                </Text>
              </Stack>
              <Group gap="xs">
                <Button
                  ref={(element) => onReplaceButtonRef(component.id, element)}
                  size="xs"
                  aria-pressed={replacementComponentId === component.id}
                  aria-controls="available-components-browser"
                  variant={replacementComponentId === component.id ? 'filled' : 'light'}
                  leftSection={<IconRefresh size={14} aria-hidden="true" />}
                  onClick={() => onReplace(component.id)}
                >
                  {t('configurations.editor.replace')}
                </Button>
                <Button
                  size="xs"
                  variant="subtle"
                  color="red"
                  leftSection={<IconTrash size={14} aria-hidden="true" />}
                  onClick={() => onRemove(component.id)}
                >
                  {t('configurations.editor.remove')}
                </Button>
              </Group>
            </Group>
          </Paper>
        ))}
      </Stack>
      <Stack gap="md" className={classes['assembly-actions']}>
        {actions}
      </Stack>
    </Paper>
  );
}
