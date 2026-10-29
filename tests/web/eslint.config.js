import js from '@eslint/js';
import playwright from 'eslint-plugin-playwright';
import tseslint from 'typescript-eslint';

export default tseslint.config(
  { ignores: ['node_modules', 'playwright-report', 'test-results'] },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  {
    files: ['specs/**/*.ts'],
    ...playwright.configs['flat/recommended'],
    rules: {
      ...playwright.configs['flat/recommended'].rules,
      // Espera fixa é proibida (estratégia §11): só esperas por estado.
      'playwright/no-wait-for-timeout': 'error',
      'playwright/no-force-option': 'error',
      'playwright/no-skipped-test': 'error',
      // Asserções vivem também em helpers de página (ex.: expectToast).
      'playwright/expect-expect': ['error', { assertFunctionNames: ['expectCurrency', 'expectToast'] }],
    },
  },
);
