import { zodResolver } from '@hookform/resolvers/zod';
import { Button, Group, Modal, Select, Stack, Textarea } from '@mantine/core';
import { useEffect, useMemo } from 'react';
import { Controller, useForm, useWatch } from 'react-hook-form';
import { useTranslation } from 'react-i18next';
import { z } from 'zod';

import { useCreateCompatibilityLinkMutation } from '@/features/compatibility/api/manual-compatibility';
import {
  getAvailableTargetNodes,
  graphNodeOptionLabel,
  hasCompatibilityPair,
  sortGraphNodes,
} from '@/features/compatibility/model/manual-compatibility';
import {
  getFieldErrors,
  type CreateCompatibilityLinkRequest,
  type GraphResponse,
} from '@/shared/api';
import { showSuccessNotification } from '@/shared/notifications/notifications';

interface ManualCompatibilityFormValues {
  componentAId: string;
  componentBId: string;
  comment: string;
}

interface ManualCompatibilityFormModalProps {
  opened: boolean;
  domainId: number;
  graph: GraphResponse;
  onClose: () => void;
}

function toRequest(values: ManualCompatibilityFormValues): CreateCompatibilityLinkRequest {
  const comment = values.comment.trim();
  return {
    componentAId: Number(values.componentAId),
    componentBId: Number(values.componentBId),
    ...(comment ? { comment } : {}),
  };
}

export function ManualCompatibilityFormModal({
  opened,
  domainId,
  graph,
  onClose,
}: ManualCompatibilityFormModalProps) {
  const { t } = useTranslation();
  const createLink = useCreateCompatibilityLinkMutation();
  const schema = useMemo(
    () =>
      z
        .object({
          componentAId: z
            .string()
            .min(1, t('manualCompatibility.form.validation.componentRequired')),
          componentBId: z.string().min(1, t('manualCompatibility.form.validation.targetRequired')),
          comment: z
            .string()
            .trim()
            .max(1000, t('manualCompatibility.form.validation.commentTooLong')),
        })
        .superRefine((values, context) => {
          if (!values.componentAId || !values.componentBId) {
            return;
          }
          const componentAId = Number(values.componentAId);
          const componentBId = Number(values.componentBId);
          if (componentAId === componentBId) {
            context.addIssue({
              code: 'custom',
              path: ['componentBId'],
              message: t('manualCompatibility.form.validation.selfLink'),
            });
          } else if (hasCompatibilityPair(graph.edges, componentAId, componentBId)) {
            context.addIssue({
              code: 'custom',
              path: ['componentBId'],
              message: t('manualCompatibility.form.validation.duplicate'),
            });
          }
        }),
    [graph.edges, t],
  );
  const form = useForm<ManualCompatibilityFormValues>({
    resolver: zodResolver(schema),
    defaultValues: { componentAId: '', componentBId: '', comment: '' },
  });
  const componentAIdValue = useWatch({ control: form.control, name: 'componentAId' });
  const componentAId = componentAIdValue ? Number(componentAIdValue) : null;
  const sourceOptions = useMemo(
    () =>
      sortGraphNodes(graph.nodes).map((node) => ({
        value: String(node.id),
        label: graphNodeOptionLabel(node),
      })),
    [graph.nodes],
  );
  const targetOptions = useMemo(
    () =>
      getAvailableTargetNodes(graph, componentAId).map((node) => ({
        value: String(node.id),
        label: graphNodeOptionLabel(node),
      })),
    [componentAId, graph],
  );

  useEffect(() => {
    if (opened) {
      form.reset({ componentAId: '', componentBId: '', comment: '' });
    }
  }, [form, opened]);

  useEffect(() => {
    const currentTarget = form.getValues('componentBId');
    if (currentTarget && !targetOptions.some((option) => option.value === currentTarget)) {
      form.setValue('componentBId', '');
    }
  }, [form, targetOptions]);

  const close = () => {
    if (!createLink.isPending) {
      onClose();
    }
  };

  const submit = form.handleSubmit(async (values) => {
    try {
      await createLink.mutateAsync({ domainId, body: toRequest(values) });
      showSuccessNotification(t('manualCompatibility.notifications.created'));
      onClose();
    } catch (error) {
      const fieldErrors = getFieldErrors(error);
      for (const field of ['componentAId', 'componentBId', 'comment'] as const) {
        const messages = Object.entries(fieldErrors).find(([path]) => path.includes(field))?.[1];
        if (messages?.[0]) {
          form.setError(field, { message: messages[0] });
        }
      }
    }
  });

  return (
    <Modal
      opened={opened}
      onClose={close}
      title={t('manualCompatibility.form.title')}
      centered
      size="lg"
      closeOnClickOutside={!createLink.isPending}
      closeOnEscape={!createLink.isPending}
    >
      <form
        onSubmit={(event) => {
          void submit(event);
        }}
        noValidate
      >
        <Stack gap="md">
          <Controller
            name="componentAId"
            control={form.control}
            render={({ field, fieldState }) => (
              <Select
                label={t('manualCompatibility.form.component')}
                placeholder={t('manualCompatibility.form.componentPlaceholder')}
                data={sourceOptions}
                value={field.value || null}
                onChange={(value) => field.onChange(value ?? '')}
                onBlur={field.onBlur}
                searchable
                clearable
                withAsterisk
                nothingFoundMessage={t('manualCompatibility.form.nothingFound')}
                error={fieldState.error?.message}
                disabled={createLink.isPending}
              />
            )}
          />
          <Controller
            name="componentBId"
            control={form.control}
            render={({ field, fieldState }) => (
              <Select
                label={t('manualCompatibility.form.target')}
                placeholder={
                  componentAId === null
                    ? t('manualCompatibility.form.selectComponentFirst')
                    : t('manualCompatibility.form.targetPlaceholder')
                }
                description={
                  componentAId !== null && targetOptions.length === 0
                    ? t('manualCompatibility.form.noAvailableTargets')
                    : undefined
                }
                data={targetOptions}
                value={field.value || null}
                onChange={(value) => field.onChange(value ?? '')}
                onBlur={field.onBlur}
                searchable
                clearable
                withAsterisk
                nothingFoundMessage={t('manualCompatibility.form.nothingFound')}
                error={fieldState.error?.message}
                disabled={componentAId === null || createLink.isPending}
              />
            )}
          />
          <Textarea
            label={t('manualCompatibility.form.comment')}
            placeholder={t('manualCompatibility.form.commentPlaceholder')}
            description={t('manualCompatibility.form.commentHint')}
            rows={3}
            maxLength={1000}
            error={form.formState.errors.comment?.message}
            disabled={createLink.isPending}
            {...form.register('comment')}
          />
          <Group justify="flex-end">
            <Button variant="default" onClick={close} disabled={createLink.isPending}>
              {t('common.cancel')}
            </Button>
            <Button type="submit" loading={createLink.isPending}>
              {t('manualCompatibility.actions.create')}
            </Button>
          </Group>
        </Stack>
      </form>
    </Modal>
  );
}
