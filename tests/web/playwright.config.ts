import { defineConfig, devices } from '@playwright/test';

/**
 * Suíte E2E da interface web (ADR-QA-003). Endereços sobrescrevíveis por variável de ambiente:
 * WEB_BASE_URL (front-end) e ORDER_API_URL / CATALOG_API_URL (APIs usadas na preparação de dados
 * e como oráculo).
 */
const isCI = !!process.env.CI;

export default defineConfig({
  testDir: './specs',
  // Um worker: o Painel e a listagem dependem do estado global de pedidos, e casos concorrentes
  // criando pedidos tornariam os oráculos não determinísticos (estratégia §11).
  workers: 1,
  fullyParallel: false,
  forbidOnly: isCI,
  // Uma nova tentativa só no CI. Um teste que passa só na nova tentativa é marcado como instável
  // no relatório (estratégia §12, política de instabilidade).
  retries: isCI ? 1 : 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [
    ['list'],
    ['html', { open: 'never', outputFolder: 'playwright-report' }],
    ['junit', { outputFile: 'test-results/junit.xml' }],
  ],
  use: {
    baseURL: process.env.WEB_BASE_URL ?? 'http://localhost:4200',
    // Fuso e locale do navegador fixos: os oráculos de data e moeda usam o Intl do próprio navegador.
    timezoneId: 'America/Sao_Paulo',
    locale: 'pt-BR',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 10_000,
    navigationTimeout: 20_000,
  },
  projects: [
    // Gate obrigatório (estratégia §12, G4).
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    // Job estendido, informativo: npx playwright install firefox webkit
    { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
  ],
});
