import { useTranslation } from 'react-i18next';

import { useExportConfigurationMutation } from '@/features/configurations/api/configurations';
import {
  configurationExportFileName,
  serializeConfigurationExport,
} from '@/features/configurations/model/configuration-operations';
import type { Configuration } from '@/shared/api';
import { downloadTextFile } from '@/shared/lib/download';
import { showSuccessNotification } from '@/shared/notifications/notifications';

export function useConfigurationExport() {
  const { t } = useTranslation();
  const exportConfigurationMutation = useExportConfigurationMutation();

  const exportConfiguration = async (configuration: Configuration) => {
    try {
      const exported = await exportConfigurationMutation.mutateAsync(configuration.id);
      downloadTextFile({
        content: serializeConfigurationExport(exported),
        fileName: configurationExportFileName(configuration.id),
        mimeType: 'application/json;charset=utf-8',
      });
      showSuccessNotification(t('configurations.notifications.exported'));
    } catch {
      // The global mutation policy presents the normalized error; no file is created.
    }
  };

  return {
    exportConfiguration,
    exportingConfigurationId: exportConfigurationMutation.isPending
      ? exportConfigurationMutation.variables
      : undefined,
  };
}
