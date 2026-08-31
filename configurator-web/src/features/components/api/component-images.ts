import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { configuratorCompatibilityKeys } from '@/features/configurator/api/configurator-compatibility';
import { componentKeys } from '@/features/components/api/components';
import { sortComponentImages } from '@/features/components/model/component-images';
import {
  apiData,
  client,
  deleteComponentImagesById,
  getComponentsByIdImages,
  postComponentsByIdImages,
  putComponentsByIdImagesOrder,
  type Component,
  type ComponentImage,
} from '@/shared/api';

export const componentImageKeys = {
  byComponent: (domainId: number | null, componentId: number | null) =>
    [...componentKeys.detail(domainId, componentId), 'images'] as const,
};

export function useComponentImagesQuery(domainId: number | null, componentId: number | null) {
  return useQuery({
    queryKey: componentImageKeys.byComponent(domainId, componentId),
    queryFn: () =>
      componentId === null
        ? Promise.resolve([])
        : apiData(
            getComponentsByIdImages({
              client,
              path: { id: componentId },
              throwOnError: true,
            }),
          ),
    enabled: domainId !== null && componentId !== null,
    select: sortComponentImages,
  });
}

function setComponentImages(
  queryClient: ReturnType<typeof useQueryClient>,
  domainId: number,
  componentId: number,
  images: ReadonlyArray<ComponentImage>,
) {
  const orderedImages = sortComponentImages(images);
  queryClient.setQueryData<Array<ComponentImage>>(
    componentImageKeys.byComponent(domainId, componentId),
    orderedImages,
  );
  queryClient.setQueryData<Component>(componentKeys.detail(domainId, componentId), (component) =>
    component
      ? { ...component, images: orderedImages, primaryImage: orderedImages[0] ?? null }
      : component,
  );
}

async function invalidateComponentCatalogs(
  queryClient: ReturnType<typeof useQueryClient>,
  domainId: number,
) {
  await Promise.all([
    queryClient.invalidateQueries({
      queryKey: componentKeys.byDomain(domainId),
      predicate: (query) => query.queryKey[3] !== 'detail',
    }),
    queryClient.invalidateQueries({ queryKey: configuratorCompatibilityKeys.root(domainId) }),
  ]);
}

interface UploadComponentImageVariables {
  domainId: number;
  componentId: number;
  file: File;
}

export function useUploadComponentImageMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ componentId, file }: UploadComponentImageVariables) =>
      apiData(
        postComponentsByIdImages({
          client,
          path: { id: componentId },
          body: { file },
          throwOnError: true,
        }),
      ),
    onSuccess: async (uploadedImage, { domainId, componentId }) => {
      const images =
        queryClient.getQueryData<Array<ComponentImage>>(
          componentImageKeys.byComponent(domainId, componentId),
        ) ?? [];
      setComponentImages(queryClient, domainId, componentId, [...images, uploadedImage]);
      await invalidateComponentCatalogs(queryClient, domainId);
    },
  });
}

interface DeleteComponentImageVariables {
  domainId: number;
  componentId: number;
  imageId: number;
}

export function useDeleteComponentImageMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ imageId }: DeleteComponentImageVariables) =>
      apiData(deleteComponentImagesById({ client, path: { id: imageId }, throwOnError: true })),
    onSuccess: async (_response, { domainId, componentId, imageId }) => {
      const images =
        queryClient.getQueryData<Array<ComponentImage>>(
          componentImageKeys.byComponent(domainId, componentId),
        ) ?? [];
      setComponentImages(
        queryClient,
        domainId,
        componentId,
        images.filter((image) => image.id !== imageId),
      );
      await invalidateComponentCatalogs(queryClient, domainId);
    },
  });
}

interface ReorderComponentImagesVariables {
  domainId: number;
  componentId: number;
  imageIds: Array<number>;
}

export function useReorderComponentImagesMutation() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ componentId, imageIds }: ReorderComponentImagesVariables) =>
      apiData(
        putComponentsByIdImagesOrder({
          client,
          path: { id: componentId },
          body: { imageIds },
          throwOnError: true,
        }),
      ),
    onSuccess: async (images, { domainId, componentId }) => {
      setComponentImages(queryClient, domainId, componentId, images);
      await invalidateComponentCatalogs(queryClient, domainId);
    },
  });
}
