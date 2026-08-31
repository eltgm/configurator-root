import type { Locator, Page } from '@playwright/test';

import { openConfigurationEditor } from './fixtures/configuration-editor';
import { expect, test } from './fixtures/mock-api';

async function expectUnobscured(page: Page, target: Locator) {
  await expect(target).toBeInViewport({ ratio: 1 });
  await expect
    .poll(async () => {
      const box = await target.boundingBox();
      const header = await page.getByRole('banner').boundingBox();
      const footer = await page.getByRole('contentinfo', { includeHidden: true }).boundingBox();
      return Boolean(
        box &&
        header &&
        box.y >= header.y + header.height &&
        box.y + box.height <= (footer?.y ?? page.viewportSize()!.height),
      );
    })
    .toBe(true);
}

for (const viewport of [
  { name: 'desktop', width: 1440, height: 900 },
  { name: 'tablet', width: 900, height: 900 },
  { name: 'mobile', width: 390, height: 844 },
]) {
  test(`${viewport.name}: replacement navigates both ways through a long composition`, async ({
    page,
  }, testInfo) => {
    await page.setViewportSize(viewport);
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await openConfigurationEditor(page, 50);
    const composition = page.getByRole('region', { name: 'Состав конфигурации' });
    const browser = page.getByRole('region', { name: 'Доступные компоненты' });
    await expect(composition.getByRole('button', { name: 'Заменить' })).toHaveCount(50);
    const compositionBox = (await composition.boundingBox())!;
    const browserBox = (await browser.boundingBox())!;
    if (viewport.name === 'desktop') {
      expect(browserBox.x + browserBox.width).toBeLessThan(compositionBox.x);
      expect(Math.abs(browserBox.y - compositionBox.y)).toBeLessThan(2);
    } else {
      expect(browserBox.y).toBeGreaterThanOrEqual(compositionBox.y + compositionBox.height);
    }
    const lastReplace = composition.getByRole('button', { name: 'Заменить' }).last();
    await lastReplace.click();
    const heading = page.getByRole('heading', { name: 'Выбор замены' });
    await expect(heading).toBeFocused();
    await expectUnobscured(page, heading);
    await expect(composition.getByRole('button', { name: 'Заменить', pressed: true })).toHaveCount(
      1,
    );
    const replacementBrowser = page.getByRole('region', { name: 'Выбор замены' });
    await expect(replacementBrowser.getByText(/«Компонент 50»/)).toBeVisible();
    if (viewport.name !== 'desktop') {
      await expect.poll(async () => (await heading.boundingBox())!.y).toBeLessThanOrEqual(104);
    }
    await expectUnobscured(
      page,
      replacementBrowser.getByRole('button', { name: 'Отменить замену' }),
    );
    await expectUnobscured(
      page,
      replacementBrowser.getByRole('button', { name: 'Выбрать Замена 50.1' }),
    );
    if (viewport.name === 'desktop') {
      await expectUnobscured(
        page,
        composition.getByRole('button', { name: 'Сохранить изменения' }),
      );
    }
    await testInfo.attach(`replacement-${viewport.name}.png`, {
      body: await page.screenshot(),
      contentType: 'image/png',
    });
    await replacementBrowser.getByRole('button', { name: 'Отменить замену' }).click();
    await expect(lastReplace).toBeFocused();
    await expectUnobscured(page, lastReplace);
    await expect(composition.getByRole('button', { name: 'Сохранить изменения' })).toBeDisabled();
    await lastReplace.press('Enter');
    await expect(heading).toBeFocused();
    await expectUnobscured(page, heading);
    await replacementBrowser.getByRole('button', { name: 'Выбрать Замена 50.1' }).click();
    await expect(composition.getByRole('link', { name: 'Замена 50.1', exact: true })).toBeVisible();
    await expect(lastReplace).toBeFocused();
    await expectUnobscured(page, lastReplace);
    await expect(composition.getByRole('button', { name: 'Сохранить изменения' })).toBeEnabled();
    expect(await page.evaluate('document.documentElement.scrollWidth <= window.innerWidth')).toBe(
      true,
    );
  });
}

test('switching replacement resets search and pagination without losing metadata', async ({
  page,
}) => {
  await page.setViewportSize({ width: 1440, height: 900 });
  await openConfigurationEditor(page);
  await page.getByRole('textbox', { name: 'Название', exact: true }).fill('Изменённое название');
  const composition = page.getByRole('region', { name: 'Состав конфигурации' });
  const replaceButtons = composition.getByRole('button', { name: 'Заменить' });
  const initialScroll = await page.evaluate<number>('window.scrollY');
  await replaceButtons.first().click();
  await expect(page.getByRole('heading', { name: 'Выбор замены' })).toBeFocused();
  expect(await page.evaluate<number>('window.scrollY')).toBe(initialScroll);
  const browser = page.getByRole('region', { name: 'Выбор замены' });
  await browser.getByRole('button', { name: 'Страница 2', exact: true }).click();
  await expect(
    browser.getByRole('button', { name: 'Выбрать Замена 1.13', exact: true }),
  ).toBeVisible();
  await replaceButtons.last().click();
  await expect(browser.getByRole('button', { name: 'Выбрать Замена 2.1' })).toBeVisible();
  const search = browser.getByRole('textbox', { name: 'Поиск компонентов' });
  await search.fill('Несуществующий компонент');
  await replaceButtons.first().click();
  await expect(search).toHaveValue('');
  await expect(
    browser.getByRole('button', { name: 'Выбрать Замена 1.1', exact: true }),
  ).toBeVisible();
  await browser.getByRole('button', { name: 'Выбрать Замена 1.1', exact: true }).click();
  const saved = page.waitForRequest((request) => request.method() === 'PUT');
  await composition.getByRole('button', { name: 'Сохранить изменения' }).click();
  expect((await saved).postDataJSON()).toEqual({
    name: 'Изменённое название',
    description: 'Сохранённое описание',
    componentIds: [10000, 1001],
  });
  await expect(page).toHaveURL(/\/configurations\/990$/);
});
