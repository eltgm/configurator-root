export interface TextDownload {
  content: string;
  fileName: string;
  mimeType: string;
}

interface DownloadEnvironment {
  createObjectUrl: (blob: Blob) => string;
  revokeObjectUrl: (objectUrl: string) => void;
  createAnchor: () => HTMLAnchorElement;
  appendAnchor: (anchor: HTMLAnchorElement) => void;
  scheduleCleanup: (cleanup: () => void) => void;
}

function browserDownloadEnvironment(): DownloadEnvironment {
  return {
    createObjectUrl: (blob) => URL.createObjectURL(blob),
    revokeObjectUrl: (objectUrl) => URL.revokeObjectURL(objectUrl),
    createAnchor: () => document.createElement('a'),
    appendAnchor: (anchor) => document.body.append(anchor),
    scheduleCleanup: (cleanup) => window.setTimeout(cleanup, 0),
  };
}

export function downloadTextFile(
  download: TextDownload,
  environment: DownloadEnvironment = browserDownloadEnvironment(),
) {
  const blob = new Blob([download.content], { type: download.mimeType });
  const objectUrl = environment.createObjectUrl(blob);
  const anchor = environment.createAnchor();
  anchor.href = objectUrl;
  anchor.download = download.fileName;
  anchor.hidden = true;
  environment.appendAnchor(anchor);
  anchor.click();
  anchor.remove();
  environment.scheduleCleanup(() => environment.revokeObjectUrl(objectUrl));
}
