#!/usr/bin/env node
// Resume relatórios JUnit XML (Surefire ou Playwright) por caso de teste: status, duração e
// primeira linha da falha.
//
// Uso: node scripts/summarize-junit.mjs <diretório-ou-arquivo> [--failures] [--markdown "Título"]
//   --failures   lista só falhas e erros
//   --markdown   saída em Markdown (para o resumo do job no GitHub Actions)

import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

const args = process.argv.slice(2);
const target = args[0];
const onlyFailures = args.includes('--failures');
const markdownIndex = args.indexOf('--markdown');
const markdownTitle = markdownIndex >= 0 ? args[markdownIndex + 1] ?? 'Resultado' : null;

if (!target) {
  console.error('Uso: node scripts/summarize-junit.mjs <diretório-ou-arquivo> [--failures] [--markdown "Título"]');
  process.exit(2);
}
if (!existsSync(target)) {
  console.log(markdownTitle ? `### ${markdownTitle}\n\nSem relatório JUnit em \`${target}\`.` : `Sem relatório em ${target}`);
  process.exit(0);
}

const decode = (s) =>
  s.replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&lt;/g, '<').replace(/&gt;/g, '>')
    .replace(/&#10;/g, ' ').replace(/&amp;/g, '&').replace(/\s+/g, ' ').trim();

const files = statSync(target).isDirectory()
  ? readdirSync(target).filter((f) => f.endsWith('.xml')).map((f) => join(target, f))
  : [target];

const rows = [];
for (const file of files) {
  const xml = readFileSync(file, 'utf8');
  for (const m of xml.matchAll(/<testcase name="([^"]*)" classname="([^"]*)" time="([^"]*)"(\/>|>([\s\S]*?)<\/testcase>)/g)) {
    const body = m[5] ?? '';
    const failure = /<(failure|error) message="([^"]*)"/.exec(body);
    const skipped = /<skipped/.test(body);
    rows.push({
      name: decode(m[1]),
      // Surefire: nome qualificado da classe (fica o último segmento); Playwright: arquivo da spec.
      cls: /\.(spec|test)\.[jt]s$/.test(m[2]) ? m[2] : m[2].split('.').pop(),
      time: Number(m[3]),
      status: failure ? (failure[1] === 'error' ? 'ERRO' : 'FALHA') : skipped ? 'IGNORADO' : 'OK',
      message: failure ? decode(failure[2]).slice(0, 220) : '',
    });
  }
}

const count = (s) => rows.filter((r) => r.status === s).length;
const duration = rows.reduce((a, r) => a + r.time, 0).toFixed(1);
const listed = rows.filter((r) => !onlyFailures || r.status !== 'OK');

if (markdownTitle) {
  console.log(`### ${markdownTitle}\n`);
  console.log('| Casos | Aprovados | Falhas | Erros | Ignorados | Duração |\n|---|---|---|---|---|---|');
  console.log(`| ${rows.length} | ${count('OK')} | ${count('FALHA')} | ${count('ERRO')} | ${count('IGNORADO')} | ${duration} s |\n`);
  if (listed.length > 0) {
    console.log('| Status | Classe | Caso | Mensagem |\n|---|---|---|---|');
    for (const r of listed) {
      console.log(`| ${r.status} | ${r.cls} | ${r.name.replace(/\|/g, '\\|')} | ${r.message.replace(/\|/g, '\\|')} |`);
    }
  }
} else {
  console.log(`casos: ${rows.length} | ok: ${count('OK')} | falha: ${count('FALHA')} | erro: ${count('ERRO')} | ignorado: ${count('IGNORADO')} | duração: ${duration} s`);
  for (const r of listed) {
    console.log(`[${r.status}] ${r.cls} :: ${r.name} (${r.time}s)${r.message ? `\n        ${r.message}` : ''}`);
  }
}
