import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { PdcaService } from '../../pdca.service';
import { PdcaCycleResponse, PdcaStatus, PdcaStepEvidence, PdcaStepResponse } from '../../pdca.types';
import { PdcaDetailComponent } from './pdca-detail.component';

const ID = '11111111-1111-1111-1111-111111111111';

function buildCycle(overrides: Partial<PdcaCycleResponse> = {}): PdcaCycleResponse {
  return {
    id: ID, tenantId: 't1', title: 'Réduction des défauts de soudure',
    description: 'Objectif -30% NC en 90j.', status: 'DO', ownerId: 'u1',
    createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-02T00:00:00Z',
    steps: [], ...overrides
  };
}

const STEP: PdcaStepResponse = {
  id: 's1', cycleId: ID, phase: 'PLAN', title: 'Analyse Pareto', status: 'DONE',
  createdAt: '2026-07-01T00:00:00Z', updatedAt: '2026-07-01T00:00:00Z'
};

/**
 * La fiche cycle porte les transitions de la roue de Deming : chaque garde
 * (identifiant, état terminal, action déjà en vol) évite une transition
 * illégitime que le serveur refuserait — ou pire, appliquerait deux fois.
 */
describe('PdcaDetailComponent', () => {
  let fixture: ComponentFixture<PdcaDetailComponent>;
  let component: PdcaDetailComponent;
  let pdca: jasmine.SpyObj<PdcaService>;
  let router: Router;
  let routeId: string;

  /** Monte le composant : le premier reload$ précède l'abonnement de la vue. */
  function setup(): void {
    fixture = TestBed.createComponent(PdcaDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    (component as unknown as { reload$: { next(v: void): void } }).reload$.next();
    fixture.detectChanges();
  }

  beforeEach(async () => {
    routeId = ID;
    pdca = jasmine.createSpyObj<PdcaService>('PdcaService',
      ['getCycle', 'advanceCycle', 'cancelCycle',
       'listStepEvidences', 'uploadStepEvidence', 'deleteStepEvidence']);
    pdca.getCycle.and.returnValue(of(buildCycle()));
    pdca.listStepEvidences.and.returnValue(of([]));

    await TestBed.configureTestingModule({
      declarations: [PdcaDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: PdcaService, useValue: pdca },
        // paramMap lu à la volée : chaque test choisit l'identifiant de route
        // avant de monter le composant.
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => routeId } } } }
      ]
    }).compileComponents();

    router = TestBed.inject(Router);
    spyOn(router, 'navigate').and.resolveTo(true);
  });

  // --- garde sur l'identifiant de route ---------------------------------------

  it('refuse un identifiant malformé et renvoie vers la liste sans appeler l\'API', () => {
    routeId = '../../admin';
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    setup();

    expect(pdca.getCycle).not.toHaveBeenCalled();
    expect(router.navigate).toHaveBeenCalledWith(['/pdca']);
    expect(snackSpy).toHaveBeenCalled();
    expect(component.cycle$).toBeUndefined();
  });

  // --- chargement --------------------------------------------------------------

  it('charge le cycle et rend ses étapes', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    setup();

    expect(pdca.getCycle).toHaveBeenCalledWith(ID);
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('h1')?.textContent).toContain('Réduction des défauts de soudure');
    expect(el.querySelectorAll('.steps-table tbody tr').length).toBe(1);
    expect(el.querySelector('.empty')).toBeNull();
  });

  it('invite à créer la première étape quand le cycle est vide', () => {
    setup();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.steps-table')).toBeNull();
    expect(el.querySelector('.empty')).toBeTruthy();
  });

  it('affiche un message sûr et aucune fiche quand le cycle est introuvable', fakeAsync(() => {
    pdca.getCycle.and.returnValue(throwError(() => new HttpErrorResponse({ status: 404 })));
    setup();
    tick();                 // deferredView publie l'état en macrotâche
    fixture.detectChanges();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.state-row.error')?.textContent).toContain('Cycle introuvable.');
    expect(el.querySelector('.info-card')).toBeNull();
  }));

  it('n\'expose pas le détail technique d\'une erreur serveur', fakeAsync(() => {
    pdca.getCycle.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 500, error: { detail: 'org.hibernate.LazyInitializationException' }
    })));
    setup();
    tick();
    fixture.detectChanges();

    const banner = (fixture.nativeElement as HTMLElement).querySelector('.state-row.error');
    expect(banner?.textContent).toContain('Erreur serveur');
    expect(banner?.textContent).not.toContain('Hibernate');
  }));

  // --- avancement de phase -----------------------------------------------------

  it('avance la phase, confirme et recharge la fiche', () => {
    setup();
    pdca.getCycle.calls.reset();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pdca.advanceCycle.and.returnValue(of(buildCycle({ status: 'CHECK' })));

    component.advance('DO');

    expect(pdca.advanceCycle).toHaveBeenCalledWith(ID);
    expect(snackSpy).toHaveBeenCalled();
    expect(pdca.getCycle).toHaveBeenCalledTimes(1);   // rechargement après transition
    expect(component.acting$.value).toBeFalse();
  });

  it('n\'avance pas un cycle déjà terminé (COMPLETED / CANCELLED)', () => {
    setup();
    component.advance('COMPLETED');
    component.advance('CANCELLED');
    expect(pdca.advanceCycle).not.toHaveBeenCalled();
  });

  it('ignore un second avancement tant que le premier est en vol', () => {
    setup();
    pdca.advanceCycle.and.returnValue(new Subject<PdcaCycleResponse>());

    component.advance('DO');
    component.advance('DO');

    expect(pdca.advanceCycle).toHaveBeenCalledTimes(1);
    expect(component.acting$.value).toBeTrue();
  });

  it('signale le refus serveur sans recharger la fiche', () => {
    setup();
    pdca.getCycle.calls.reset();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pdca.advanceCycle.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.advance('DO');

    expect(snackSpy).toHaveBeenCalledWith(
      'État incompatible — rechargez la page.', 'OK', { duration: 4000 });
    expect(pdca.getCycle).not.toHaveBeenCalled();
    expect(component.acting$.value).toBeFalse();
  });

  // --- annulation --------------------------------------------------------------

  it('annule le cycle et recharge la fiche', () => {
    setup();
    pdca.getCycle.calls.reset();
    pdca.cancelCycle.and.returnValue(of(buildCycle({ status: 'CANCELLED' })));

    component.cancel();

    expect(pdca.cancelCycle).toHaveBeenCalledWith(ID);
    expect(pdca.getCycle).toHaveBeenCalledTimes(1);
    expect(component.acting$.value).toBeFalse();
  });

  it('ignore une seconde annulation tant que la première est en vol', () => {
    setup();
    pdca.cancelCycle.and.returnValue(new Subject<PdcaCycleResponse>());
    component.cancel();
    component.cancel();
    expect(pdca.cancelCycle).toHaveBeenCalledTimes(1);
  });

  it('signale l\'échec d\'annulation avec un message sûr', () => {
    setup();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pdca.cancelCycle.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));

    component.cancel();

    expect(snackSpy).toHaveBeenCalledWith(
      'Vous n\'avez pas les droits pour cette action.', 'OK', { duration: 4000 });
    expect(component.acting$.value).toBeFalse();
  });

  // --- ajout d'étape -----------------------------------------------------------

  it('présélectionne la phase courante dans le dialogue d\'étape et recharge après ajout', () => {
    setup();
    pdca.getCycle.calls.reset();
    const dialog = TestBed.inject(MatDialog);
    const openSpy = spyOn(dialog, 'open').and.returnValue({ afterClosed: () => of(STEP) } as never);

    component.openAddStep('CHECK');

    expect(openSpy.calls.mostRecent().args[1]?.data).toEqual({ cycleId: ID, defaultPhase: 'CHECK' });
    expect(pdca.getCycle).toHaveBeenCalledTimes(1);
  });

  it('ne présélectionne aucune phase depuis un statut hors roue', () => {
    setup();
    pdca.getCycle.calls.reset();
    const dialog = TestBed.inject(MatDialog);
    const openSpy = spyOn(dialog, 'open').and.returnValue({ afterClosed: () => of(undefined) } as never);

    component.openAddStep('COMPLETED');

    expect(openSpy.calls.mostRecent().args[1]?.data)
      .toEqual({ cycleId: ID, defaultPhase: undefined });
    expect(pdca.getCycle).not.toHaveBeenCalled();   // dialogue fermé sans étape : pas de rechargement
  });

  // --- présentation ------------------------------------------------------------

  it('classe les états terminaux de la roue', () => {
    setup();
    const terminal: PdcaStatus[] = ['COMPLETED', 'CANCELLED'];
    const running: PdcaStatus[] = ['PLAN', 'DO', 'CHECK', 'ACT'];
    terminal.forEach(s => expect(component.isTerminal(s)).withContext(s).toBeTrue());
    running.forEach(s => expect(component.isTerminal(s)).withContext(s).toBeFalse());
  });

  it('dérive les classes de badge du statut, du statut d\'étape et de la phase', () => {
    setup();
    expect(component.statusBadge('COMPLETED')).toBe('badge badge-completed');
    expect(component.stepStatusBadge('IN_PROGRESS')).toBe('badge badge-in_progress');
    expect(component.phaseColor('ACT')).toBe('phase phase-act');
  });

  it('revient à la liste des cycles', () => {
    setup();
    component.goBack();
    expect(router.navigate).toHaveBeenCalledWith(['/pdca']);
  });

  // --- preuve d'étape (§3.1, ADR 0061) ------------------------------------------
  // Une étape déclarée faite sans document ne prouve rien : elle affirme. Ce qui
  // se teste ici, c'est que la colonne dise la vérité — pas de bouton là où rien
  // ne peut être versé, pas de pièce affichée sur la mauvaise ligne, et un refus
  // serveur traduit en une phrase qui dit quoi corriger.

  const EVIDENCE: PdcaStepEvidence = {
    id: 'evd-1', cycleId: ID, stepId: 's1', contentType: 'application/pdf',
    sizeBytes: 1024, originalFilename: 'releve-signe.pdf',
    createdAt: '2026-08-20T09:00:00Z', url: 'https://minio.local/releve.pdf?sig=x'
  };

  /** Un fichier factice : seul son passage par l'entrée compte, pas son contenu. */
  function pickFile(component: PdcaDetailComponent, file: File | null): void {
    const input = document.createElement('input');
    input.type = 'file';
    Object.defineProperty(input, 'files', { value: file ? [file] : [] });
    component.onStepEvidenceSelected({ target: input } as unknown as Event);
  }

  function pdf(): File {
    return new File(['%PDF-1.7'], 'releve-signe.pdf', { type: 'application/pdf' });
  }

  it('charge les preuves à l\'ouverture et les range par étape', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    pdca.listStepEvidences.and.returnValue(of([EVIDENCE]));
    setup();

    expect(pdca.listStepEvidences).toHaveBeenCalledWith(ID);
    expect(component.stepEvidence('s1')).toEqual(EVIDENCE);
    expect(component.stepEvidence('inconnue')).toBeUndefined();
  });

  it('affiche le document joint avec son lien de téléchargement', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    pdca.listStepEvidences.and.returnValue(of([EVIDENCE]));
    setup();

    const link = (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLAnchorElement>('.steps-table a.step-evidence');
    expect(link?.getAttribute('href')).toBe('https://minio.local/releve.pdf?sig=x');
    expect(link?.textContent).toContain('releve-signe.pdf');
    // Ouverture en nouvel onglet sans laisser la page fille manipuler l'ouvrante.
    expect(link?.getAttribute('rel')).toContain('noopener');
  });

  it('affiche la pièce sans lien plutôt qu\'un lien mort quand l\'URL manque', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    pdca.listStepEvidences.and.returnValue(of([{ ...EVIDENCE, url: undefined }]));
    setup();

    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.steps-table a.step-evidence')).toBeNull();
    expect(el.querySelector('.steps-table span.step-evidence')?.textContent)
      .toContain('releve-signe.pdf');
  });

  it('propose « Joindre » sur une étape sans preuve d\'un cycle vivant', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    setup();

    const btn = (fixture.nativeElement as HTMLElement)
      .querySelector<HTMLButtonElement>('.steps-table .attach-btn');
    expect(btn).toBeTruthy();
    expect(btn?.getAttribute('aria-label'))
      .toBe('Joindre une preuve à l\'étape : Analyse Pareto');
  });

  it('ne propose rien à joindre sur un cycle clos ou annulé', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ status: 'COMPLETED', steps: [STEP] })));
    setup();

    expect(component.canAttachStepEvidence('s1', 'COMPLETED')).toBeFalse();
    expect(component.canAttachStepEvidence('s1', 'CANCELLED')).toBeFalse();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.steps-table .attach-btn')).toBeNull();
    expect(el.querySelector('.steps-table .muted')?.textContent).toContain('—');
  });

  it('n\'offre pas de second dépôt sur une étape qui porte déjà sa preuve', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    pdca.listStepEvidences.and.returnValue(of([EVIDENCE]));
    setup();

    expect(component.canAttachStepEvidence('s1', 'DO')).toBeFalse();
  });

  it('annonce le stockage coupé et retire le bouton plutôt que de le laisser inerte', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    pdca.listStepEvidences.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 503, error: { type: 'https://qualitos.io/errors/storage-disabled' }
    })));
    setup();

    expect(component.evidenceStorageDisabled$.value).toBeTrue();
    expect(component.canAttachStepEvidence('s1', 'DO')).toBeFalse();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.storage-off')).toBeTruthy();
    expect(el.querySelector('.steps-table .attach-btn')).toBeNull();
  });

  it('n\'invente pas une coupure de stockage sur une autre panne', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    pdca.listStepEvidences.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    setup();

    expect(component.evidenceStorageDisabled$.value).toBeFalse();
  });

  it('mémorise l\'étape visée au clic et remet le champ à zéro', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    setup();
    const input = document.createElement('input');
    input.type = 'file';
    const clickSpy = spyOn(input, 'click');

    component.triggerStepEvidencePicker(input, 's1');

    expect(clickSpy).toHaveBeenCalled();
    expect(input.value).toBe('');
  });

  it('ignore un second clic tant qu\'un échange est en vol sur une ligne', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    setup();
    component.busyEvidenceStepId$.next('s1');
    const input = document.createElement('input');
    const clickSpy = spyOn(input, 'click');

    component.triggerStepEvidencePicker(input, 's1');

    expect(clickSpy).not.toHaveBeenCalled();
  });

  it('verse la pièce et l\'affiche sur la ligne de l\'étape', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    setup();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pdca.uploadStepEvidence.and.returnValue(of(EVIDENCE));

    const input = document.createElement('input');
    spyOn(input, 'click');
    component.triggerStepEvidencePicker(input, 's1');
    pickFile(component, pdf());

    expect(pdca.uploadStepEvidence).toHaveBeenCalledWith(ID, 's1', jasmine.any(File));
    expect(component.stepEvidence('s1')).toEqual(EVIDENCE);
    expect(component.busyEvidenceStepId$.value).toBeNull();
    expect(snackSpy).toHaveBeenCalled();
  });

  it('ne verse rien quand aucun fichier n\'a été choisi', () => {
    setup();
    const input = document.createElement('input');
    spyOn(input, 'click');
    component.triggerStepEvidencePicker(input, 's1');
    pickFile(component, null);

    expect(pdca.uploadStepEvidence).not.toHaveBeenCalled();
  });

  it('ne verse rien quand aucune étape n\'a été visée', () => {
    setup();
    pickFile(component, pdf());
    expect(pdca.uploadStepEvidence).not.toHaveBeenCalled();
  });

  it('traduit chaque refus de dépôt en une phrase qui dit quoi corriger', () => {
    setup();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    const attendus: Array<[number, string]> = [
      [413, 'Pièce trop lourde — 10 Mo au maximum.'],
      [400, 'Format refusé — PDF, image, Word ou Excel, et le contenu doit correspondre au format annoncé.'],
      [409, 'Cette étape porte déjà sa preuve, ou le cycle est clos.'],
      [404, 'Cette étape n\'existe plus — recharge la fiche.']
    ];

    for (const [status, message] of attendus) {
      snackSpy.calls.reset();
      pdca.uploadStepEvidence.and.returnValue(throwError(() => new HttpErrorResponse({ status })));
      const input = document.createElement('input');
      spyOn(input, 'click');
      component.triggerStepEvidencePicker(input, 's1');
      pickFile(component, pdf());

      expect(snackSpy).toHaveBeenCalledWith(message, 'OK', { duration: 5000 });
      expect(component.busyEvidenceStepId$.value).toBeNull();
    }
  });

  it('reste sur un message sûr quand la panne n\'a pas de raison connue', () => {
    setup();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pdca.uploadStepEvidence.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 500, error: { detail: 'org.hibernate.LazyInitializationException' }
    })));

    const input = document.createElement('input');
    spyOn(input, 'click');
    component.triggerStepEvidencePicker(input, 's1');
    pickFile(component, pdf());

    expect(snackSpy).toHaveBeenCalled();
    // OWASP A09 — le détail technique ne franchit jamais l'écran.
    expect(snackSpy.calls.mostRecent().args[0] as string).not.toContain('Hibernate');
  });

  it('bascule en « stockage coupé » plutôt que d\'afficher un refus sur un 503', () => {
    setup();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    pdca.uploadStepEvidence.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 503, error: { type: 'https://qualitos.io/errors/storage-disabled' }
    })));

    const input = document.createElement('input');
    spyOn(input, 'click');
    component.triggerStepEvidencePicker(input, 's1');
    pickFile(component, pdf());

    expect(component.evidenceStorageDisabled$.value).toBeTrue();
    expect(snackSpy).not.toHaveBeenCalled();
  });

  it('retire la preuve après confirmation et vide la cellule', () => {
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    pdca.listStepEvidences.and.returnValue(of([EVIDENCE]));
    setup();
    spyOn(TestBed.inject(MatDialog), 'open')
      .and.returnValue({ afterClosed: () => of(true) } as never);
    pdca.deleteStepEvidence.and.returnValue(of(void 0));

    component.removeStepEvidence('s1', EVIDENCE);

    expect(pdca.deleteStepEvidence).toHaveBeenCalledWith(ID, 's1', 'evd-1');
    expect(component.stepEvidence('s1')).toBeUndefined();
    expect(component.busyEvidenceStepId$.value).toBeNull();
  });

  it('conserve la preuve quand le retrait n\'est pas confirmé', () => {
    pdca.listStepEvidences.and.returnValue(of([EVIDENCE]));
    setup();
    spyOn(TestBed.inject(MatDialog), 'open')
      .and.returnValue({ afterClosed: () => of(false) } as never);

    component.removeStepEvidence('s1', EVIDENCE);

    expect(pdca.deleteStepEvidence).not.toHaveBeenCalled();
    expect(component.stepEvidence('s1')).toEqual(EVIDENCE);
  });

  it('ignore un retrait tant qu\'un échange est en vol sur la ligne', () => {
    setup();
    const openSpy = spyOn(TestBed.inject(MatDialog), 'open');
    component.busyEvidenceStepId$.next('s1');

    component.removeStepEvidence('s1', EVIDENCE);

    expect(openSpy).not.toHaveBeenCalled();
  });

  it('signale un retrait refusé avec un message sûr et garde la pièce affichée', () => {
    pdca.listStepEvidences.and.returnValue(of([EVIDENCE]));
    setup();
    const snackSpy = spyOn(TestBed.inject(MatSnackBar), 'open');
    spyOn(TestBed.inject(MatDialog), 'open')
      .and.returnValue({ afterClosed: () => of(true) } as never);
    pdca.deleteStepEvidence.and.returnValue(throwError(() => new HttpErrorResponse({
      status: 500, error: { detail: 'org.hibernate.LazyInitializationException' }
    })));

    component.removeStepEvidence('s1', EVIDENCE);

    expect(snackSpy).toHaveBeenCalled();
    expect(snackSpy.calls.mostRecent().args[0] as string).not.toContain('Hibernate');
    expect(component.stepEvidence('s1')).toEqual(EVIDENCE);
    expect(component.busyEvidenceStepId$.value).toBeNull();
  });

  it('choisit une icône par famille de document', () => {
    setup();
    expect(component.evidenceIcon('application/pdf')).toBe('picture_as_pdf');
    expect(component.evidenceIcon('image/png')).toBe('image');
    expect(component.evidenceIcon(
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet')).toBe('table_chart');
    expect(component.evidenceIcon(
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document')).toBe('description');
    expect(component.evidenceIcon('application/octet-stream')).toBe('attach_file');
  });

  it('nomme ses boutons d\'action pour les lecteurs d\'écran', () => {
    setup();
    expect(component.attachEvidenceAria('Analyse Pareto'))
      .toBe('Joindre une preuve à l\'étape : Analyse Pareto');
    expect(component.removeEvidenceAria('Analyse Pareto'))
      .toBe('Retirer la preuve de l\'étape : Analyse Pareto');
  });

  it('place la colonne « Preuve » juste après l\'échéance', () => {
    // « Pour quand » puis « et voici que c'est fait » : rejeter la pièce en fin
    // de ligne l'aurait éloignée de la date qu'elle justifie.
    pdca.getCycle.and.returnValue(of(buildCycle({ steps: [STEP] })));
    setup();

    expect(component.stepColumns)
      .toEqual(['phase', 'title', 'status', 'dueDate', 'evidence', 'updatedAt']);
    const entetes = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('.steps-table th'))
      .map(th => th.textContent?.trim());
    expect(entetes[3]).toBe('Échéance');
    expect(entetes[4]).toBe('Preuve');
  });
});
