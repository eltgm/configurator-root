import type { ComponentImage } from '@/shared/api';

export const maxComponentImageSizeBytes = 10 * 1024 * 1024;
export const supportedComponentImageTypes = ['image/jpeg', 'image/png', 'image/webp'] as const;
export const componentImageAccept = supportedComponentImageTypes.join(',');

export type ComponentImageFileError = 'empty' | 'tooLarge' | 'unsupported';
export type ComponentImageMoveDirection = 'earlier' | 'later';

export function validateComponentImageFile(file: File): ComponentImageFileError | null {
  if (file.size === 0) return 'empty';
  if (file.size > maxComponentImageSizeBytes) return 'tooLarge';
  if (!supportedComponentImageTypes.some((contentType) => contentType === file.type)) {
    return 'unsupported';
  }
  return null;
}

export function sortComponentImages(images: ReadonlyArray<ComponentImage>): Array<ComponentImage> {
  return [...images].sort(
    (left, right) =>
      (left.orderIndex ?? Number.MAX_SAFE_INTEGER) -
        (right.orderIndex ?? Number.MAX_SAFE_INTEGER) || left.id - right.id,
  );
}

export function moveComponentImage(
  images: ReadonlyArray<ComponentImage>,
  imageId: number,
  direction: ComponentImageMoveDirection,
): Array<ComponentImage> {
  const currentIndex = images.findIndex((image) => image.id === imageId);
  const targetIndex = direction === 'earlier' ? currentIndex - 1 : currentIndex + 1;

  if (currentIndex < 0 || targetIndex < 0 || targetIndex >= images.length) {
    return [...images];
  }

  const reordered = [...images];
  [reordered[currentIndex], reordered[targetIndex]] = [
    reordered[targetIndex]!,
    reordered[currentIndex]!,
  ];
  return reordered;
}
