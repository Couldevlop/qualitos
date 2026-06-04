import { defineConfig } from '@playwright/test';

/**
 * E2E smoke (CLAUDE.md §14.3) — la SPA est servie en configuration 'e2e'
 * (useMockApi=true, authMode='dev') : aucun backend ni Keycloak requis.
 * Objectif : valider le shell, le routing lazy et le rendu des features clés,
 * pas l'intégration serveur (couverte par les tests d'intégration backend).
 */
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  retries: process.env['CI'] ? 1 : 0,
  reporter: process.env['CI'] ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: 'http://127.0.0.1:4444',
    trace: 'on-first-retry'
  },
  webServer: {
    command: 'npx ng serve --configuration e2e --port 4444 --host 127.0.0.1',
    url: 'http://127.0.0.1:4444',
    reuseExistingServer: !process.env['CI'],
    timeout: 240_000
  }
});
