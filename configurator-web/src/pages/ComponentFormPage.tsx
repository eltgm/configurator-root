import { Button, Stack } from '@mantine/core';
import { IconArrowLeft, IconSettings } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';

import { useComponentTypesQuery } from '@/features/component-types/api/component-types';
import { useComponentQuery } from '@/features/components/api/components';
import { ComponentForm } from '@/features/components/ui/ComponentForm';
import { useDomainContext } from '@/features/domains/model/domain-context';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { EmptyState, ErrorState, LoadingState, PageHeader } from '@/shared/ui';

export function ComponentFormPage() {
  const { t } = useTranslation();
  const { componentId: rawComponentId } = useParams();
  const { selectedDomainId } = useDomainContext();
  const isEditing = rawComponentId !== undefined;
  const parsedId = rawComponentId === undefined ? null : Number(rawComponentId);
  const componentId =
    parsedId !== null && Number.isInteger(parsedId) && parsedId > 0 ? parsedId : null;
  const componentTypesQuery = useComponentTypesQuery(selectedDomainId);
  const componentQuery = useComponentQuery(selectedDomainId, isEditing ? componentId : null);
  const title = isEditing ? t('components.form.editTitle') : t('components.form.createTitle');
  useDocumentTitle(title, t('app.name'));

  if (selectedDomainId === null) return null;
  if (isEditing && componentId === null) {
    return (
      <EmptyState
        title={t('components.detail.notFoundTitle')}
        description={t('components.detail.notFoundDescription')}
      />
    );
  }
  if (componentTypesQuery.isPending || (isEditing && componentQuery.isPending)) {
    return <LoadingState label={t('components.detail.loading')} />;
  }
  if (componentTypesQuery.error) {
    return (
      <ErrorState
        error={componentTypesQuery.error}
        onRetry={() => void componentTypesQuery.refetch()}
      />
    );
  }
  if (componentQuery.error) {
    return (
      <ErrorState error={componentQuery.error} onRetry={() => void componentQuery.refetch()} />
    );
  }

  const componentTypes = componentTypesQuery.data ?? [];
  const component = componentQuery.data ?? undefined;
  if (
    isEditing &&
    (!component || !componentTypes.some((type) => type.id === component.componentTypeId))
  ) {
    return (
      <EmptyState
        title={t('components.detail.notFoundTitle')}
        description={t('components.detail.notFoundDescription')}
      />
    );
  }
  if (component?.archived) {
    return (
      <EmptyState
        title={t('components.form.archivedTitle')}
        description={t('components.form.archivedDescription')}
        action={
          <Button component={Link} to={`/components/${component.id}`}>
            {t('components.actions.open')}
          </Button>
        }
      />
    );
  }
  if (!isEditing && componentTypes.length === 0) {
    return (
      <EmptyState
        title={t('components.form.noTypesTitle')}
        description={t('components.form.noTypesDescription')}
        action={
          <Button component={Link} to="/settings/types" leftSection={<IconSettings size={16} />}>
            {t('components.form.configureTypes')}
          </Button>
        }
      />
    );
  }

  return (
    <Stack gap="xl">
      <PageHeader
        title={title}
        description={
          isEditing
            ? t('components.form.editDescription', { name: component?.name })
            : t('components.form.createDescription')
        }
        actions={
          <Button
            component={Link}
            to={component ? `/components/${component.id}` : '/components'}
            variant="default"
            leftSection={<IconArrowLeft size={16} />}
          >
            {t('components.actions.backToCatalog')}
          </Button>
        }
      />
      <ComponentForm
        domainId={selectedDomainId}
        componentTypes={componentTypes}
        component={component}
      />
    </Stack>
  );
}
