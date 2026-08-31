import { MantineProvider } from '@mantine/core';
import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { ComponentThumbnail } from './ComponentThumbnail';

describe('component thumbnail', () => {
  it('loads only the reduced URL and recovers from an error when the image changes', () => {
    const image = {
      id: 1,
      url: '/component-images/1/content',
      thumbnailUrl: '/component-images/1/thumbnail',
    };
    const { rerender } = render(
      <MantineProvider>
        <ComponentThumbnail image={image} alt="Component" />
      </MantineProvider>,
    );
    expect(screen.getByRole('img')).toHaveAttribute('src', '/api/component-images/1/thumbnail');
    expect(screen.getByRole('img')).toHaveAttribute('loading', 'lazy');
    fireEvent.error(screen.getByRole('img'));
    expect(screen.queryByRole('img')).not.toBeInTheDocument();
    rerender(
      <MantineProvider>
        <ComponentThumbnail
          image={{ ...image, id: 2, thumbnailUrl: '/component-images/2/thumbnail' }}
        />
      </MantineProvider>,
    );
    expect(document.querySelector('img')).toHaveAttribute(
      'src',
      '/api/component-images/2/thumbnail',
    );
  });

  it('shows a placeholder when there is no image', () => {
    const { container } = render(
      <MantineProvider>
        <ComponentThumbnail image={null} />
      </MantineProvider>,
    );
    expect(container.querySelector('img')).toBeNull();
    expect(container.querySelector('svg')).not.toBeNull();
  });
});
