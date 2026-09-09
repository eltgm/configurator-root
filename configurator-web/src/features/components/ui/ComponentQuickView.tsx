import { Button, Drawer, Group, Image, Stack, Table, Text } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { useComponentQuery } from '@/features/components/api/components';
import { toComponentImageUrl } from '@/features/components/model/catalog-preferences';
import { ErrorState, LoadingState } from '@/shared/ui';

export function ComponentQuickView({
  domainId,
  componentId,
  onClose,
}: {
  domainId: number;
  componentId: number | null;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const query = useComponentQuery(domainId, componentId);
  const component = query.data;
  return (
    <Drawer
      closeButtonProps={{ 'aria-label': t('common.close') }}
      opened={componentId !== null}
      onClose={onClose}
      title={component?.name ?? t('ux.quickView')}
      position="right"
      size="md"
    >
      <Stack>
        {query.isPending ? <LoadingState /> : null}
        {query.error ? (
          <ErrorState error={query.error} onRetry={() => void query.refetch()} />
        ) : null}
        {component ? (
          <>
            {component.primaryImage?.thumbnailUrl ? (
              <Image
                src={toComponentImageUrl(component.primaryImage.thumbnailUrl)}
                alt=""
                h={180}
                fit="contain"
              />
            ) : null}
            <Text fw={600}>{component.brand}</Text>
            {component.description ? <Text>{component.description}</Text> : null}
            <Text>
              {t('components.item.availableQuantity', {
                available: component.availableQuantity,
                total: component.totalQuantity,
              })}
            </Text>
            <Table captionSide="top">
              <Table.Caption>{t('components.form.sections.attributes')}</Table.Caption>
              <Table.Tbody>
                {(component.attributes ?? []).map((attribute) => (
                  <Table.Tr key={attribute.attributeDefinitionId}>
                    <Table.Th scope="row">{attribute.label}</Table.Th>
                    <Table.Td>{attribute.value || t('components.item.noValue')}</Table.Td>
                  </Table.Tr>
                ))}
              </Table.Tbody>
            </Table>
            <Group>
              <Button component={Link} to={`/components/${component.id}`} variant="light">
                {t('ux.details')}
              </Button>
            </Group>
          </>
        ) : null}
      </Stack>
    </Drawer>
  );
}
