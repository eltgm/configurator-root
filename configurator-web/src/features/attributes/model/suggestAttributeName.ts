const russianToLatin: Readonly<Record<string, string>> = {
  а: 'a',
  б: 'b',
  в: 'v',
  г: 'g',
  д: 'd',
  е: 'e',
  ё: 'yo',
  ж: 'zh',
  з: 'z',
  и: 'i',
  й: 'y',
  к: 'k',
  л: 'l',
  м: 'm',
  н: 'n',
  о: 'o',
  п: 'p',
  р: 'r',
  с: 's',
  т: 't',
  у: 'u',
  ф: 'f',
  х: 'kh',
  ц: 'ts',
  ч: 'ch',
  ш: 'sh',
  щ: 'shch',
  ъ: '',
  ы: 'y',
  ь: '',
  э: 'e',
  ю: 'yu',
  я: 'ya',
};

/** A suggestion only: never normalize a manually entered or persisted name. */
export function suggestAttributeName(label: string): string {
  return (
    label
      .normalize('NFC')
      // Split HTTPVersion, but keep short unit abbreviations such as MHz together.
      .replace(/([A-ZА-ЯЁ]{2})([A-ZА-ЯЁ][a-zа-яё])/g, '$1_$2')
      .replace(/([a-zа-яё0-9])([A-ZА-ЯЁ])/g, '$1_$2')
      .toLowerCase()
      .replace(/[а-яё]/g, (character) => russianToLatin[character] ?? '')
      .normalize('NFD')
      .replace(/\p{M}/gu, '')
      .replace(/[^a-z0-9]+/g, '_')
      .replace(/^_+|_+$/g, '')
  );
}
