import { Button, Paper, SimpleGrid, Stack, Text, ThemeIcon, Title } from '@mantine/core';
import { useDisclosure } from '@mantine/hooks';
import { IconDatabasePlus, IconPlus } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';

import { useDomainContext } from '@/features/domains/model/domain-context';
import { CreateDemoDomainButton } from '@/features/domains/ui/CreateDemoDomainButton';
import { DomainFormModal } from '@/features/domains/ui/DomainFormModal';

export function FirstRunState() {
  const { t } = useTranslation();
  const { selectDomain } = useDomainContext();
  const [formOpened, form] = useDisclosure(false);

  return (
    <>
      <Paper maw={880} mx="auto" p={{ base: 'lg', sm: 40 }} radius="lg" withBorder>
        <Stack gap="lg" align="flex-start">
          <ThemeIcon size={56} radius="lg" variant="light">
            <IconDatabasePlus size={32} stroke={1.6} aria-hidden="true" />
          </ThemeIcon>
          <Stack gap="xs">
            <Title order={1}>{t('domains.firstRun.title')}</Title>
            <Text c="dimmed" size="lg" maw={700}>
              {t('domains.firstRun.description')}
            </Text>
          </Stack>
          <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="sm" w="100%">
            <Button
              size="md"
              leftSection={<IconPlus size={19} aria-hidden="true" />}
              onClick={form.open}
            >
              {t('domains.actions.create')}
            </Button>
            <CreateDemoDomainButton
              size="md"
              variant="light"
              label={t('domains.actions.createDemoFull')}
            />
          </SimpleGrid>
          <Text size="sm" c="dimmed">
            {t('domains.firstRun.hint')}
          </Text>
        </Stack>
      </Paper>

      <DomainFormModal
        opened={formOpened}
        onClose={form.close}
        onSaved={(domain) => {
          selectDomain(domain.id);
        }}
      />
    </>
  );
}
