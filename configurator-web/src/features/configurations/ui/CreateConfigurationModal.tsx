import { zodResolver } from '@hookform/resolvers/zod';
import {
  Badge,
  Button,
  Group,
  Modal,
  Paper,
  Stack,
  Text,
  Textarea,
  TextInput,
} from '@mantine/core';
import { useEffect, useMemo } from 'react';
import { useForm } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';

import { useCreateConfigurationMutation } from '@/features/configurations/api/configurations';
import {
  toCreateConfigurationRequest,
  type ConfigurationFormValues,
} from '@/features/configurations/model/configuration-create';
import { getFieldErrors, type Configuration } from '@/shared/api';
import { showSuccessNotification } from '@/shared/notifications/notifications';

export interface ConfigurationSummaryItem {
  id: number;
  name: string;
  typeName: string;
  brand?: string;
}

interface CreateConfigurationModalProps {
  opened: boolean;
  domainId: number;
  componentIds: ReadonlyArray<number>;
  components: ReadonlyArray<ConfigurationSummaryItem>;
  onClose: () => void;
  onSaved: (configuration: Configuration) => void;
}

export function CreateConfigurationModal({
  opened,
  domainId,
  componentIds,
  components,
  onClose,
  onSaved,
}: CreateConfigurationModalProps) {
  const { t } = useTranslation();
  const createConfiguration = useCreateConfigurationMutation();
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
  const form = useForm<ConfigurationFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { name: '', description: '' },
  });

  useEffect(() => {
    if (opened) {
      form.reset({ name: '', description: '' });
    }
  }, [form, opened]);

  const close = () => {
    if (!createConfiguration.isPending) {
      onClose();
    }
  };

  const submit = form.handleSubmit(async (values) => {
    try {
      const configuration = await createConfiguration.mutateAsync({
        domainId,
        body: toCreateConfigurationRequest(values, componentIds),
      });
      showSuccessNotification(t('configurations.notifications.created'));
      onSaved(configuration);
      onClose();
    } catch (error) {
      const fieldErrors = getFieldErrors(error);
      for (const [fieldPath, messages] of Object.entries(fieldErrors)) {
        const field = fieldPath.split('.').at(-1);
        if ((field === 'name' || field === 'description') && messages[0]) {
          form.setError(field, { message: messages[0] });
        }
      }
    }
  });

  return (
    <Modal
      opened={opened}
      onClose={close}
      title={t('configurations.form.title')}
      centered
      closeOnClickOutside={!createConfiguration.isPending}
      closeOnEscape={!createConfiguration.isPending}
    >
      <form
        onSubmit={(event) => {
          void submit(event);
        }}
        noValidate
      >
        <Stack gap="md">
          <TextInput
            label={t('configurations.form.name')}
            placeholder={t('configurations.form.namePlaceholder')}
            withAsterisk
            autoFocus
            maxLength={255}
            error={form.formState.errors.name?.message}
            {...form.register('name')}
          />
          <Textarea
            label={t('configurations.form.description')}
            placeholder={t('configurations.form.descriptionPlaceholder')}
            rows={3}
            maxLength={4000}
            error={form.formState.errors.description?.message}
            {...form.register('description')}
          />
          <Stack gap="xs">
            <Group justify="space-between">
              <Text fw={600} size="sm">
                {t('configurations.form.composition')}
              </Text>
              <Badge variant="light">
                {t('configurations.components.count', { count: components.length })}
              </Badge>
            </Group>
            {components.map((component) => (
              <Paper key={component.id} p="xs" withBorder>
                <Text size="sm" fw={600}>
                  {component.name}
                </Text>
                <Text size="xs" c="dimmed">
                  {[component.typeName, component.brand].filter(Boolean).join(' · ')}
                </Text>
              </Paper>
            ))}
          </Stack>
          <Group justify="flex-end">
            <Button variant="default" onClick={close} disabled={createConfiguration.isPending}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" loading={createConfiguration.isPending}>
              {t('configurations.actions.save')}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
