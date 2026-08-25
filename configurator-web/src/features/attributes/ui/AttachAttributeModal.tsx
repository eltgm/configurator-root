import { Button, Group, Modal, NumberInput, Select, Stack, Switch } from '@mantine/core';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';

import {
  useAttachAttributeMutation,
  useAttributeCatalogQuery,
} from '@/features/attributes/api/attributes';
import type { AttributeDefinition } from '@/shared/api';
import { showSuccessNotification } from '@/shared/notifications/notifications';

interface AttachAttributeModalProps {
  opened: boolean;
  domainId: number;
  componentTypeId: number;
  linkedAttributes: ReadonlyArray<AttributeDefinition>;
  onClose: () => void;
}

export function AttachAttributeModal({
  opened,
  domainId,
  componentTypeId,
  linkedAttributes,
  onClose,
}: AttachAttributeModalProps) {
  const { t } = useTranslation();
  const catalogQuery = useAttributeCatalogQuery(domainId);
  const attachAttribute = useAttachAttributeMutation();
  const [attributeId, setAttributeId] = useState<string | null>(null);
  const [isRequired, setIsRequired] = useState(false);
  const [orderIndex, setOrderIndex] = useState<number | string>('');
  const linkedNames = useMemo(
    () => new Set(linkedAttributes.map((attribute) => attribute.name)),
    [linkedAttributes],
  );
  const available = (catalogQuery.data ?? []).filter(
    (attribute) =>
      !attribute.componentTypeIds?.includes(componentTypeId) && !linkedNames.has(attribute.name),
  );

  const close = () => {
    if (!attachAttribute.isPending) {
      setAttributeId(null);
      setIsRequired(false);
      setOrderIndex('');
      onClose();
    }
  };

  const submit = async () => {
    if (!attributeId) {
      return;
    }
    try {
      await attachAttribute.mutateAsync({
        domainId,
        componentTypeId,
        attributeId: Number(attributeId),
        body: {
          isRequired,
          ...(typeof orderIndex === 'number' ? { orderIndex } : {}),
        },
      });
      showSuccessNotification(t('attributes.notifications.attached'));
      setAttributeId(null);
      setIsRequired(false);
      setOrderIndex('');
      onClose();
    } catch {
      // The global mutation policy presents the structured API error.
    }
  };

  return (
    <Modal
      opened={opened}
      onClose={close}
      title={t('attributes.attach.title')}
      centered
      closeOnClickOutside={!attachAttribute.isPending}
      closeOnEscape={!attachAttribute.isPending}
    >
      <Stack gap="md">
        <Select
          label={t('attributes.attach.attribute')}
          placeholder={t('attributes.attach.placeholder')}
          data={available.map((attribute) => ({
            value: String(attribute.id),
            label: `${attribute.label} (${attribute.name})`,
          }))}
          value={attributeId}
          onChange={setAttributeId}
          searchable
          nothingFoundMessage={t('attributes.attach.empty')}
          disabled={catalogQuery.isPending}
          withAsterisk
        />
        <Switch
          label={t('attributes.form.isRequired')}
          checked={isRequired}
          onChange={(event) => setIsRequired(event.currentTarget.checked)}
        />
        <NumberInput
          label={t('attributes.form.orderIndex')}
          description={t('attributes.form.orderIndexHint')}
          min={0}
          allowDecimal={false}
          allowNegative={false}
          value={orderIndex}
          onChange={setOrderIndex}
        />
        <Group justify="flex-end">
          <Button variant="default" onClick={close} disabled={attachAttribute.isPending}>
            {t('common.cancel')}
          </Button>
          <Button
            onClick={() => void submit()}
            disabled={!attributeId}
            loading={attachAttribute.isPending}
          >
            {t('attributes.actions.attach')}
          </Button>
        </Group>
      </Stack>
    </Modal>
  );
}
