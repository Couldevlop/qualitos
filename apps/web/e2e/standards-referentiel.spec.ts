import { expect, test } from '@playwright/test';

/**
 * Référentiel « procédure interne » (§8) — ce que le harnais E2E peut tenir.
 *
 * <p><b>Portée assumée.</b> Ce harnais sert la SPA en configuration `e2e`
 * (`useMockApi=true`, `authMode='dev'`) : aucun backend, aucun Keycloak. Les
 * routes d'ÉCRITURE du référentiel — création depuis une procédure, saisie de
 * l'arborescence, génération de la checklist — n'ont volontairement aucune
 * branche de démonstration : des exigences fabriquées côté client donneraient un
 * audit qui ne vérifie rien. Le parcours d'écriture complet ne peut donc pas
 * être joué ici ; il est couvert par les tests du moteur
 * (`ProcedureStandardServiceTest`, `ProcedureStandardEditingTest`,
 * `AuditChecklistFromStandardTest`) et par les specs des écrans.
 *
 * <p>Ce qui se vérifie ici est ce que le harnais rend réellement observable :
 * qu'un référentiel du tenant se DISTINGUE d'une norme livrée, que le filtre le
 * retrouve, et que l'entrée de création existe et s'ouvre. C'est exactement ce
 * qu'un utilisateur cherche des yeux en arrivant sur l'écran.
 */
test.describe('Standards Hub — référentiels du tenant', () => {

  test.beforeEach(async ({ page }) => {
    await page.goto('/standards');
    await expect(page.locator('.qos-sidebar__link').first()).toBeVisible({ timeout: 15_000 });
  });

  test('distingue à l\'œil un référentiel maison d\'une norme livrée', async ({ page }) => {
    const badges = page.locator('.owned-badge');
    await expect(badges).toHaveCount(1);
    await expect(badges.first()).toHaveText(/Proc[ée]dure interne/);
  });

  test('le filtre ramène les procédures internes seules', async ({ page }) => {
    const rows = page.locator('.section-card:last-of-type table tbody tr');
    await expect(rows).toHaveCount(3);

    await page.locator('.scope-select').click();
    await page.getByRole('option', { name: /Proc[ée]dures internes/ }).click();

    await expect(rows).toHaveCount(1);
    await expect(rows.first()).toContainText('PRO-002');
  });

  test('propose de créer un référentiel depuis une procédure', async ({ page }) => {
    await page.locator('.create-procedure-btn').click();

    // La boîte s'ouvre et annonce ce qu'elle fera du document choisi.
    await expect(page.getByRole('dialog')).toContainText(/proc[ée]dure/i);
  });
});
