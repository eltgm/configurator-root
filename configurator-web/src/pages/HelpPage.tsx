import { Button, Paper, Stack, Text, Title } from '@mantine/core';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { PageHeader } from '@/shared/ui';
import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';

const links = [
  '/settings/domain',
  '/settings/types',
  '/components',
  '/settings/compatibility/rules',
  '/configurator',
];
export function HelpPage() {
  const { t } = useTranslation();
  useDocumentTitle(t('ux.helpTitle'), t('app.name'));
  return (
    <Stack gap="lg">
      <PageHeader title={t('ux.helpTitle')} description={t('ux.helpDescription')} />
      {links.map((link, index) => (
        <Paper p="lg" withBorder key={link}>
          <Stack gap="sm">
            <Title order={2} size="h3">
              {t(`ux.steps.${index}.title`)}
            </Title>
            <Text>{t(`ux.steps.${index}.text`)}</Text>
            <Button component={Link} to={link} variant="light" w="fit-content">
              {t(`ux.steps.${index}.title`)}
            </Button>
          </Stack>
        </Paper>
      ))}
      <Paper p="lg" withBorder>
        <Title order={2} size="h3">
          {t('ux.inventoryHelp')}
        </Title>
        <Text mt="sm">{t('ux.inventoryText')}</Text>
      </Paper>
      <Paper p="lg" withBorder>
        <Title order={2} size="h3">
          {t('ux.compatibilityHelp')}
        </Title>
        <Text mt="sm">{t('ux.compatibilityText')}</Text>
      </Paper>
    </Stack>
  );
}
