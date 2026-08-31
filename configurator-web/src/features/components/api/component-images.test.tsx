import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import type { PropsWithChildren } from 'react';
import { describe, expect, it, vi } from 'vitest';

import {
  componentImageKeys,
  useComponentImagesQuery,
  useDeleteComponentImageMutation,
  useReorderComponentImagesMutation,
  useUploadComponentImageMutation,
} from '@/features/components/api/component-images';
import { componentKeys } from '@/features/components/api/components';
import type { Component, ComponentImage } from '@/shared/api';
import { server, testApiBaseUrl } from '@/test/server';

const domainId = 7;
const componentId = 101;
const firstImage: ComponentImage = {
  id: 11,
  url: '/component-images/11/content',
  thumbnailUrl: '/component-images/11/thumbnail',
  orderIndex: 0,
};
const secondImage: ComponentImage = {
  id: 12,
  url: '/component-images/12/content',
  thumbnailUrl: '/component-images/12/thumbnail',
  orderIndex: 1,
};

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

function createQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

describe('component image API', () => {
  it('loads and sorts a gallery with a domain- and component-scoped key', async () => {
    server.use(
      http.get(`${testApiBaseUrl}/components/:id/images`, ({ params }) => {
        expect(params['id']).toBe(String(componentId));
        return HttpResponse.json([secondImage, firstImage]);
      }),
    );
    const queryClient = createQueryClient();
    const gallery = renderHook(() => useComponentImagesQuery(domainId, componentId), {
      wrapper: createWrapper(queryClient),
    });

    await waitFor(() => expect(gallery.result.current.isSuccess).toBe(true));
    expect(gallery.result.current.data?.map(({ id }) => id)).toEqual([11, 12]);
    expect(componentImageKeys.byComponent(domainId, componentId)).toEqual([
      'domains',
      domainId,
      'components',
      'detail',
      componentId,
      'images',
    ]);
  });

  it('uploads multipart data, appends the response and synchronizes component caches', async () => {
    let submittedContentType: string | null = null;
    server.use(
      http.post(`${testApiBaseUrl}/components/:id/images`, ({ request }) => {
        submittedContentType = request.headers.get('content-type');
        return HttpResponse.json(secondImage, { status: 201 });
      }),
    );
    const queryClient = createQueryClient();
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');
    const otherDomainKey = [
      'domains',
      8,
      'configurator',
      'compatibility',
      'candidates',
      [1],
    ] as const;
    const sameDomainKey = [
      'domains',
      domainId,
      'configurator',
      'compatibility',
      'candidates',
      [1],
    ] as const;
    queryClient.setQueryData(otherDomainKey, {});
    queryClient.setQueryData(sameDomainKey, {});
    queryClient.setQueryData(componentImageKeys.byComponent(domainId, componentId), [firstImage]);
    queryClient.setQueryData<Component>(componentKeys.detail(domainId, componentId), {
      id: componentId,
      componentTypeId: 1,
      name: 'Component',
      archived: false,
      createdAt: '2026-08-23T00:00:00Z',
      images: [firstImage],
    });
    const upload = renderHook(() => useUploadComponentImageMutation(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      await upload.result.current.mutateAsync({
        domainId,
        componentId,
        file: new File(['png'], 'image.png', { type: 'image/png' }),
      });
    });
    expect(upload.result.current.error).toBeNull();

    expect(queryClient.getQueryState(sameDomainKey)?.isInvalidated).toBe(true);
    expect(queryClient.getQueryState(otherDomainKey)?.isInvalidated).toBe(false);
    expect(
      queryClient.getQueryData<Component>(componentKeys.detail(domainId, componentId))
        ?.primaryImage,
    ).toEqual(firstImage);
    expect(submittedContentType).toMatch(/^multipart\/form-data; boundary=/);
    expect(queryClient.getQueryData(componentImageKeys.byComponent(domainId, componentId))).toEqual(
      [firstImage, secondImage],
    );
    expect(
      queryClient.getQueryData<Component>(componentKeys.detail(domainId, componentId))?.images,
    ).toEqual([firstImage, secondImage]);
    expect(invalidate).toHaveBeenCalledWith(
      expect.objectContaining({ queryKey: componentKeys.byDomain(domainId) }),
    );
  });

  it('deletes only after success and removes the image from both caches', async () => {
    let deletedImageId: string | undefined;
    server.use(
      http.delete(`${testApiBaseUrl}/component-images/:id`, ({ params }) => {
        deletedImageId = String(params['id']);
        return new HttpResponse(null, { status: 204 });
      }),
    );
    const queryClient = createQueryClient();
    queryClient.setQueryData(componentImageKeys.byComponent(domainId, componentId), [
      firstImage,
      secondImage,
    ]);
    queryClient.setQueryData<Component>(componentKeys.detail(domainId, componentId), {
      id: componentId,
      componentTypeId: 1,
      name: 'Component',
      archived: false,
      createdAt: '2026-08-23T00:00:00Z',
      images: [firstImage, secondImage],
    });
    const remove = renderHook(() => useDeleteComponentImageMutation(), {
      wrapper: createWrapper(queryClient),
    });

    remove.result.current.mutate({ domainId, componentId, imageId: firstImage.id });
    await waitFor(() => expect(remove.result.current.isSuccess).toBe(true));

    expect(deletedImageId).toBe(String(firstImage.id));
    expect(
      queryClient.getQueryData<Component>(componentKeys.detail(domainId, componentId))
        ?.primaryImage,
    ).toEqual(secondImage);
    expect(queryClient.getQueryData(componentImageKeys.byComponent(domainId, componentId))).toEqual(
      [secondImage],
    );
    expect(
      queryClient.getQueryData<Component>(componentKeys.detail(domainId, componentId))?.images,
    ).toEqual([secondImage]);
  });

  it('sends the complete order once and uses the authoritative response', async () => {
    let submittedBody: unknown;
    const reordered = [
      { ...secondImage, orderIndex: 0 },
      { ...firstImage, orderIndex: 1 },
    ];
    server.use(
      http.put(`${testApiBaseUrl}/components/:id/images/order`, async ({ request }) => {
        submittedBody = await request.json();
        return HttpResponse.json(reordered);
      }),
    );
    const queryClient = createQueryClient();
    const reorder = renderHook(() => useReorderComponentImagesMutation(), {
      wrapper: createWrapper(queryClient),
    });

    reorder.result.current.mutate({
      domainId,
      componentId,
      imageIds: [secondImage.id, firstImage.id],
    });
    await waitFor(() => expect(reorder.result.current.isSuccess).toBe(true));

    expect(submittedBody).toEqual({ imageIds: [12, 11] });
    expect(queryClient.getQueryData(componentImageKeys.byComponent(domainId, componentId))).toEqual(
      reordered,
    );
  });
});
