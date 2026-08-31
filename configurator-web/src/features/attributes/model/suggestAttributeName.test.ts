import { describe, expect, it } from 'vitest';

import { suggestAttributeName } from './suggestAttributeName';

describe('attribute system name suggestions', () => {
  it.each([
    ['Объём памяти', 'obyom_pamyati'],
    ['Memory Size', 'memory_size'],
    ['memorySize', 'memory_size'],
    ['HTTPVersion', 'http_version'],
    ['Разъём USB Type-C', 'razyom_usb_type_c'],
    ['  Частота — MHz  ', 'chastota_mhz'],
    ['ОБЪЁМ ПАМЯТИ', 'obyom_pamyati'],
    ['Объе\u0308м и и\u0306од', 'obyom_i_yod'],
    [
      'Ёжик, чай, щука, юла, якорь, эхо, цапля, мышь',
      'yozhik_chay_shchuka_yula_yakor_ekho_tsaplya_mysh',
    ],
    ['12 Вольт / DDR4', '12_volt_ddr4'],
    ['already_snake_case', 'already_snake_case'],
    [' __  USB...Type--C / Size  __ ', 'usb_type_c_size'],
    ['Crème brûlée', 'creme_brulee'],
    ['', ''],
    ['   ', ''],
    ['💻 !!! ьъ', ''],
    ['中文', ''],
  ])('suggests %j → %j', (label, expected) => {
    expect(suggestAttributeName(label)).toBe(expected);
  });

  it('does not silently truncate a transliteration longer than the form limit', () => {
    expect(suggestAttributeName('щ'.repeat(65))).toBe('shch'.repeat(65));
  });
});
