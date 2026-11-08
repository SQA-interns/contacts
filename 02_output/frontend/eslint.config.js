import js from '@eslint/js';
import jsxA11y from 'eslint-plugin-jsx-a11y';
import reactHooks from 'eslint-plugin-react-hooks';
import security from 'eslint-plugin-security';
import globals from 'globals';
import tseslint from 'typescript-eslint';

const literalText = {
  selector: 'JSXText[value=/[^\\s{}]/]',
  message: 'UI text comes from the message catalogs (AR-08, UI-30); use t().',
};
const literalAttribute = {
  selector: 'JSXAttribute[name.name=/^(aria-label|placeholder|title|alt)$/] > Literal',
  message: 'UI text comes from the message catalogs (AR-08, UI-30); use t().',
};

const noHtmlFromText = {
  selector: 'JSXAttribute[name.name=dangerouslySetInnerHTML]',
  message: 'Text entered by users is shown as plain text, never as HTML (R-018-14, SR-05).',
};

export default tseslint.config(
  {
    ignores: [
      'public',
      'dist',
      'coverage',
      'playwright-report',
      'test-results',
      'node_modules',
      'src/components/ui/**',
    ],
  },
  js.configs.recommended,
  ...tseslint.configs.recommended,
  jsxA11y.flatConfigs.recommended,
  security.configs.recommended,
  {
    files: ['**/*.{ts,tsx}'],
    languageOptions: { ecmaVersion: 2022, globals: { ...globals.browser, ...globals.node } },
    plugins: { 'react-hooks': reactHooks },
    rules: {
      ...reactHooks.configs.recommended.rules,
      'no-restricted-syntax': ['error', literalText, literalAttribute, noHtmlFromText],
      // AR-03: the browser reaches data only through src/api (REST) and src/auth (Firebase).
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            { group: ['firebase', 'firebase/*'], message: 'Import Firebase only in src/auth (AR-03).' },
          ],
        },
      ],
      'no-restricted-globals': [
        'error',
        { name: 'fetch', message: 'Call the backend only through src/api (AR-03).' },
      ],
      'no-restricted-properties': [
        'error',
        { object: 'window', property: 'fetch', message: 'Call the backend only through src/api (AR-03).' },
      ],
      '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
      'security/detect-object-injection': 'off',
    },
  },
  { files: ['src/auth/**'], rules: { 'no-restricted-imports': 'off' } },
  { files: ['src/api/**'], rules: { 'no-restricted-globals': 'off', 'no-restricted-properties': 'off' } },
  {
    files: [
      '**/*.test.{ts,tsx}',
      'src/test/**',
      'e2e/**',
      'acceptance/**',
      'ui-checks/**',
      '*.config.{ts,js}',
    ],
    rules: {
      'no-restricted-syntax': 'off',
      'no-restricted-globals': 'off',
      'no-restricted-properties': 'off',
      'no-restricted-imports': 'off',
    },
  },
  // JSON answers of the REST interface in the browser tests are read without a schema type
  { files: ['acceptance/**', 'e2e/**'], rules: { '@typescript-eslint/no-explicit-any': 'off' } },
);
