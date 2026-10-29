import type { Page } from '@playwright/test';
import { ANGULAR_LOCALE, type Lang } from './i18n';

/**
 * Oráculos de formatação calculados com o Intl do próprio navegador (mesmo fuso, configurado no
 * playwright.config.ts). Os espaços especiais do CLDR (NBSP, NNBSP) são normalizados dos dois lados.
 */
export function normalizeSpaces(text: string): string {
  return text
    .replace(/[\u00a0\u202f]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

/** Valor em BRL como a interface deve exibir no idioma informado (CurrencyPipe, 2 casas). */
export async function expectedCurrency(page: Page, value: number, lang: Lang): Promise<string> {
  const formatted = await page.evaluate(
    ([amount, locale]) =>
      new Intl.NumberFormat(locale, {
        style: 'currency',
        currency: 'BRL',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }).format(amount),
    [value, ANGULAR_LOCALE[lang]] as const,
  );
  return normalizeSpaces(formatted);
}

/** Soma monetária exata em centavos (evita erro de ponto flutuante no oráculo). */
export function sumAmounts(values: number[]): number {
  return values.reduce((cents, value) => cents + Math.round(value * 100), 0) / 100;
}

/**
 * Sequência numérica esperada de uma data/hora no formato "short" do idioma (ex.: pt → [29, 10, 2026, 16, 39];
 * en → [10, 29, 26, 4, 39]). Comparar a sequência de números evita depender de pontuação e espaços,
 * que variam entre versões do CLDR do Angular e do navegador.
 */
export async function expectedShortDateNumbers(page: Page, iso: string, lang: Lang): Promise<number[]> {
  const text = await page.evaluate(
    ([value, locale]) =>
      new Intl.DateTimeFormat(locale, { dateStyle: 'short', timeStyle: 'short' }).format(new Date(value)),
    [iso, ANGULAR_LOCALE[lang]] as const,
  );
  return numbersIn(text);
}

export function numbersIn(text: string): number[] {
  return (text.match(/\d+/g) ?? []).map(Number);
}
