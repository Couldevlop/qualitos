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
 * avec la réponse, que le vérificateur se choisisse dans l'annuaire par son
 * NOM, et qu'un « non » enregistré se relise sur la fiche.
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

  test('le vérificateur se choisit dans l\'annuaire, par son nom', async ({ page }) => {
    // L'intention d'origine tient toujours : l'utilisateur n'est jamais devant
    // une liste vide sans explication. En configuration `e2e`, l'annuaire a sa
    // démonstration (`DirectoryService`, mode maquette) : la liste propose des
    // PERSONNES, nommées — jamais un identifiant de compte. Un annuaire en
    // panne, lui, se dit dans la liste (« Annuaire indisponible »), et c'est
    // couvert par les specs du sélecteur.
    await ouvrirPremierDossier(page);
    const dialogue = await ouvrirEdition(page);

    await dialogue.locator('[data-test="verification-exigee"]')
      .getByRole('radio', { name: /^Oui$/ }).click();

    const champ = dialogue.locator('[data-test="verification-qui"] [data-test="user-picker-input"]');
    await expect(champ).toBeVisible();
    await champ.click();

    // La liste de l'autocomplétion s'ouvre dans la surcouche, hors du dialogue.
    const options = page.locator('[data-test="user-picker-option"]');
    await expect(options.first()).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('[data-test="user-picker-empty"]')).toHaveCount(0);
    await expect(page.locator('[data-test="user-picker-error"]')).toHaveCount(0);

    const UUID = /[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/i;
    const noms = await options.locator('.option__name').allInnerTexts();
    expect(noms.length).toBeGreaterThan(0);
    for (const nom of noms) {
      expect(nom.trim()).not.toBe('');
      expect(nom).not.toMatch(UUID);
    }

    // Choisir une personne l'affiche par son nom dans le champ.
    const premier = noms[0].trim();
    await options.first().click();
    await expect(champ).toHaveValue(premier);
    await expect(champ).not.toHaveValue(UUID);
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

/**
 * Le même bloc, atteint par l'autre porte : depuis une non-conformité.
 *
 * <p>Depuis l'ADR 0088, « Créer la CAPA » ouvre LA fenêtre unique, qui crée le
 * dossier et son action en une écriture. Personne ne passera ensuite par
 * « Modifier » : la question de la vérification se pose donc là, et elle y est
 * OBLIGATOIRE — c'est la même décision de clôture d'où qu'on ouvre la CAPA.
 *
 * <p>Ce banc ne vérifie que ce que le harnais rend observable (`useMockApi`,
 * aucun backend) : que le bloc soit là, qu'il exige une réponse avant de
 * laisser créer, et qu'un « oui » demande à qui et quoi vérifier.
 */
test.describe('NC — la fenêtre de création CAPA pose la vérification', () => {

  test('la question est posée, et il faut y répondre pour créer', async ({ page }) => {
    const erreurs: string[] = [];
    page.on('pageerror', (err) => erreurs.push(err.message));

    await page.goto('/nc/interne');
    await expect(page.locator('.qos-sidebar__link').first()).toBeVisible({ timeout: 15_000 });

    const lignes = page.locator('tr[mat-row]');
    await expect(lignes.first()).toBeVisible({ timeout: 15_000 });
    await lignes.first().click();

    const creer = page.locator('[data-test="creer-capa"]');
    await expect(creer).toBeVisible({ timeout: 15_000 });
    await creer.click();

    const dialogue = page.getByRole('dialog');
    // La source est celle de la NC, verrouillée.
    await expect(dialogue.locator('[data-test="source-verrouillee"]')).toBeVisible({ timeout: 10_000 });
    // Le bloc est là : sans lui, la question ne serait jamais posée sur ce chemin.
    await expect(dialogue.locator('[data-test="bloc-verification"]')).toBeVisible();

    const choix = dialogue.locator('[data-test="verification-exigee"]');
    await expect(choix).toHaveAttribute('aria-required', 'true');
    await expect(choix.getByRole('radio', { name: /^Oui$/ })).toBeVisible();
    await expect(choix.getByRole('radio', { name: /^Non$/ })).toBeVisible();
    // Aucune réponse pré-cochée : un défaut ferait passer l'oubli pour une décision.
    await expect(choix.getByRole('radio', { name: /^Oui$/ })).not.toBeChecked();
    await expect(choix.getByRole('radio', { name: /^Non$/ })).not.toBeChecked();

    // Rien de demandé tant que rien n'est répondu.
    await expect(dialogue.locator('[data-test="verification-qui"]')).toHaveCount(0);
    await expect(dialogue.locator('[data-test="verification-consignes"]')).toHaveCount(0);

    // Tout le reste rempli (intitulé repris de la NC, moi comme responsable) :
    // seule la question manque, et la création reste impossible.
    const demain = new Date(Date.now() + 86_400_000);
    const iso = `${demain.getFullYear()}-${String(demain.getMonth() + 1).padStart(2, '0')}-`
      + `${String(demain.getDate()).padStart(2, '0')}`;
    await dialogue.locator('[data-test="capa-echeance"]').fill(iso);
    const envoyer = dialogue.getByRole('button', { name: /Créer la CAPA/ });
    await expect(envoyer).toBeDisabled();

    // « Non » est une réponse : elle suffit à créer.
    await choix.getByRole('radio', { name: /^Non$/ }).click();
    await expect(envoyer).toBeEnabled();

    // « Oui » demande à qui, et quoi vérifier — et redevient bloquant tant que
    // personne n'est désigné.
    await choix.getByRole('radio', { name: /^Oui$/ }).click();
    await expect(dialogue.locator('[data-test="verification-qui"]')).toBeVisible();
    await expect(dialogue.locator('[data-test="verification-consignes"]')).toBeVisible();
    await expect(envoyer).toBeDisabled();

    // L'apparition du bloc attache ses validateurs : c'est exactement ce qui a
    // produit quatre NG0100 sur le dialogue d'édition. NG0100 est une erreur en
    // mode développement, donc `pageerror` la capterait.
    expect(erreurs).toEqual([]);
  });
});
