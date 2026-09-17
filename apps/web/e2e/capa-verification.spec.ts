import { expect, Page, test } from '@playwright/test';

/**
 * Vérification d'efficacité CAPA (V132) — le parcours vu par l'utilisateur.
 *
 * <p><b>Pourquoi ce test existe.</b> La règle que porte cet écran n'est pas
 * « un champ de plus » : c'est la distinction entre un dossier qu'on a
 * délibérément choisi de ne pas vérifier et un dossier qu'on a oublié de
 * vérifier. Elle se joue dans l'apparition conditionnelle du bloc, et c'est
 * exactement le genre de logique qu'un test unitaire de composant valide en
 * isolation alors qu'elle casse à l'assemblage — ce lot a d'ailleurs coûté
 * quatre passes de NG0100, toutes nées de cette apparition conditionnelle.
 *
 * <p><b>Portée assumée.</b> Le harnais sert la SPA en configuration `e2e`
 * (`useMockApi=true`, `authMode='dev'`) : aucun backend, aucun Keycloak. Les
 * trois gardes du serveur — exiger sans désigner (422), désigner sans exiger
 * (422), ne plus exiger efface le reste — sont tenues par `CapaService` côté
 * Java et couvertes par `CapaServiceTest`. Ce qui se vérifie ICI est ce que le
 * harnais rend réellement observable : que le bloc apparaisse et disparaisse
 * avec la réponse, que l'annuaire absent soit DIT au lieu d'offrir une liste
 * vide, et qu'un « non » enregistré se relise sur la fiche.
 *
 * <p>Aucun compteur codé en dur ici : le jeu de démonstration bouge, et un
 * `toHaveCount(3)` transforme un enrichissement des données en échec de test.
 */

/** Ouvre le premier dossier de la liste et rend la main sur sa fiche. */
async function ouvrirPremierDossier(page: Page): Promise<void> {
  await page.goto('/capa');
  await expect(page.locator('.qos-sidebar__link').first()).toBeVisible({ timeout: 15_000 });

  const lignes = page.locator('tr[mat-row]');
  await expect(lignes.first()).toBeVisible({ timeout: 15_000 });
  await lignes.first().click();

  // La fiche est arrivée quand ses actions sont là — le titre, lui, affiche
  // « Chargement… » puis le vrai libellé, et l'attendre par son texte ferait
  // dépendre le test du jeu de démonstration.
  await expect(page.getByRole('button', { name: /Modifier/ }).first())
    .toBeVisible({ timeout: 15_000 });
}

/** Ouvre le dialogue d'édition depuis la fiche. */
async function ouvrirEdition(page: Page) {
  await page.getByRole('button', { name: /Modifier/ }).first().click();
  const dialogue = page.getByRole('dialog');
  await expect(dialogue.locator('[data-test="bloc-verification"]')).toBeVisible({ timeout: 10_000 });
  return dialogue;
}

test.describe('CAPA — vérification d\'efficacité', () => {

  test('le dossier pose la question, et « non » est une réponse possible', async ({ page }) => {
    await ouvrirPremierDossier(page);
    const dialogue = await ouvrirEdition(page);

    // Les deux réponses sont offertes. Ne proposer que « oui » reviendrait à
    // faire de l'absence de réponse un « non » implicite — précisément ce que
    // cette fonctionnalité corrige.
    const choix = dialogue.locator('[data-test="verification-exigee"]');
    await expect(choix.getByRole('radio', { name: /^Oui$/ })).toBeVisible();
    await expect(choix.getByRole('radio', { name: /^Non$/ })).toBeVisible();

    // Tant que rien n'est répondu, on ne demande ni à qui ni quoi vérifier.
    await expect(dialogue.locator('[data-test="verification-qui"]')).toHaveCount(0);
    await expect(dialogue.locator('[data-test="verification-consignes"]')).toHaveCount(0);
  });

  test('répondre « oui » demande à qui, et quoi vérifier', async ({ page }) => {
    const erreurs: string[] = [];
    page.on('pageerror', (err) => erreurs.push(err.message));

    await ouvrirPremierDossier(page);
    const dialogue = await ouvrirEdition(page);

    await dialogue.locator('[data-test="verification-exigee"]')
      .getByRole('radio', { name: /^Oui$/ }).click();

    await expect(dialogue.locator('[data-test="verification-qui"]')).toBeVisible();
    await expect(dialogue.locator('[data-test="verification-consignes"]')).toBeVisible();

    // L'apparition du bloc attache un validateur `required` : c'est elle qui a
    // produit quatre NG0100 successifs pendant le développement. NG0100 est une
    // ERREUR en mode développement, donc `pageerror` la capterait.
    expect(erreurs).toEqual([]);
  });

  test('revenir à « non » referme la question au lieu de la laisser pendante', async ({ page }) => {
    await ouvrirPremierDossier(page);
    const dialogue = await ouvrirEdition(page);

    const choix = dialogue.locator('[data-test="verification-exigee"]');
    await choix.getByRole('radio', { name: /^Oui$/ }).click();
    await expect(dialogue.locator('[data-test="verification-consignes"]')).toBeVisible();

    await choix.getByRole('radio', { name: /^Non$/ }).click();

    // Laisser traîner un vérificateur et des consignes sur un dossier qui
    // n'exige plus de vérification laisserait croire qu'une vérification est
    // attendue. Le serveur les efface ; l'écran, lui, cesse de les demander.
    await expect(dialogue.locator('[data-test="verification-qui"]')).toHaveCount(0);
    await expect(dialogue.locator('[data-test="verification-consignes"]')).toHaveCount(0);
  });

  test('un annuaire muet est annoncé, pas déguisé en liste vide', async ({ page }) => {
    // En configuration `e2e` il n'y a pas de backend : `/api/v1/users` échoue.
    // C'est le cas réel qu'on veut voir — un vérificateur qu'on ne peut pas
    // désigner doit se DIRE, sinon l'utilisateur ouvre une liste vide et croit
    // que son organisation ne compte personne.
    await ouvrirPremierDossier(page);
    const dialogue = await ouvrirEdition(page);

    await dialogue.locator('[data-test="verification-exigee"]')
      .getByRole('radio', { name: /^Oui$/ }).click();

    await expect(dialogue.locator('[data-test="annuaire-indisponible"]'))
      .toBeVisible({ timeout: 10_000 });
  });

  test('un « non » enregistré se relit sur la fiche', async ({ page }) => {
    await ouvrirPremierDossier(page);
    const dialogue = await ouvrirEdition(page);

    await dialogue.locator('[data-test="verification-exigee"]')
      .getByRole('radio', { name: /^Non$/ }).click();
    await dialogue.getByRole('button', { name: /Enregistrer|Valider|Modifier/ }).last().click();

    // La fiche n'affiche la ligne QUE si la question a été tranchée — un dossier
    // sans décision ne doit rien annoncer du tout (§ « NULL n'est pas false »).
    const ligne = page.locator('[data-test="verification-exigee"]');
    await expect(ligne).toBeVisible({ timeout: 10_000 });
    await expect(ligne).toContainText(/Non exig[ée]e/);
  });
});
