#!/usr/bin/env node
// Resume relatórios JUnit XML (Surefire) por caso de teste: status, duração e primeira linha da falha.
//
// Uso: node scripts/summarize-junit.mjs <diretório-de-relatórios> [--failures]

import { readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const [dir, flag] = process.argv.slice(2);
if (!dir) {
  console.error('Uso: node scripts/summarize-junit.mjs <diretório> [--failures]');
  process.exit(2);
}

const decode = (s) =>
  s.replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&lt;/g, '<').replace(/&gt;/g, '>')
    .replace(/&#10;/g, ' ').replace(/&amp;/g, '&').replace(/\s+/g, ' ').trim();

const rows = [];
for (const file of readdirSync(dir).filter((f) => f.startsWith('TEST-') && f.endsWith('.xml'))) {
  const xml = readFileSync(join(dir, file), 'utf8');
  for (const m of xml.matchAll(/<testcase name="([^"]*)" classname="([^"]*)" time="([^"]*)"(\/>|>([\s\S]*?)<\/testcase>)/g)) {
    const body = m[5] ?? '';
    const failure = /<(failure|error) message="([^"]*)"/.exec(body);
    const skipped = /<skipped/.test(body);
    rows.push({
      name: decode(m[1]),
      cls: m[2].split('.').pop(),
      time: Number(m[3]),
      status: failure ? (failure[1] === 'error' ? 'ERRO' : 'FALHA') : skipped ? 'IGNORADO' : 'OK',
      message: failure ? decode(failure[2]).slice(0, 220) : '',
    });
  }
}

const total = rows.length;
const by = (s) => rows.filter((r) => r.status === s).length;
console.log(`casos: ${total} | ok: ${by('OK')} | falha: ${by('FALHA')} | erro: ${by('ERRO')} | ignorado: ${by('IGNORADO')}`
  + ` | duração: ${rows.reduce((a, r) => a + r.time, 0).toFixed(1)} s`);
for (const r of rows.filter((x) => flag !== '--failures' || x.status !== 'OK')) {
  console.log(`[${r.status}] ${r.cls} :: ${r.name} (${r.time}s)${r.message ? `\n        ${r.message}` : ''}`);
}
