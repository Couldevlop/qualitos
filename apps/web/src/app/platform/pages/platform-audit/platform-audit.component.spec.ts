import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { PersonNameModule } from '../../../shared/person-name/person-name.module';
import { SharedModule } from '../../../shared/shared.module';
import { UiModule } from '../../../shared/ui/ui.module';
import { PlatformAuditService } from '../../platform-audit.service';
import {
  PlatformAuditAction, PlatformAuditEvent, PlatformAuditPage
} from '../../platform-audit.types';
import { ACTES_PROPOSES, LIBELLES_DES_ACTES, PlatformAuditComponent } from './platform-audit.component';

/**
 * La piste d'audit de la plateforme, vue par l'éditeur.
 *
 * <p>Les bancs qui comptent le plus ne vérifient pas que la liste s'affiche,
 * mais ce que l'écran DIT du journal : qu'un seul filtre est retenu, qu'une
 * chaîne intacte ne vaut pas un journal complet, et que l'écran n'offre aucune
 * écriture. Une piste d'audit qui laisse croire plus qu'elle ne prouve est pire
 * qu'une absence de piste d'audit.
 */
describe('PlatformAuditComponent', () => {
  let fixture: ComponentFixture<PlatformAuditComponent>;
  let component: PlatformAuditComponent;
  let svc: jasmine.SpyObj<PlatformAuditService>;

  const CLIENT = 'c0ffee00-0000-4000-8000-000000000001';
  const AUTEUR = 'a0ffee00-0000-4000-8000-000000000002';

  const acte = (sequenceNo: number, action: PlatformAuditAction = 'TENANT_CREATED',
                targetTenantId: string | null = CLIENT): PlatformAuditEvent => ({
    id: `id-${sequenceNo}`,
    sequenceNo,
    occurredAt: '2026-11-08T09:30:00Z',
    recordedAt: '2026-11-08T09:30:00Z',
    actorType: 'USER',
    actorUserId: AUTEUR,
    action,
    targetType: 'tenant',
    targetId: targetTenantId,
    targetTenantId,
    summary: `Acte numéro ${sequenceNo}`,
    payloadJson: '{"slug":"metalux"}',
    ipAddress: '203.0.113.7',
    userAgent: 'Chrome/130',
    integrityHash: 'abcdef0123456789'.repeat(4),
    previousHash: sequenceNo === 1 ? null : 'f'.repeat(64)
  });

  const page = (actes: PlatformAuditEvent[]): PlatformAuditPage => ({
    content: actes, totalElements: actes.length, totalPages: 1, number: 0, size: 50
  });

  beforeEach(async () => {
    svc = jasmine.createSpyObj<PlatformAuditService>('PlatformAuditService',
      ['list', 'verify', 'export', 'effectiveCriterion']);
    svc.list.and.returnValue(of(page([acte(3), acte(2), acte(1)])));
    svc.effectiveCriterion.and.returnValue('none');

    await TestBed.configureTestingModule({
      declarations: [PlatformAuditComponent],
      imports: [SharedModule, UiModule, PersonNameModule, NoopAnimationsModule],
      // PersonNameModule résout l'auteur d'un acte dans l'annuaire, donc par
      // HTTP : sans client simulé, chaque banc échoue sur NG0201 avant même
      // d'avoir rien vérifié.
      providers: [
        { provide: PlatformAuditService, useValue: svc },
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PlatformAuditComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  const texte = (selecteur: string): string => {
    const element = fixture.nativeElement.querySelector(selecteur) as HTMLElement | null;
    return element ? element.textContent!.trim() : '';
  };

  // ---- chargement ------------------------------------------------------------

  it('charge la piste d’audit à l’ouverture, sans filtre', () => {
    expect(svc.list).toHaveBeenCalledWith({}, 0, 50);
  });

  it('affiche les actes du plus récent au plus ancien', () => {
    const lignes = fixture.nativeElement.querySelectorAll('tr[mat-row]');
    expect(lignes.length).toBe(3);
    expect(lignes[0].textContent).toContain('#3');
  });

  it('dit en clair quand la piste est vide', () => {
    svc.list.and.returnValue(of(page([])));
    component.reinitialiser();
    fixture.detectChanges();

    expect(texte('[data-test="vide"]')).toContain('Aucun acte');
  });

  it('annonce l’échec de chargement sans vider silencieusement l’écran', () => {
    svc.list.and.returnValue(throwError(() => new Error('réseau')));
    component.reinitialiser();
    fixture.detectChanges();

    expect(texte('[data-test="erreur"]')).toBeTruthy();
  });

  // ---- filtres ---------------------------------------------------------------

  it('ne transmet pas un critère vide', () => {
    component.filtres.patchValue({ action: '', targetTenantId: '' });
    component.appliquer();

    expect(svc.list).toHaveBeenCalledWith({}, 0, 50);
  });

  it('transmet le client visé', () => {
    component.filtres.patchValue({ targetTenantId: CLIENT });
    component.appliquer();

    expect(svc.list).toHaveBeenCalledWith({ targetTenantId: CLIENT }, 0, 50);
  });

  it('refuse un identifiant mal formé plutôt que de le faire refuser par le serveur', () => {
    component.filtres.patchValue({ targetTenantId: 'metalux' });
    component.appliquer();

    // Deux appels seulement : celui de l'ouverture. Rien n'est parti.
    expect(svc.list).toHaveBeenCalledTimes(1);
    expect(component.filtres.controls.targetTenantId.invalid).toBeTrue();
  });

  it('convertit une heure locale en instant absolu', () => {
    component.filtres.patchValue({ from: '2026-11-08T08:00', to: '2026-11-08T18:00' });
    component.appliquer();

    const filtre = svc.list.calls.mostRecent().args[0];
    // `Instant` refuserait « 2026-11-08T08:00 » : sans fuseau, il n'y a pas
    // d'instant.
    expect(filtre.from).toMatch(/Z$/);
    expect(filtre.to).toMatch(/Z$/);
  });

  it('ignore une date illisible au lieu de l’envoyer', () => {
    component.filtres.patchValue({ from: 'pas-une-date' });
    component.appliquer();

    expect(svc.list.calls.mostRecent().args[0].from).toBeUndefined();
  });

  it('annonce le critère que le serveur retiendra réellement', () => {
    // Le point le plus trompeur de cet écran : saisir trois filtres n'en
    // applique qu'un. Le dire vaut mieux que de laisser l'utilisateur déduire
    // un ET inexistant de ses résultats.
    expect(component.libelleDuCritere('tenant')).toContain('client visé');
    expect(component.libelleDuCritere('action')).toContain('type d');
    expect(component.libelleDuCritere('actor')).toContain('auteur');
    expect(component.libelleDuCritere('period')).toContain('deux bornes');
    expect(component.libelleDuCritere('none')).toContain('Aucun filtre');
  });

  it('n’active « Réinitialiser » que lorsqu’un filtre est posé', () => {
    expect(component.filtreActif).toBeFalse();
    component.filtres.patchValue({ action: 'TENANT_CREATED' });
    expect(component.filtreActif).toBeTrue();
  });

  // ---- pagination ------------------------------------------------------------

  it('rabat la taille de page sur le maximum réellement servi', () => {
    // Le serveur rabote au-delà de 100 SANS le dire : le paginateur compterait
    // ses pages sur 200 pendant que le serveur en sert 100, et la moitié du
    // journal deviendrait inatteignable.
    component.surChangementDePage({ pageIndex: 2, pageSize: 500, length: 1000 });

    expect(component.taillePage).toBe(100);
    expect(svc.list).toHaveBeenCalledWith({}, 2, 100);
  });

  it('ferme le détail en changeant de page : la ligne ouverte n’y est plus', () => {
    component.ouvrirDetail(acte(3));
    component.surChangementDePage({ pageIndex: 1, pageSize: 50, length: 100 });

    expect(component.detail).toBeNull();
  });

  // ---- détail ----------------------------------------------------------------

  it('ouvre le détail depuis la ligne, sans second appel', () => {
    // La liste rend déjà tout, origine et empreintes comprises. Le serveur
    // n'expose d'ailleurs pas d'accès à l'unité : une piste d'audit se lit par
    // fenêtres.
    component.ouvrirDetail(acte(2));
    fixture.detectChanges();

    expect(texte('[data-test="detail"]')).toContain('Acte numéro 2');
  });

  it('indente le détail enregistré quand il est lisible', () => {
    expect(component.detailLisible('{"a":1}')).toBe('{\n  "a": 1\n}');
  });

  it('rend un détail illisible tel quel plutôt que rien', () => {
    expect(component.detailLisible('{ cassé')).toBe('{ cassé');
    expect(component.detailLisible(null)).toBe('');
  });

  // ---- intégrité -------------------------------------------------------------

  it('vérifie tout le journal sans bornes', () => {
    svc.verify.and.returnValue(of({ fromSeq: 1, toSeq: 3, examined: 3, intact: true, breaks: [] }));

    component.verifierTout();

    expect(svc.verify).toHaveBeenCalledWith(undefined, undefined);
    expect(component.verdict!.intact).toBeTrue();
    expect(component.fenetreDuVerdict).toBe('totalite');
  });

  it('vérifie la page affichée depuis ses bornes, sans rien faire saisir', () => {
    svc.verify.and.returnValue(of({ fromSeq: 1, toSeq: 3, examined: 3, intact: true, breaks: [] }));

    component.verifierLaPage({ from: 1, to: 3 });

    expect(svc.verify).toHaveBeenCalledWith(1, 3);
    expect(component.fenetreDuVerdict).toBe('page');
  });

  it('distingue le verdict sur la page de celui sur la totalité', () => {
    // Un « chaîne intacte » obtenu sur vingt-cinq lignes ne dit rien du
    // journal : l'écran doit porter la portée du verdict, pas seulement son
    // résultat.
    svc.verify.and.returnValue(of({ fromSeq: 1, toSeq: 3, examined: 3, intact: true, breaks: [] }));
    component.verifierLaPage({ from: 1, to: 3 });
    fixture.detectChanges();

    expect(texte('[data-test="verdict"]')).toContain('page affichée seulement');
  });

  it('dit qu’une chaîne intacte ne prouve pas un journal complet', () => {
    svc.verify.and.returnValue(of({ fromSeq: 1, toSeq: 3, examined: 3, intact: true, breaks: [] }));
    component.verifierTout();
    fixture.detectChanges();

    // La limite réelle de la mécanique : un acte jamais consigné ne laisse
    // aucun vide. La taire ferait de cet écran une fausse assurance.
    expect(texte('[data-test="verdict"]')).toContain('jamais été consigné');
  });

  it('montre chaque rupture, avec son numéro', () => {
    svc.verify.and.returnValue(of({
      fromSeq: 1, toSeq: 3, examined: 2, intact: false,
      breaks: [{ eventId: 'id-2', sequenceNo: 2, reason: 'Empreinte invalide' }]
    }));

    component.verifierTout();
    fixture.detectChanges();

    const verdict = texte('[data-test="verdict"]');
    expect(verdict).toContain('rompue');
    expect(verdict).toContain('#2');
    expect(verdict).toContain('Empreinte invalide');
  });

  it('annonce l’échec d’une vérification', () => {
    svc.verify.and.returnValue(throwError(() => new Error('réseau')));

    component.verifierTout();

    expect(component.erreurDeVerification).toBeTruthy();
    expect(component.verdict).toBeNull();
  });

  it('ne lance pas deux vérifications à la fois', () => {
    component.verificationEnCours = true;
    component.verifierTout();

    expect(svc.verify).not.toHaveBeenCalled();
  });

  // ---- export ----------------------------------------------------------------

  it('télécharge le dossier scellé et retient son sceau', () => {
    const sceau = 'abcdef0123456789'.repeat(4);
    svc.export.and.returnValue(of({
      generatedAt: '2026-11-08T09:30:00Z', fromSeq: 1, toSeq: 3, count: 3, seal: sceau,
      verification: { fromSeq: 1, toSeq: 3, examined: 3, intact: true, breaks: [] },
      events: [acte(1)]
    }));
    // Le téléchargement réel ouvrirait une fenêtre : on observe le lien créé.
    // Le détournement ne porte QUE sur <a> — Angular appelle lui aussi
    // `createElement` pour rendre le gabarit, et lui rendre toujours le même
    // nœud casse son arbre (« the new child element contains the parent »).
    const vrai = document.createElement.bind(document);
    const lien = vrai('a') as HTMLAnchorElement;
    const clic = spyOn(lien, 'click');
    spyOn(document, 'createElement').and.callFake(
      ((balise: string) => (balise === 'a' ? lien : vrai(balise))) as typeof document.createElement);
    spyOn(URL, 'createObjectURL').and.returnValue('blob:faux');
    const revocation = spyOn(URL, 'revokeObjectURL');

    component.exporter();
    fixture.detectChanges();

    expect(lien.download).toBe(`qualitos-piste-audit-1-3-${sceau.slice(0, 12)}.json`);
    expect(clic).toHaveBeenCalled();
    // Sans révocation, chaque export laisse le dossier entier en mémoire pour
    // la durée de vie de l'onglet.
    expect(revocation).toHaveBeenCalledWith('blob:faux');
    expect(texte('[data-test="sceau"]')).toContain(sceau);
  });

  it('annonce l’échec d’un export', () => {
    svc.export.and.returnValue(throwError(() => new Error('réseau')));

    component.exporter();

    expect(component.erreurExport).toBeTruthy();
  });

  it('ne lance pas deux exports à la fois', () => {
    component.exportEnCours = true;
    component.exporter();

    expect(svc.export).not.toHaveBeenCalled();
  });

  // ---- présentation ----------------------------------------------------------

  it('nomme chaque acte en clair', () => {
    // Un dossier destiné à une autorité ne doit pas porter
    // « PARTNER_CONSULTANT_ACTIVE_CHANGED ».
    for (const action of ACTES_PROPOSES) {
      expect(LIBELLES_DES_ACTES[action]).withContext(action).toBeTruthy();
      expect(component.libelleDeLActe(action)).not.toBe(action);
    }
  });

  it('propose tous les actes du contrat dans le filtre', () => {
    // Un acte consignable mais absent du sélecteur serait invisible à qui
    // cherche précisément celui-là.
    const connus = (Object.keys(LIBELLES_DES_ACTES) as PlatformAuditAction[]).sort();
    expect([...ACTES_PROPOSES].sort()).toEqual(connus);
  });

  it('montre le code brut d’un acte inconnu plutôt que rien', () => {
    expect(component.libelleDeLActe('ACTE_FUTUR' as PlatformAuditAction)).toBe('ACTE_FUTUR');
  });

  it('distingue l’acte d’une personne de celui de la machine', () => {
    expect(component.tonDeLAuteur('USER')).toBe('accent');
    expect(component.tonDeLAuteur('SYSTEM')).toBe('neutral');
    expect(component.tonDeLAuteur('SCHEDULER')).toBe('neutral');
  });

  it('abrège une empreinte sans la laisser noyer la cellule', () => {
    expect(component.empreinteCourte('a'.repeat(64))).toBe('aaaaaaaaaaaa…');
    expect(component.empreinteCourte(null)).toBe('—');
    expect(component.empreinteCourte('court')).toBe('court');
  });

  it('n’offre aucune action d’écriture', () => {
    // Le journal est en ajout seul jusque dans l'écran : ni bouton de
    // modification, ni suppression, ni ancrage déclenché depuis ici.
    const boutons = Array.from(
      fixture.nativeElement.querySelectorAll('button') as NodeListOf<HTMLButtonElement>);
    const libelles = boutons.map(b => b.textContent!.trim().toLowerCase()).join(' | ');

    expect(libelles).not.toContain('supprimer');
    expect(libelles).not.toContain('modifier');
    expect(libelles).not.toContain('ancrer');
  });
});
