import { Image, ThemeIcon } from '@mantine/core';
import { IconPhotoOff } from '@tabler/icons-react';
import { useState } from 'react';

import { toComponentImageUrl } from '@/features/components/model/catalog-preferences';
import type { ComponentImage } from '@/shared/api';

export function ComponentThumbnail({
  image,
  alt = '',
}: {
  image: ComponentImage | null | undefined;
  alt?: string;
}) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const url = image?.thumbnailUrl;
  return url && failedUrl !== url ? (
    <Image
      src={toComponentImageUrl(url)}
      alt={alt}
      fit="contain"
      h="100%"
      w="100%"
      mih={0}
      miw={0}
      loading="lazy"
      decoding="async"
      onError={() => setFailedUrl(url)}
    />
  ) : (
    <ThemeIcon size={40} radius="xl" variant="light" aria-hidden="true">
      <IconPhotoOff size={22} stroke={1.6} />
    </ThemeIcon>
  );
}
