import { zodResolver } from '@hookform/resolvers/zod';
import {
  Button,
  Group,
  Modal,
  Paper,
  Stack,
  Text,
  Textarea,
  TextInput,
  Title,
} from '@mantine/core';
import { IconArrowLeft, IconDeviceFloppy } from '@tabler/icons-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { Link, useNavigate } from 'react-router-dom';
import { z } from 'zod';

import type { ComponentType, Configuration } from '@/shared/api';
import { getFieldErrors } from '@/shared/api/errors';
import { useAssemblyCandidatesQuery } from '@/features/configurator/api/configurator-compatibility';
import type { ConfiguratorComponentSelection } from '@/features/configurator/model/configurator-compatibility';
import { replacementBaseComponentIds } from '@/features/configurator/model/configurator-compatibility';
import { AvailableComponentBrowser } from '@/features/configurator/ui/AvailableComponentBrowser';
import { useUpdateConfigurationMutation } from '@/features/configurations/api/configurations';
import {
  addConfigurationEditorComponent,
  configurationComponentIds,
  configurationComponentsChanged,
  configurationEditorInitialComponents,
  configurationEditorInitialValues,
  getConfigurationEditorEligibility,
  removeConfigurationEditorComponent,
  replaceConfigurationEditorComponent,
  toUpdateConfigurationRequest,
  type ConfigurationEditorComponent,
  type ConfigurationEditorValidationState,
  type ConfigurationEditorValues,
} from '@/features/configurations/model/configuration-editor';
import { useUnsavedChangesGuard } from '@/features/components/model/use-unsaved-changes-guard';
import { ConfigurationAssemblyEditor } from '@/features/configurations/ui/ConfigurationAssemblyEditor';
import { useRegisterDomainChangeGuard } from '@/features/domains/model/use-domain-change-guard';
import { showSuccessNotification } from '@/shared/notifications/notifications';
import { ErrorState, PageHeader } from '@/shared/ui';

import classes from './configuration-editor.module.css';

interface ConfigurationEditorProps {
  configuration: Configuration;
  componentTypes: ReadonlyArray<ComponentType>;
}

export function ConfigurationEditor({ configuration, componentTypes }: ConfigurationEditorProps) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const schema = useMemo(
    () =>
      z.object({
        name: z
          .string()
          .trim()
          .min(1, t('configurations.form.validation.nameRequired'))
          .max(255, t('configurations.form.validation.nameTooLong')),
        description: z.string().max(4000, t('configurations.form.validation.descriptionTooLong')),
      }),
    [t],
  );
  const form = useForm<ConfigurationEditorValues>({
    resolver: zodResolver(schema),
    defaultValues: configurationEditorInitialValues(configuration),
    mode: 'onChange',
  });
  const baselineComponents = useMemo(
    () => configurationEditorInitialComponents(configuration),
    [configuration],
  );
  const [components, setComponents] = useState<ConfigurationEditorComponent[]>(baselineComponents);
  const [replacementComponentId, setReplacementComponentId] = useState<number | null>(null);
  const browserHeadingRef = useRef<HTMLHeadingElement>(null);
  const replacementButtonsRef = useRef(new Map<number, HTMLButtonElement>());
  const [focusRequest, setFocusRequest] = useState<
    { target: 'browser' } | { target: 'component'; componentId: number } | null
  >(null);

  useEffect(() => {
    if (!focusRequest) return;
    const target =
      focusRequest.target === 'browser'
        ? browserHeadingRef.current
        : replacementButtonsRef.current.get(focusRequest.componentId);
    if (!target) return;

    target.focus({ preventScroll: true });
    const behavior = window.matchMedia('(prefers-reduced-motion: reduce)').matches
      ? 'instant'
      : 'smooth';
    if (focusRequest.target === 'component') {
      target.scrollIntoView({ behavior, block: 'nearest', inline: 'nearest' });
    } else {
      const bounds = target.getBoundingClientRect();
      const style = window.getComputedStyle(document.documentElement);
      const topInset = Number.parseFloat(style.scrollPaddingTop) || 0;
      const bottomInset = Number.parseFloat(style.scrollPaddingBottom) || 0;
      if (
        window.matchMedia('(max-width: 62em)').matches ||
        bounds.top < topInset ||
        bounds.bottom > window.innerHeight - bottomInset
      ) {
        target.scrollIntoView({ behavior, block: 'start', inline: 'nearest' });
      }
    }
  }, [focusRequest]);
  const componentIds = configurationComponentIds(components);
  const hasArchivedComponents = components.some((component) => component.archived);
  const assemblyQuery = useAssemblyCandidatesQuery(
    configuration.domainId,
    componentIds,
    componentIds.length >= 2 && !hasArchivedComponents,
  );
  const validationState: ConfigurationEditorValidationState =
    componentIds.length < 2
      ? 'idle'
      : hasArchivedComponents
        ? 'idle'
        : assemblyQuery.isPending
          ? 'pending'
          : assemblyQuery.error
            ? 'error'
            : assemblyQuery.data?.assemblyStatus === 'VALID'
              ? 'valid'
              : assemblyQuery.data?.assemblyStatus === 'BLOCKED'
                ? 'blocked'
                : 'disconnected';
  const eligibility = getConfigurationEditorEligibility(components, validationState);
  const compositionDirty = configurationComponentsChanged(baselineComponents, components);
  const isDirty = form.formState.isDirty || compositionDirty;
  const updateMutation = useUpdateConfigurationMutation();
  const { blocker, allowNavigation } = useUnsavedChangesGuard(isDirty);
  useRegisterDomainChangeGuard(isDirty, allowNavigation);
  const replacementTarget = components.find((component) => component.id === replacementComponentId);
  const baseComponentIds = replacementBaseComponentIds(componentIds, replacementComponentId);
  const baseComponents = components.filter((component) => baseComponentIds.includes(component.id));
  const baseComponentNames = new Map(components.map((component) => [component.id, component.name]));
  const selectedItems = components.map((component) => ({
    componentId: component.id,
    componentTypeId: component.componentTypeId,
  }));
  const browserBlocked = replacementTarget
    ? baseComponents.some((component) => component.archived)
    : components.some((component) => component.archived) || components.length >= 50;

  const resetServerError = () => {
    if (updateMutation.error) updateMutation.reset();
  };

  const toEditorComponent = (
    component: ConfiguratorComponentSelection,
  ): ConfigurationEditorComponent => ({
    ...component,
    componentTypeName:
      componentTypes.find((type) => type.id === component.componentTypeId)?.name ??
      replacementTarget?.componentTypeName ??
      t('components.item.unknownType'),
    archived: false,
  });

  const selectComponent = (selection: ConfiguratorComponentSelection) => {
    const component = toEditorComponent(selection);
    setComponents((current) =>
      replacementTarget
        ? replaceConfigurationEditorComponent(current, replacementTarget.id, component)
        : addConfigurationEditorComponent(current, component),
    );
    setReplacementComponentId(null);
    if (replacementTarget) {
      setFocusRequest({ target: 'component', componentId: component.id });
    }
    resetServerError();
  };

  const submit = form.handleSubmit(async (values) => {
    if (!eligibility.allowed || !isDirty) return;
    try {
      const updated = await updateMutation.mutateAsync({
        domainId: configuration.domainId,
        configurationId: configuration.id,
        body: toUpdateConfigurationRequest(values, components),
      });
      form.reset(configurationEditorInitialValues(updated));
      setComponents(configurationEditorInitialComponents(updated));
      showSuccessNotification(t('configurations.notifications.updated'));
      allowNavigation();
      void navigate(`/configurations/${updated.id}`, { replace: true });
    } catch (error) {
      const fieldErrors = getFieldErrors(error);
      for (const [path, messages] of Object.entries(fieldErrors)) {
        const message = messages[0];
        if (!message) continue;
        if (path.includes('name')) form.setError('name', { message });
        if (path.includes('description')) form.setError('description', { message });
      }
    }
  });

  return (
    <>
      <Stack gap="xl">
        <PageHeader
          title={t('configurations.editor.title')}
          description={t('configurations.editor.subtitle', { name: configuration.name })}
          actions={
            <Button
              component={Link}
              to={`/configurations/${configuration.id}`}
              variant="default"
              leftSection={<IconArrowLeft size={16} aria-hidden="true" />}
            >
              {t('configurations.actions.cancelEditing')}
            </Button>
          }
        />

        <form onSubmit={(event) => void submit(event)} noValidate>
          <Stack gap="lg">
            <Paper p="lg" withBorder>
              <Stack gap="md">
                <Title order={2} size="h3">
                  {t('configurations.editor.metadataTitle')}
                </Title>
                <TextInput
                  label={t('configurations.form.name')}
                  placeholder={t('configurations.form.namePlaceholder')}
                  withAsterisk
                  maxLength={255}
                  error={form.formState.errors.name?.message}
                  {...form.register('name', { onChange: resetServerError })}
                />
                <Textarea
                  label={t('configurations.form.description')}
                  placeholder={t('configurations.form.descriptionPlaceholder')}
                  minRows={4}
                  maxLength={4000}
                  error={form.formState.errors.description?.message}
                  {...form.register('description', { onChange: resetServerError })}
                />
              </Stack>
            </Paper>

            <div className={classes.workspace}>
              <ConfigurationAssemblyEditor
                components={components}
                eligibility={eligibility}
                replacementComponentId={replacementComponentId}
                onReplace={(componentId) => {
                  setReplacementComponentId(componentId);
                  setFocusRequest({ target: 'browser' });
                  resetServerError();
                }}
                onReplaceButtonRef={(componentId, element) => {
                  if (element) replacementButtonsRef.current.set(componentId, element);
                  else replacementButtonsRef.current.delete(componentId);
                }}
                onRemove={(componentId) => {
                  setComponents((current) =>
                    removeConfigurationEditorComponent(current, componentId),
                  );
                  setReplacementComponentId((current) =>
                    current === componentId ? null : current,
                  );
                  resetServerError();
                }}
                actions={
                  <>
                    {updateMutation.error ? <ErrorState error={updateMutation.error} /> : null}
                    <Button
                      type="submit"
                      leftSection={<IconDeviceFloppy size={16} aria-hidden="true" />}
                      loading={updateMutation.isPending}
                      disabled={!eligibility.allowed || !isDirty}
                      fullWidth
                    >
                      {t('configurations.editor.save')}
                    </Button>
                  </>
                }
              />

              <div
                className={classes.browser}
                data-replacing={Boolean(replacementTarget) || undefined}
              >
                <AvailableComponentBrowser
                  key={replacementComponentId ?? 'add'}
                  headingRef={browserHeadingRef}
                  domainId={configuration.domainId}
                  componentTypes={componentTypes}
                  componentTypesLoading={false}
                  componentTypesUnavailable={false}
                  selectedItems={selectedItems}
                  baseComponentIds={baseComponentIds}
                  baseComponentNames={baseComponentNames}
                  includeTransitive={false}
                  compatibilityBlocked={browserBlocked}
                  {...(replacementTarget ? { replacementTarget } : {})}
                  onCancelReplacement={() => {
                    if (replacementComponentId !== null) {
                      setFocusRequest({ target: 'component', componentId: replacementComponentId });
                    }
                    setReplacementComponentId(null);
                  }}
                  onSelect={selectComponent}
                />
              </div>
            </div>
          </Stack>
        </form>
      </Stack>

      <Modal
        opened={blocker.state === 'blocked'}
        onClose={() => blocker.reset?.()}
        title={t('configurations.editor.unsaved.title')}
        centered
      >
        <Stack>
          <Text>{t('configurations.editor.unsaved.description')}</Text>
          <Group justify="flex-end">
            <Button variant="default" onClick={() => blocker.reset?.()}>
              {t('configurations.editor.unsaved.stay')}
            </Button>
            <Button color="red" onClick={() => blocker.proceed?.()}>
              {t('configurations.editor.unsaved.leave')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </>
  );
}
