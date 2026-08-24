import { Alert, Button, Progress, Stack, Text } from '@mantine/core';
import { IconDatabase } from '@tabler/icons-react';
import { useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate, useParams } from 'react-router-dom';

import { useConfigurationQuery } from '@/features/configurations/api/configurations';
import { getConfigurationCopyInitialValues } from '@/features/configurations/model/configuration-operations';
import { useConfigurationExport } from '@/features/configurations/model/useConfigurationExport';
import { ConfigurationDetails } from '@/features/configurations/ui/ConfigurationDetails';
import { CreateConfigurationModal } from '@/features/configurations/ui/CreateConfigurationModal';
import { DeleteConfigurationModal } from '@/features/configurations/ui/DeleteConfigurationModal';
import { useDomainContext } from '@/features/domains/model/domain-context';
import { normalizeApiError } from '@/shared/api/errors';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { EmptyState, ErrorState, LoadingState } from '@/shared/ui';

function positiveIdentifier(value: string | undefined) {
  if (value === undefined) return null;
  const identifier = Number(value);
  return Number.isSafeInteger(identifier) && identifier > 0 ? identifier : null;
}

export function ConfigurationDetailsPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { configurationId: rawConfigurationId } = useParams();
  const configurationId = positiveIdentifier(rawConfigurationId);
  const { domains, selectedDomainId, selectDomain } = useDomainContext();
  const query = useConfigurationQuery(selectedDomainId, configurationId);
  const [copying, setCopying] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const { exportConfiguration, exportingConfigurationId } = useConfigurationExport();
  const hasMatchedDomain = useRef(false);
  const configuration = query.data;
  const owningDomain = domains.find((domain) => domain.id === configuration?.domainId);
  const domainMatches = configuration?.domainId === selectedDomainId;
  const title = configuration?.name ?? t('configurations.detail.title');
  const copyInitialValues = useMemo(
    () =>
      configuration
        ? getConfigurationCopyInitialValues(configuration, t('configurations.copy.suffix'))
        : undefined,
    [configuration, t],
  );
  useDocumentTitle(title, t('app.name'));

  useEffect(() => {
    if (domainMatches) {
      hasMatchedDomain.current = true;
      return;
    }
    if (configuration && hasMatchedDomain.current) {
      void navigate('/configurations', { replace: true });
    }
  }, [configuration, domainMatches, navigate]);

  if (configurationId === null) {
    return (
      <EmptyState
        title={t('configurations.detail.notFoundTitle')}
        description={t('configurations.detail.notFoundDescription')}
      />
    );
  }
  if (query.isPending) {
    return <LoadingState label={t('configurations.detail.loading')} />;
  }
  if (query.error) {
    if (normalizeApiError(query.error).status === 404) {
      return (
        <EmptyState
          title={t('configurations.detail.notFoundTitle')}
          description={t('configurations.detail.notFoundDescription')}
        />
      );
    }
    return <ErrorState error={query.error} onRetry={() => void query.refetch()} />;
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
    <Stack gap="sm">
      {query.isFetching ? (
        <Progress
          value={100}
          size="xs"
          animated
          aria-label={t('configurations.detail.refreshing')}
        />
      ) : null}
      <ConfigurationDetails
        configuration={configuration}
        domainName={owningDomain?.name ?? ''}
        isExporting={exportingConfigurationId === configuration.id}
        onCopy={() => setCopying(true)}
        onExport={() => void exportConfiguration(configuration)}
        onDelete={() => setDeleting(true)}
      />
      <CreateConfigurationModal
        opened={copying}
        mode="copy"
        domainId={configuration.domainId}
        componentIds={configuration.components.map((component) => component.id)}
        components={configuration.components.map((component) => ({
          id: component.id,
          name: component.name,
          typeName: component.componentTypeName,
          ...(component.brand ? { brand: component.brand } : {}),
          archived: component.archived,
        }))}
        initialValues={copyInitialValues}
        onClose={() => setCopying(false)}
        onSaved={(copy) => void navigate(`/configurations/${copy.id}`)}
      />
      <DeleteConfigurationModal
        configuration={deleting ? configuration : undefined}
        onClose={() => setDeleting(false)}
        onDeleted={() => {
          setDeleting(false);
          void navigate('/configurations');
        }}
      />
    </Stack>
  );
}
