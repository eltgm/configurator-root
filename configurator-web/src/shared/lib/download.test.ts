import { describe, expect, it, vi } from 'vitest';

import { downloadTextFile } from '@/shared/lib/download';

describe('downloadTextFile', () => {
  it('downloads a typed blob and cleans up the temporary URL and anchor', async () => {
    const anchor = document.createElement('a');
    const click = vi.spyOn(anchor, 'click').mockImplementation(() => undefined);
    const remove = vi.spyOn(anchor, 'remove');
    const revokeObjectUrl = vi.fn();
    let blob: Blob | undefined;

    downloadTextFile(
      {
        content: '{\n  "schemaVersion": 1\n}\n',
        fileName: 'configuration-41.json',
        mimeType: 'application/json;charset=utf-8',
      },
      {
        createObjectUrl: (createdBlob) => {
          blob = createdBlob;
          return 'blob:configuration-41';
        },
        revokeObjectUrl,
        createAnchor: () => anchor,
        appendAnchor: (createdAnchor) => document.body.append(createdAnchor),
        scheduleCleanup: (cleanup) => cleanup(),
      },
    );

    expect(anchor.download).toBe('configuration-41.json');
    expect(anchor.href).toBe('blob:configuration-41');
    expect(click).toHaveBeenCalledOnce();
    expect(remove).toHaveBeenCalledOnce();
    expect(revokeObjectUrl).toHaveBeenCalledWith('blob:configuration-41');
    expect(blob?.type).toBe('application/json;charset=utf-8');
    await expect(blob?.text()).resolves.toBe('{\n  "schemaVersion": 1\n}\n');
  });
});
