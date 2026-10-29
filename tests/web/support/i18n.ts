import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * Oráculo dos textos da interface: os próprios arquivos de tradução do SUT
 * (sut/frontend/public/assets/i18n). O mesmo teste roda nos três idiomas e uma mudança de texto
 * não quebra seletores (ADR-QA-003).
 */
export type Lang = 'pt-BR' | 'en' | 'es';
export const LANGS: readonly Lang[] = ['pt-BR', 'en', 'es'];

/** Locale do Angular usado por cada idioma (LANGUAGE_LOCALES em sut/.../locale.service.ts, docs/i18n.md). */
export const ANGULAR_LOCALE: Record<Lang, string> = { 'pt-BR': 'pt', en: 'en', es: 'es' };

const I18N_DIR = resolve(dirname(fileURLToPath(import.meta.url)), '../../../sut/frontend/public/assets/i18n');

type Dictionary = { [key: string]: string | Dictionary };
const cache = new Map<Lang, Dictionary>();

export function dictionary(lang: Lang): Dictionary {
  let dict = cache.get(lang);
  if (!dict) {
    dict = JSON.parse(readFileSync(resolve(I18N_DIR, `${lang}.json`), 'utf8')) as Dictionary;
    cache.set(lang, dict);
  }
  return dict;
}

/** Texto traduzido com interpolação de parâmetros ({{id}}, {{ ids }}). Falha se a chave não existir. */
export function t(key: string, lang: Lang = 'pt-BR', params: Record<string, string | number> = {}): string {
  const value = key
    .split('.')
    .reduce<
      string | Dictionary | undefined
    >((node, part) => (typeof node === 'object' ? node[part] : undefined), dictionary(lang));
  if (typeof value !== 'string') {
    throw new Error(`Chave de tradução inexistente: "${key}" (${lang})`);
  }
  return value.replace(/\{\{\s*(\w+)\s*\}\}/g, (match, name: string) =>
    name in params ? String(params[name]) : match,
  );
}

/** Todas as chaves (pontilhadas) e valores de um idioma. */
export function flatten(lang: Lang): Map<string, string> {
  const result = new Map<string, string>();
  const walk = (node: Dictionary, prefix: string) => {
    for (const [key, value] of Object.entries(node)) {
      if (typeof value === 'string') {
        result.set(prefix + key, value);
      } else {
        walk(value, `${prefix}${key}.`);
      }
    }
  };
  walk(dictionary(lang), '');
  return result;
}
