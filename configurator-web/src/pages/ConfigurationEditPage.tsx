import { Alert, Button, Stack, Text } from '@mantine/core';
import { IconDatabase } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { useParams } from 'react-router-dom';

import { useComponentTypesQuery } from '@/features/component-types/api/component-types';
import { useConfigurationQuery } from '@/features/configurations/api/configurations';
import { ConfigurationEditor } from '@/features/configurations/ui/ConfigurationEditor';
import { useDomainContext } from '@/features/domains/model/domain-context';
import { normalizeApiError } from '@/shared/api/errors';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { EmptyState, ErrorState, LoadingState } from '@/shared/ui';

function positiveIdentifier(value: string | undefined) {
  if (value === undefined) return null;
  const identifier = Number(value);
  return Number.isSafeInteger(identifier) && identifier > 0 ? identifier : null;
}

export function ConfigurationEditPage() {
  const { t } = useTranslation();
  const { configurationId: rawConfigurationId } = useParams();
  const configurationId = positiveIdentifier(rawConfigurationId);
  const { domains, selectedDomainId, selectDomain } = useDomainContext();
  const configurationQuery = useConfigurationQuery(selectedDomainId, configurationId);
  const componentTypesQuery = useComponentTypesQuery(selectedDomainId);
  const configuration = configurationQuery.data;
  const owningDomain = domains.find((domain) => domain.id === configuration?.domainId);
  const domainMatches = configuration?.domainId === selectedDomainId;
  useDocumentTitle(t('configurations.editor.title'), t('app.name'));

  if (configurationId === null) {
    return (
      <EmptyState
        title={t('configurations.detail.notFoundTitle')}
        description={t('configurations.detail.notFoundDescription')}
      />
    );
  }
  if (configurationQuery.isPending || componentTypesQuery.isPending) {
    return <LoadingState label={t('configurations.editor.loading')} />;
  }
  if (configurationQuery.error) {
    if (normalizeApiError(configurationQuery.error).status === 404) {
      return (
        <EmptyState
          title={t('configurations.detail.notFoundTitle')}
          description={t('configurations.detail.notFoundDescription')}
        />
      );
    }
    return (
      <ErrorState
        error={configurationQuery.error}
        onRetry={() => void configurationQuery.refetch()}
      />
    );
  }
  if (componentTypesQuery.error) {
    return (
      <ErrorState
        error={componentTypesQuery.error}
        onRetry={() => void componentTypesQuery.refetch()}
      />
    );
  }
  if (!configuration) return null;

  if (!domainMatches) {
    return (
      <Alert
        color="blue"
        icon={<IconDatabase aria-hidden="true" />}
        title={t('configurations.detail.domainMismatchTitle')}
      >
        <Stack align="flex-start" gap="md">
          <Text size="sm">
            {t('configurations.detail.domainMismatchDescription', {
              domain: owningDomain?.name ?? `#${configuration.domainId}`,
            })}
          </Text>
          {owningDomain ? (
            <Button onClick={() => selectDomain(owningDomain.id)}>
              {t('configurations.detail.switchDomain', { domain: owningDomain.name })}
            </Button>
          ) : null}
        </Stack>
      </Alert>
    );
  }

  return (
    <ConfigurationEditor
      key={configuration.id}
      configuration={configuration}
      componentTypes={componentTypesQuery.data ?? []}
    />
  );
}
