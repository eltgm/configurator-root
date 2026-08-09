import { Box, Group, Stack, Text, Title } from '@mantine/core';
import type { ReactNode } from 'react';

import classes from './shared-ui.module.css';

interface PageHeaderProps {
  title: string;
  description?: string;
  actions?: ReactNode;
}

export function PageHeader({ title, description, actions }: PageHeaderProps) {
  return (
    <Group justify="space-between" align="flex-start" gap="md" wrap="wrap">
      <Stack className={classes['page-header-copy']} gap={4} maw={760}>
        <Title className={classes['page-header-title']} order={1}>
          {title}
        </Title>
        {description ? (
          <Text c="dimmed" size="sm">
            {description}
          </Text>
        ) : null}
      </Stack>
      {actions ? <Box className={classes['page-header-actions']}>{actions}</Box> : null}
    </Group>
  );
}
