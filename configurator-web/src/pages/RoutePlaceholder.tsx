import { Badge, Paper, Stack, Text, ThemeIcon, Title } from '@mantine/core';
import type { TablerIcon } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';

import { useDocumentTitle } from '@/shared/lib/useDocumentTitle';

interface RoutePlaceholderProps {
  icon: TablerIcon;
  titleKey: string;
  descriptionKey: string;
}

export function RoutePlaceholder({ icon: Icon, titleKey, descriptionKey }: RoutePlaceholderProps) {
  const { t } = useTranslation();
  const title = t(titleKey);
  useDocumentTitle(title, t('app.name'));

  return (
    <Paper maw={880} mx="auto" p={{ base: 'lg', sm: 40 }} radius="lg" withBorder>
      <Stack gap="md" align="flex-start">
        <ThemeIcon size={52} radius="lg" variant="light">
          <Icon size={30} stroke={1.6} />
        </ThemeIcon>
        <Badge variant="light">{t('pages.status')}</Badge>
        <Title order={1}>{title}</Title>
        <Text c="dimmed" size="lg" maw={680}>
          {t(descriptionKey)}
        </Text>
      </Stack>
    </Paper>
  );
}
