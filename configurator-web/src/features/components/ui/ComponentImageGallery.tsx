import {
  ActionIcon,
  Alert,
  Badge,
  Button,
  Divider,
  FileInput,
  Group,
  Image,
  Modal,
  Paper,
  SimpleGrid,
  Stack,
  Text,
  ThemeIcon,
  Title,
  Tooltip,
  UnstyledButton,
} from '@mantine/core';
import {
  IconArrowLeft,
  IconArrowRight,
  IconArrowsSort,
  IconDeviceFloppy,
  IconInfoCircle,
  IconPhoto,
  IconPhotoOff,
  IconTrash,
  IconUpload,
  IconX,
} from '@tabler/icons-react';
import { useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';

import {
  useComponentImagesQuery,
  useDeleteComponentImageMutation,
  useReorderComponentImagesMutation,
  useUploadComponentImageMutation,
} from '@/features/components/api/component-images';
import {
  componentImageAccept,
  moveComponentImage,
  sortComponentImages,
  type ComponentImageFileError,
  validateComponentImageFile,
} from '@/features/components/model/component-images';
import { toComponentImageUrl } from '@/features/components/model/catalog-preferences';
import type { ComponentImage } from '@/shared/api';
import { showSuccessNotification } from '@/shared/notifications/notifications';
import { ErrorState, LoadingState } from '@/shared/ui';

import classes from './component-image-gallery.module.css';

interface ComponentImageGalleryProps {
  domainId: number;
  componentId: number;
  componentName: string;
  archived: boolean;
}

function sameOrder(left: ReadonlyArray<ComponentImage>, right: ReadonlyArray<ComponentImage>) {
  return (
    left.length === right.length && left.every((image, index) => image.id === right[index]?.id)
  );
}

export function ComponentImageGallery({
  domainId,
  componentId,
  componentName,
  archived,
}: ComponentImageGalleryProps) {
  const { t } = useTranslation();
  const galleryQuery = useComponentImagesQuery(domainId, componentId);
  const uploadImage = useUploadComponentImageMutation();
  const deleteImage = useDeleteComponentImageMutation();
  const reorderImages = useReorderComponentImagesMutation();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [fileError, setFileError] = useState<ComponentImageFileError | null>(null);
  const [previewImage, setPreviewImage] = useState<ComponentImage | null>(null);
  const [imageToDelete, setImageToDelete] = useState<ComponentImage | null>(null);
  const [orderDraft, setOrderDraft] = useState<Array<ComponentImage> | null>(null);

  const serverImages = useMemo(
    () => sortComponentImages(galleryQuery.data ?? []),
    [galleryQuery.data],
  );
  const displayedImages = orderDraft ?? serverImages;
  const isOrdering = orderDraft !== null;
  const isOrderDirty = orderDraft !== null && !sameOrder(orderDraft, serverImages);
  const mutationPending = uploadImage.isPending || deleteImage.isPending || reorderImages.isPending;

  const chooseFile = (file: File | null) => {
    setSelectedFile(file);
    setFileError(file ? validateComponentImageFile(file) : null);
  };

  const upload = async () => {
    if (!selectedFile) return;
    const validationError = validateComponentImageFile(selectedFile);
    setFileError(validationError);
    if (validationError) return;

    try {
      await uploadImage.mutateAsync({ domainId, componentId, file: selectedFile });
      setSelectedFile(null);
      showSuccessNotification(t('components.gallery.notifications.uploaded'));
    } catch {
      // Global mutation handling shows a safe structured error.
    }
  };

  const remove = async () => {
    if (!imageToDelete) return;
    try {
      await deleteImage.mutateAsync({ domainId, componentId, imageId: imageToDelete.id });
      setImageToDelete(null);
      showSuccessNotification(t('components.gallery.notifications.deleted'));
    } catch {
      // Keep the confirmation open so the user can deliberately retry.
    }
  };

  const saveOrder = async () => {
    if (!orderDraft || !isOrderDirty) return;
    try {
      await reorderImages.mutateAsync({
        domainId,
        componentId,
        imageIds: orderDraft.map(({ id }) => id),
      });
      setOrderDraft(null);
      showSuccessNotification(t('components.gallery.notifications.reordered'));
    } catch {
      setOrderDraft(null);
      await galleryQuery.refetch();
    }
  };

  const fileErrorMessage = fileError ? t('components.gallery.validation.' + fileError) : undefined;

  return (
    <Paper p="lg" withBorder>
      <Stack gap="md">
        <Stack gap={2}>
          <Group justify="space-between" align="flex-start" wrap="wrap">
            <Title order={2} size="h3">
              {t('components.detail.sections.images')}
            </Title>
            {!archived && serverImages.length > 1 && !isOrdering ? (
              <Button
                size="xs"
                variant="default"
                leftSection={<IconArrowsSort size={16} aria-hidden="true" />}
                disabled={mutationPending}
                onClick={() => setOrderDraft([...serverImages])}
              >
                {t('components.gallery.actions.reorder')}
              </Button>
            ) : null}
          </Group>
          <Text size="sm" c="dimmed">
            {archived ? t('components.gallery.readOnlyHint') : t('components.gallery.activeHint')}
          </Text>
        </Stack>
        <Divider />

        {!archived ? (
          <Group align="flex-end" className={classes['upload-controls']}>
            <FileInput
              className={classes['file-input']}
              label={t('components.gallery.file.label')}
              placeholder={t('components.gallery.file.placeholder')}
              description={t('components.gallery.file.description')}
              accept={componentImageAccept}
              value={selectedFile}
              error={fileErrorMessage}
              clearable
              leftSection={<IconPhoto size={16} aria-hidden="true" />}
              disabled={mutationPending || isOrdering}
              onChange={chooseFile}
            />
            <Button
              leftSection={<IconUpload size={16} aria-hidden="true" />}
              loading={uploadImage.isPending}
              disabled={!selectedFile || fileError !== null || isOrdering || mutationPending}
              onClick={() => void upload()}
            >
              {t('components.gallery.actions.upload')}
            </Button>
          </Group>
        ) : null}

        {isOrdering ? (
          <Alert
            icon={<IconInfoCircle aria-hidden="true" />}
            title={t('components.gallery.order.title')}
            color={isOrderDirty ? 'blue' : 'gray'}
          >
            <Stack gap="sm">
              <Text size="sm">{t('components.gallery.order.description')}</Text>
              <Group>
                <Button
                  size="xs"
                  leftSection={<IconDeviceFloppy size={16} aria-hidden="true" />}
                  loading={reorderImages.isPending}
                  disabled={!isOrderDirty || mutationPending}
                  onClick={() => void saveOrder()}
                >
                  {t('components.gallery.actions.saveOrder')}
                </Button>
                <Button
                  size="xs"
                  variant="default"
                  leftSection={<IconX size={16} aria-hidden="true" />}
                  disabled={mutationPending}
                  onClick={() => setOrderDraft(null)}
                >
                  {t('components.gallery.actions.cancelOrder')}
                </Button>
              </Group>
            </Stack>
          </Alert>
        ) : null}

        {galleryQuery.isPending ? (
          <LoadingState label={t('components.gallery.states.loading')} rows={2} />
        ) : galleryQuery.error ? (
          <ErrorState error={galleryQuery.error} onRetry={() => void galleryQuery.refetch()} />
        ) : displayedImages.length ? (
          <SimpleGrid cols={{ base: 1, xs: 2 }}>
            {displayedImages.map((image, index) => (
              <Paper key={image.id} p="xs" withBorder className={classes['image-card']}>
                <Stack gap="xs">
                  <UnstyledButton
                    className={classes['preview-button']}
                    aria-label={t('components.gallery.actions.view', { number: index + 1 })}
                    onClick={() => setPreviewImage(image)}
                  >
                    <Image
                      src={toComponentImageUrl(image.url)}
                      alt={t('components.detail.imageAlt', {
                        name: componentName,
                        number: index + 1,
                      })}
                      radius="sm"
                      className={classes.image}
                    />
                  </UnstyledButton>
                  <Group justify="space-between" wrap="nowrap">
                    <Badge variant="light">
                      {t('components.gallery.imageNumber', { number: index + 1 })}
                    </Badge>
                    {!archived ? (
                      <Group gap={4} wrap="nowrap">
                        {isOrdering ? (
                          <>
                            <Tooltip label={t('components.gallery.actions.moveEarlier')}>
                              <ActionIcon
                                variant="default"
                                disabled={index === 0 || mutationPending}
                                aria-label={t('components.gallery.actions.moveEarlier')}
                                onClick={() =>
                                  setOrderDraft((draft) =>
                                    draft ? moveComponentImage(draft, image.id, 'earlier') : draft,
                                  )
                                }
                              >
                                <IconArrowLeft size={16} aria-hidden="true" />
                              </ActionIcon>
                            </Tooltip>
                            <Tooltip label={t('components.gallery.actions.moveLater')}>
                              <ActionIcon
                                variant="default"
                                disabled={index === displayedImages.length - 1 || mutationPending}
                                aria-label={t('components.gallery.actions.moveLater')}
                                onClick={() =>
                                  setOrderDraft((draft) =>
                                    draft ? moveComponentImage(draft, image.id, 'later') : draft,
                                  )
                                }
                              >
                                <IconArrowRight size={16} aria-hidden="true" />
                              </ActionIcon>
                            </Tooltip>
                          </>
                        ) : (
                          <Tooltip label={t('components.gallery.actions.delete')}>
                            <ActionIcon
                              color="red"
                              variant="subtle"
                              disabled={mutationPending}
                              aria-label={t('components.gallery.actions.deleteImage', {
                                number: index + 1,
                              })}
                              onClick={() => setImageToDelete(image)}
                            >
                              <IconTrash size={16} aria-hidden="true" />
                            </ActionIcon>
                          </Tooltip>
                        )}
                      </Group>
                    ) : null}
                  </Group>
                </Stack>
              </Paper>
            ))}
          </SimpleGrid>
        ) : (
          <Stack align="center" py="xl">
            <ThemeIcon size={56} radius="xl" variant="light">
              <IconPhotoOff size={28} aria-hidden="true" />
            </ThemeIcon>
            <Text c="dimmed" ta="center">
              {t('components.gallery.states.empty')}
            </Text>
          </Stack>
        )}
      </Stack>

      <Modal
        opened={previewImage !== null}
        onClose={() => setPreviewImage(null)}
        title={t('components.gallery.preview.title')}
        size="xl"
        centered
      >
        {previewImage ? (
          <Image
            src={toComponentImageUrl(previewImage.url)}
            alt={t('components.gallery.preview.alt', { name: componentName })}
            radius="md"
            className={classes['preview-image']}
          />
        ) : null}
      </Modal>

      <Modal
        opened={imageToDelete !== null}
        onClose={() => !deleteImage.isPending && setImageToDelete(null)}
        title={t('components.gallery.delete.title')}
        centered
        closeOnClickOutside={!deleteImage.isPending}
        closeOnEscape={!deleteImage.isPending}
      >
        <Stack gap="md">
          <Text>{t('components.gallery.delete.description')}</Text>
          <Text size="sm" c="dimmed">
            {t('components.gallery.delete.warning')}
          </Text>
          <Group justify="flex-end">
            <Button
              variant="default"
              disabled={deleteImage.isPending}
              onClick={() => setImageToDelete(null)}
            >
              {t('common.cancel')}
            </Button>
            <Button color="red" loading={deleteImage.isPending} onClick={() => void remove()}>
              {t('components.gallery.actions.delete')}
            </Button>
          </Group>
        </Stack>
      </Modal>
    </Paper>
  );
}
