import { describe, expect, it } from 'vitest';

import {
  maxComponentImageSizeBytes,
  moveComponentImage,
  sortComponentImages,
  validateComponentImageFile,
} from '@/features/components/model/component-images';
import type { ComponentImage } from '@/shared/api';

const images: Array<ComponentImage> = [
  { id: 1, url: '/one', orderIndex: 0 },
  { id: 2, url: '/two', orderIndex: 1 },
  { id: 3, url: '/three', orderIndex: 2 },
];

describe('component image model', () => {
  it('validates observable file constraints at their boundaries', () => {
    expect(validateComponentImageFile(new File([], 'empty.png', { type: 'image/png' }))).toBe(
      'empty',
    );
    expect(
      validateComponentImageFile(
        new File([new Uint8Array(maxComponentImageSizeBytes)], 'maximum.webp', {
          type: 'image/webp',
        }),
      ),
    ).toBeNull();
    expect(
      validateComponentImageFile(
        new File([new Uint8Array(maxComponentImageSizeBytes + 1)], 'large.jpg', {
          type: 'image/jpeg',
        }),
      ),
    ).toBe('tooLarge');
    expect(
      validateComponentImageFile(new File(['gif'], 'unsupported.gif', { type: 'image/gif' })),
    ).toBe('unsupported');
  });

  it('sorts by order index with null values last and then by id', () => {
    const source: Array<ComponentImage> = [
      { id: 4, url: '/four' },
      { id: 3, url: '/three', orderIndex: 1 },
      { id: 2, url: '/two', orderIndex: 0 },
      { id: 1, url: '/one' },
    ];

    expect(sortComponentImages(source).map(({ id }) => id)).toEqual([2, 3, 1, 4]);
    expect(source.map(({ id }) => id)).toEqual([4, 3, 2, 1]);
  });

  it('moves images without mutating the source and keeps boundary actions as no-ops', () => {
    expect(moveComponentImage(images, 2, 'earlier').map(({ id }) => id)).toEqual([2, 1, 3]);
    expect(moveComponentImage(images, 2, 'later').map(({ id }) => id)).toEqual([1, 3, 2]);
    expect(moveComponentImage(images, 1, 'earlier').map(({ id }) => id)).toEqual([1, 2, 3]);
    expect(moveComponentImage(images, 3, 'later').map(({ id }) => id)).toEqual([1, 2, 3]);
    expect(images.map(({ id }) => id)).toEqual([1, 2, 3]);
  });
});
