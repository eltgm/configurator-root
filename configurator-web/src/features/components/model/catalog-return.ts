const urls = new Map<number, string>();
export function rememberCatalogUrl(domainId: number | null, query: string) {
  if (domainId !== null) urls.set(domainId, `/components${query ? `?${query}` : ''}`);
}
export function catalogReturnUrl(domainId: number | null) {
  return domainId === null ? '/components' : (urls.get(domainId) ?? '/components');
}
