import { Anchor, Button, Stack } from '@mantine/core';
import { IconArrowLeft, IconCategory } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';
import { Link, useParams } from 'react-router-dom';

import { useCompatibilityRuleQuery } from '@/features/compatibility/api/compatibility-rules';
import { CompatibilityRuleForm } from '@/features/compatibility/ui/CompatibilityRuleForm';
import { useComponentTypesQuery } from '@/features/component-types/api/component-types';
import { useDomainContext } from '@/features/domains/model/domain-context';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';
import { EmptyState, ErrorState, LoadingState, PageHeader } from '@/shared/ui';

import classes from './compatibility-rule-form-page.module.css';

interface CompatibilityRuleFormContentProps {
  domainId: number;
}

function CompatibilityRuleFormContent({ domainId }: CompatibilityRuleFormContentProps) {
  const { t } = useTranslation();
  const params = useParams<{ ruleId?: string }>();
  const isEditing = params.ruleId !== undefined;
  const parsedRuleId = Number(params.ruleId);
  const ruleId =
    isEditing && Number.isInteger(parsedRuleId) && parsedRuleId > 0 ? parsedRuleId : null;
  const componentTypesQuery = useComponentTypesQuery(domainId);
  const ruleQuery = useCompatibilityRuleQuery(domainId, isEditing ? ruleId : null);
  const rule = ruleQuery.data;
  const isPending = componentTypesQuery.isPending || (isEditing && ruleQuery.isPending);
  const error = componentTypesQuery.error ?? ruleQuery.error;
  const title = isEditing
    ? t('compatibilityRules.page.editTitle')
    : t('compatibilityRules.page.createTitle');
  useDocumentTitle(title, t('app.name'));

  return (
    <Stack className={classes.container} gap="xl">
      <Anchor component={Link} to="/settings/compatibility/rules" fw={600} size="sm">
        <IconArrowLeft size={16} className={classes['back-icon']} />
        {t('compatibilityRules.actions.back')}
      </Anchor>
      <PageHeader
        title={title}
        description={
          isEditing
            ? t('compatibilityRules.page.editDescription', { name: rule?.name ?? '' })
            : t('compatibilityRules.page.createDescription')
        }
      />

      {isPending ? <LoadingState label={t('compatibilityRules.states.loadingRule')} /> : null}
      {error && !isPending ? (
        <ErrorState
          error={error}
          onRetry={() => void Promise.all([componentTypesQuery.refetch(), ruleQuery.refetch()])}
        />
      ) : null}
      {isEditing && ruleId === null ? (
        <EmptyState
          title={t('compatibilityRules.states.notFoundTitle')}
          description={t('compatibilityRules.states.notFoundDescription')}
        />
      ) : null}
      {!isPending && !error && componentTypesQuery.data && componentTypesQuery.data.length < 2 ? (
        <EmptyState
          icon={<IconCategory size={26} stroke={1.7} />}
          title={t('compatibilityRules.states.notEnoughTypesTitle')}
          description={t('compatibilityRules.states.notEnoughTypesDescription')}
          action={
            <Button component={Link} to="/settings/types">
              {t('navigation.types')}
            </Button>
          }
        />
      ) : null}
      {!isPending &&
      !error &&
      componentTypesQuery.data &&
      componentTypesQuery.data.length >= 2 &&
      (!isEditing || rule) ? (
        <CompatibilityRuleForm
          key={rule?.id ?? 'new'}
          domainId={domainId}
          componentTypes={componentTypesQuery.data}
          {...(rule ? { rule } : {})}
        />
      ) : null}
    </Stack>
  );
}

export function CompatibilityRuleFormPage() {
  const { selectedDomainId } = useDomainContext();
  if (selectedDomainId === null) {
    return null;
  }
  return <CompatibilityRuleFormContent key={selectedDomainId} domainId={selectedDomainId} />;
}
