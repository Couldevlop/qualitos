import { expect, Page, test } from '@playwright/test';

/**
 * Smoke E2E — navigation des features clés en mode mock (auth bypassée).
 * Chaque route doit : charger son bundle lazy, rendre le shell (sidebar) et
 * ne lever aucune erreur JS non gérée (pageerror).
 */

const ROUTES: Array<{ path: string; label: string }> = [
  { path: '/home', label: 'accueil' },
  { path: '/dashboard', label: 'dashboard exécutif' },
  { path: '/pdca', label: 'cycles PDCA' },
  { path: '/ishikawa', label: 'diagrammes Ishikawa' },
  { path: '/fives', label: 'audits 5S' },
  { path: '/capa', label: 'CAPA' },
  { path: '/standards', label: 'Standards Hub' },
  { path: '/suppliers', label: 'fournisseurs' },
  { path: '/nlq', label: 'NLQ' }
];

function collectPageErrors(page: Page): string[] {
  const errors: string[] = [];
  page.on('pageerror', (err) => errors.push(err.message));
  return errors;
}

test.describe('QualitOS — smoke navigation', () => {
  test('le shell se charge avec la navigation', async ({ page }) => {
    const errors = collectPageErrors(page);
    await page.goto('/');
    // La sidebar du MainShell doit proposer plusieurs entrées de navigation.
    const links = page.locator('.qos-sidebar__link');
    await expect(links.first()).toBeVisible({ timeout: 15_000 });
    expect(await links.count()).toBeGreaterThan(5);
    expect(errors).toEqual([]);
  });

  for (const route of ROUTES) {
    test(`la route ${route.path} (${route.label}) rend sans erreur JS`, async ({ page }) => {
      const errors = collectPageErrors(page);
      await page.goto(route.path);
      // Le shell reste présent (pas de page blanche ni de redirect d'erreur)…
      await expect(page.locator('.qos-sidebar__link').first()).toBeVisible({ timeout: 15_000 });
      // …et le contenu routé est rendu (l'outlet a produit un composant).
      await expect(page.locator('router-outlet ~ *').first()).toBeAttached();
      expect(errors).toEqual([]);
    });
  }
});
