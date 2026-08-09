import { Button, type ButtonProps } from '@mantine/core';
import { IconSparkles } from '@tabler/icons-react';
import { useTranslation } from 'react-i18next';

import { useCreateDemoDomainMutation } from '@/features/domains/api/domains';
import { useDomainContext } from '@/features/domains/model/domain-context';
import { showSuccessNotification } from '@/shared/notifications/notifications';

interface CreateDemoDomainButtonProps extends ButtonProps {
  label?: string | undefined;
}

export function CreateDemoDomainButton({ label, ...buttonProps }: CreateDemoDomainButtonProps) {
  const { t } = useTranslation();
  const { selectDomain } = useDomainContext();
  const createDemo = useCreateDemoDomainMutation();

  const create = async () => {
    try {
      const domain = await createDemo.mutateAsync();
      selectDomain(domain.id);
      showSuccessNotification(t('domains.notifications.demoCreated'));
    } catch {
      // The global mutation policy presents the structured API error.
    }
  };

  return (
    <Button
      {...buttonProps}
      leftSection={<IconSparkles size={18} aria-hidden="true" />}
      loading={createDemo.isPending}
      onClick={() => void create()}
    >
      {label ?? t('domains.actions.createDemo')}
    </Button>
  );
}
