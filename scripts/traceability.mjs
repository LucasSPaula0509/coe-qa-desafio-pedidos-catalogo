#!/usr/bin/env node
// Gera docs/traceability-matrix.md a partir da especificação (specs/) e dos
// casos de teste (docs/test-cases/), validando a consistência dos IDs (ADR-QA-005).
//
// Uso:
//   node scripts/traceability.mjs           # valida e (re)gera a matriz
//   node scripts/traceability.mjs --check   # valida e falha se a matriz estiver desatualizada (CI)
//
// Falha (exit 1) quando: ID de caso duplicado; referência a RN/NF/Q/H inexistente;
// RN/NF sem nenhum caso; Q/H não referenciada por nenhum caso; caso citado em
// specs/open-questions.md que não existe.

import { readFileSync, readdirSync, writeFileSync, existsSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = join(fileURLToPath(import.meta.url), '..', '..');
const paths = {
  rules: join(root, 'specs', 'business-rules.md'),
  questions: join(root, 'specs', 'open-questions.md'),
  cases: join(root, 'docs', 'test-cases'),
  tests: join(root, 'tests'),
  output: join(root, 'docs', 'traceability-matrix.md'),
};

const CASE_ID = /CT-[A-Z]{3}-\d{3}/g;
const REF_ID = /\b(?:RN|NF|Q|H)-\d{2}\b/g;
const LEVEL_ORDER = ['API', 'API+DB', 'API+FI', 'WEB', 'WEB+MOCK', 'ESTÁTICO', 'SEC', 'DAST', 'PERF'];
const AREA_NAMES = {
  ORD: 'API de Pedidos',
  CAT: 'API de Catálogo',
  INT: 'Integração',
  TRV: 'Transversais',
  WEB: 'Interface web',
  PRF: 'Performance',
  SEC: 'Segurança',
};

/** Linhas de tabelas Markdown cuja primeira célula casa com `idPattern`, mapeadas pelo cabeçalho. */
function parseTables(markdown, idPattern) {
  const rows = [];
  let header = null;
  for (const line of markdown.split(/\r?\n/)) {
    if (!line.startsWith('|')) {
      header = null;
      continue;
    }
    const cells = line.slice(1, line.trimEnd().endsWith('|') ? -1 : undefined).split('|').map((c) => c.trim());
    if (cells.every((c) => /^:?-+:?$/.test(c))) continue;
    if (!header) {
      header = cells;
      continue;
    }
    if (idPattern.test(cells[0])) {
      rows.push(Object.fromEntries(header.map((name, i) => [name, cells[i] ?? ''])));
    }
  }
  return rows;
}

function stripMarkdown(text) {
  return text.replace(/\*\*|`/g, '').trim();
}

function ruleTitle(text) {
  const bold = text.match(/^\*\*(.+?)\*\*/);
  return stripMarkdown(bold ? bold[1].replace(/\.$/, '') : text.split('.')[0]);
}

function listFiles(dir, predicate) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir).flatMap((name) => {
    const full = join(dir, name);
    if (statSync(full).isDirectory()) {
      return ['node_modules', 'target', 'dist'].includes(name) ? [] : listFiles(full, predicate);
    }
    return predicate(full) ? [full] : [];
  });
}

// --- Leitura ---------------------------------------------------------------

const rules = parseTables(readFileSync(paths.rules, 'utf8'), /^(RN|NF)-\d{2}$/).map((r) => ({
  id: r.ID,
  title: ruleTitle(r.Regra ?? r.Requisito ?? ''),
}));
const questionsMd = readFileSync(paths.questions, 'utf8');
const questions = parseTables(questionsMd, /^(Q|H)-\d{2}$/).map((r) => ({
  id: r.ID,
  title: stripMarkdown(r['Questão'] ?? r['Suspeita'] ?? ''),
  citedCases: (r['Casos afetados'] ?? r['Confirmação'] ?? '').match(CASE_ID) ?? [],
}));

const caseFiles = readdirSync(paths.cases).filter((f) => f.endsWith('.md') && f !== 'README.md').sort();
const cases = caseFiles.flatMap((file) =>
  parseTables(readFileSync(join(paths.cases, file), 'utf8'), /^CT-[A-Z]{3}-\d{3}$/).map((r) => ({
    id: r.ID,
    file,
    scenario: stripMarkdown(r['Cenário']),
    refs: [...new Set((r['Ref.'] ?? '').match(REF_ID) ?? [])],
    technique: r['Técnica'],
    priority: r['Prio'],
    level: r['Nível'],
  })),
);

// IDs de casos presentes no código das suítes (a partir da Parte 2).
const automated = new Set(
  listFiles(paths.tests, (f) => /\.(java|ts|js|mjs)$/.test(f)).flatMap((f) => readFileSync(f, 'utf8').match(CASE_ID) ?? []),
);

// --- Validação --------------------------------------------------------------

const errors = [];
const known = new Set([...rules, ...questions].map((x) => x.id));
const seen = new Set();
for (const c of cases) {
  if (seen.has(c.id)) errors.push(`${c.id}: ID duplicado (${c.file})`);
  seen.add(c.id);
  if (!c.refs.some((r) => /^(RN|NF)-/.test(r))) errors.push(`${c.id}: não referencia nenhuma RN/NF`);
  for (const ref of c.refs) if (!known.has(ref)) errors.push(`${c.id}: referência inexistente ${ref}`);
  if (!/^P[123]$/.test(c.priority)) errors.push(`${c.id}: prioridade inválida "${c.priority}"`);
  if (!LEVEL_ORDER.includes(c.level)) errors.push(`${c.id}: nível inválido "${c.level}"`);
}
const casesByRef = new Map();
for (const c of cases) for (const ref of c.refs) casesByRef.set(ref, [...(casesByRef.get(ref) ?? []), c]);
for (const x of [...rules, ...questions]) {
  if (!casesByRef.has(x.id)) errors.push(`${x.id}: nenhum caso de teste referencia esta ${x.id.split('-')[0]}`);
}
for (const id of new Set(questionsMd.match(CASE_ID) ?? [])) {
  if (!seen.has(id)) errors.push(`specs/open-questions.md cita ${id}, que não existe`);
}
for (const q of questions) {
  for (const id of q.citedCases) {
    const c = cases.find((x) => x.id === id);
    if (c && !c.refs.includes(q.id)) errors.push(`${q.id} cita ${id}, mas ${id} não referencia ${q.id}`);
  }
}

if (errors.length > 0) {
  console.error(`Rastreabilidade inconsistente (${errors.length}):\n- ${errors.join('\n- ')}`);
  process.exit(1);
}

// --- Geração ----------------------------------------------------------------

const count = (list, key) => list.reduce((acc, c) => acc.set(c[key], (acc.get(c[key]) ?? 0) + 1), new Map());
const area = (id) => id.slice(3, 6);
const caseLinks = (list) => list.map((c) => `[${c.id}](test-cases/${c.file})`).join(', ');
const status = (c) => (automated.has(c.id) ? '✅ Automatizado' : '⏳ Planejado');
const byPriority = (list) => ['P1', 'P2', 'P3'].map((p) => list.filter((c) => c.priority === p).length).join(' / ');

const areas = [...new Set(cases.map((c) => area(c.id)))];
const levels = count(cases, 'level');
const priorities = count(cases, 'priority');
const automatedCount = cases.filter((c) => automated.has(c.id)).length;

const lines = [
  '# Matriz de Rastreabilidade',
  '',
  '> Gerada por `node scripts/traceability.mjs` a partir de `specs/` e `docs/test-cases/`. **Não edite à mão.** O script também valida a consistência dos IDs (ADR-QA-005).',
  '',
  '## Resumo',
  '',
  `| Indicador | Valor |`,
  `|---|---|`,
  `| Regras funcionais (RN) | ${rules.filter((r) => r.id.startsWith('RN')).length} |`,
  `| Requisitos não funcionais (NF) | ${rules.filter((r) => r.id.startsWith('NF')).length} |`,
  `| Questões (Q) / suspeitas (H) | ${questions.filter((q) => q.id.startsWith('Q')).length} / ${questions.filter((q) => q.id.startsWith('H')).length} |`,
  `| Casos de teste | ${cases.length} |`,
  `| Cobertura de requisitos (RN/NF com ≥ 1 caso) | ${rules.length}/${rules.length} (100%) |`,
  `| Casos automatizados | ${automatedCount}/${cases.length} (${Math.round((automatedCount / cases.length) * 100)}%) |`,
  '',
  '### Casos por área e prioridade',
  '',
  '| Área | Arquivo | Casos | P1 / P2 / P3 |',
  '|---|---|---|---|',
  ...areas.map((a) => {
    const list = cases.filter((c) => area(c.id) === a);
    return `| ${AREA_NAMES[a] ?? a} | [${list[0].file}](test-cases/${list[0].file}) | ${list.length} | ${byPriority(list)} |`;
  }),
  `| **Total** | | **${cases.length}** | **${['P1', 'P2', 'P3'].map((p) => priorities.get(p) ?? 0).join(' / ')}** |`,
  '',
  '### Casos por nível de execução',
  '',
  '| Nível | Casos |',
  '|---|---|',
  ...LEVEL_ORDER.filter((l) => levels.has(l)).map((l) => `| ${l} | ${levels.get(l)} |`),
  '',
  '## Requisitos → casos',
  '',
  '| Requisito | Descrição | Casos | P1 / P2 / P3 |',
  '|---|---|---|---|',
  ...rules.map((r) => {
    const list = casesByRef.get(r.id);
    return `| ${r.id} | ${r.title} | ${caseLinks(list)} | ${byPriority(list)} |`;
  }),
  '',
  '## Questões e suspeitas → casos',
  '',
  'Uma suspeita (H) só vira Issue quando o caso indicado a reproduz.',
  '',
  '| ID | Descrição | Casos |',
  '|---|---|---|',
  ...questions.map((q) => {
    const title = q.title.length > 110 ? `${q.title.slice(0, 107).trimEnd()}…` : q.title;
    return `| ${q.id} | ${title} | ${caseLinks(casesByRef.get(q.id))} |`;
  }),
  '',
  '## Casos → requisitos e automação',
  '',
  '| Caso | Cenário | Requisitos | Prio | Nível | Automação |',
  '|---|---|---|---|---|---|',
  ...cases.map((c) => `| [${c.id}](test-cases/${c.file}) | ${c.scenario} | ${c.refs.join(', ')} | ${c.priority} | ${c.level} | ${status(c)} |`),
  '',
];

const content = lines.join('\n');
const current = existsSync(paths.output) ? readFileSync(paths.output, 'utf8') : '';

if (process.argv.includes('--check')) {
  if (current !== content) {
    console.error(`${relative(root, paths.output)} está desatualizada. Execute: node scripts/traceability.mjs`);
    process.exit(1);
  }
  console.log(`OK: rastreabilidade consistente — ${cases.length} casos, ${rules.length} requisitos, matriz atualizada.`);
} else {
  writeFileSync(paths.output, content);
  console.log(`Matriz gerada: ${relative(root, paths.output)} — ${cases.length} casos, ${rules.length} requisitos.`);
}
