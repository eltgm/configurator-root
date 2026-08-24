import { Paper, Stack, Text, ThemeIcon, Title } from '@mantine/core';
import { IconInbox } from '@tabler/icons-react';
import type { ReactNode } from 'react';

import classes from '@/shared/ui/shared-ui.module.css';

interface EmptyStateProps {
  title: string;
  description?: string | undefined;
  action?: ReactNode;
  icon?: ReactNode;
}

export function EmptyState({ title, description, action, icon }: EmptyStateProps) {
  return (
    <Paper className={classes.state} p="xl" withBorder>
      <Stack align="center" gap="sm">
        <ThemeIcon size={48} radius="xl" variant="light" aria-hidden="true">
          {icon ?? <IconInbox size={26} stroke={1.7} />}
        </ThemeIcon>
        <Title order={2} size="h3" ta="center">
          {title}
        </Title>
        {description ? (
          <Text c="dimmed" ta="center" maw={560}>
            {description}
          </Text>
        ) : null}
        {action}
      </Stack>
    </Paper>
  );
}
