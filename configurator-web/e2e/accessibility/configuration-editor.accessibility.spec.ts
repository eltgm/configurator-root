import { openConfigurationEditor } from '../fixtures/configuration-editor';
import { expect, test } from '../fixtures/mock-api';
import { expectNoAxeViolations } from './axe-test';

for (const scheme of ['light', 'dark']) {
  test(`${scheme} configuration replacement supports keyboard focus and accessible panels`, async ({
    page,
  }, testInfo) => {
    await page.addInitScript(`localStorage.setItem('configurator.color-scheme', '${scheme}')`);
    await openConfigurationEditor(page);
    const composition = page.getByRole('region', { name: 'Состав конфигурации' });
    const replace = composition.getByRole('button', { name: 'Заменить' }).last();
    await replace.focus();
    await replace.press('Enter');
    const heading = page.getByRole('heading', { name: 'Выбор замены' });
    await expect(heading).toBeFocused();
    await expect(page.getByRole('button', { name: 'Выбрать Замена 2.1' })).toBeVisible();
    await expectNoAxeViolations(page, testInfo);

    await page.keyboard.press('Tab');
    const cancel = page.getByRole('button', { name: 'Отменить замену' });
    await expect(cancel).toBeFocused();
    await cancel.press('Enter');
    await expect(replace).toBeFocused();
    await expectNoAxeViolations(page, testInfo);
  });
}
