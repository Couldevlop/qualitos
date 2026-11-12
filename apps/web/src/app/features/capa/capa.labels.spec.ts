import { CAPA_SOURCES, capaCriticityLabel, capaSourceLabel, capaSourceRegister } from './capa.labels';

describe('capa.labels', () => {
  it('traduit la criticité au lieu d\'afficher le code de l\'API', () => {
    expect(capaCriticityLabel('LOW')).toBe('Faible');
    expect(capaCriticityLabel('MEDIUM')).toBe('Moyenne');
    expect(capaCriticityLabel('HIGH')).toBe('Élevée');
    expect(capaCriticityLabel('CRITICAL')).toBe('Critique');
  });

  it('rend la valeur brute si la criticité est inconnue, un tiret si elle manque', () => {
    expect(capaCriticityLabel('EXTREME')).toBe('EXTREME');
    expect(capaCriticityLabel(null)).toBe('—');
  });

  it('traduit la source, y compris celles des automates', () => {
    expect(capaSourceLabel('NON_CONFORMITY')).toBe('Non-conformité interne');
    expect(capaSourceLabel('RISK')).toBe('Risque');
  });

  // « Non-conformité » tout court ne disait pas si l'écart venait de l'intérieur
  // ou d'un client. Le code de l'énumération, lui, ne change pas : un dossier
  // déjà ouvert le porte, et le renommer casserait son rattachement.
  it("nomme la non-conformité « interne » sans toucher au code de l'énumération", () => {
    const source = CAPA_SOURCES.find(s => s.value === 'NON_CONFORMITY');
    expect(source?.label).toBe('Non-conformité interne');
    expect(source?.value).toBe('NON_CONFORMITY');
  });

  // §6.1 traite le risque AVANT qu'il se réalise : obliger à passer par la
  // fiche du registre privait d'un chemin légitime.
  it('propose « Risque » à la création libre', () => {
    expect(CAPA_SOURCES.find(s => s.value === 'RISK')?.manual).toBeTrue();
  });

  /**
   * Trois sources ne s'ouvrent pas a la main. Deux viennent d'un automate
   * (carte de controle, detection d'anomalie). La troisieme -- l'ecart externe
   * -- se pose a l'escalade depuis la fiche de l'ecart, seule a savoir si
   * l'ecart vient du dehors : la proposer a la creation libre laisserait
   * declarer « externe » un dossier rattache a un constat interne.
   */
  it("n'ouvre pas à la main les sources qu'un autre écran pose", () => {
    const automates = CAPA_SOURCES.filter(s => !s.manual).map(s => s.value);
    expect(automates).toEqual(['EXTERNAL_NON_CONFORMITY', 'SPC_ALERT', 'ANOMALY']);
  });

  it('nomme « externe » la source d’un écart signalé du dehors', () => {
    const source = CAPA_SOURCES.find(s => s.value === 'EXTERNAL_NON_CONFORMITY');
    expect(source?.label).toContain('externe');
    // Aucun registre : la reference est posee par l'escalade, pas choisie ici.
    expect(source?.register).toBeUndefined();
  });

  it('dit à quel registre se choisit la référence de chaque source', () => {
    expect(capaSourceRegister('NON_CONFORMITY')).toBe('nc');
    expect(capaSourceRegister('AUDIT')).toBe('audit');
    expect(capaSourceRegister('COMPLAINT')).toBe('complaint');
    expect(capaSourceRegister('RISK')).toBe('risk');
  });

  // Une détection interne ne désigne aucun objet : le texte libre est la seule
  // réponse honnête, et imposer un choix forcerait à mentir.
  it('ne réclame aucun registre pour les sources qui ne désignent rien', () => {
    expect(capaSourceRegister('INTERNAL')).toBeUndefined();
    expect(capaSourceRegister('IOT_ALERT')).toBeUndefined();
    expect(capaSourceRegister('OTHER')).toBeUndefined();
    expect(capaSourceRegister(null)).toBeUndefined();
  });
});
