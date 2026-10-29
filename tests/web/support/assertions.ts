import { expect, type Locator } from '@playwright/test';
import { expectedCurrency, normalizeSpaces } from './format';
import type { Lang } from './i18n';

/**
 * O texto do elemento é o valor em BRL formatado como a interface deve exibir no idioma.
 * Usa espera por estado (expect.poll): o valor pode ser recalculado de forma assíncrona (signals).
 */
export async function expectCurrency(locator: Locator, value: number, lang: Lang = 'pt-BR'): Promise<void> {
  const expected = await expectedCurrency(locator.page(), value, lang);
  await expect
    .poll(async () => normalizeSpaces(await locator.innerText()), { message: `valor ${value} em ${lang}` })
    .toBe(expected);
}
