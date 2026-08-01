import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';

import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AiSystemsService } from '../../ai-systems.service';
import { AiSystemView } from '../../ai-systems.types';
import { AiSystemFormDialogComponent } from '../ai-system-form-dialog/ai-system-form-dialog.component';
import { AiSystemWithdrawDialogComponent } from '../ai-system-withdraw-dialog/ai-system-withdraw-dialog.component';
import { AiSystemDetailComponent } from './ai-system-detail.component';

describe('AiSystemDetailComponent', () => {
  let fixture: ComponentFixture<AiSystemDetailComponent>;
  let component: AiSystemDetailComponent;
  let svc: jasmine.SpyObj<AiSystemsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let router: Router;

  const params$ = new BehaviorSubject(convertToParamMap({ id: 'id-1' }));

  const view = (over: Partial<AiSystemView> = {}): AiSystemView => ({
    id: 'id-1', tenantId: 't-1', reference: 'AISYS-TRIAGE', name: 'Triage urgences',
    description: 'Modèle interne', providerName: 'MedAI',
    intendedPurpose: 'Priorisation des passages aux urgences',
    riskClassification: 'MINIMAL_OR_NO', role: 'DEPLOYER', generalPurpose: false,
    status: 'DRAFT',
    conformityAssessmentEvidenceUrl: null, ceMarkingNumber: null,
    humanOversightDescription: null, transparencyMeasures: null, dataGovernanceNotes: null,
    linkedDpiaId: null, linkedProcessingActivityIds: [], linkedAutomatedDecisionIds: [],
    effectiveFrom: null, effectiveTo: null, withdrawalReason: null,
    createdByUserId: 'u-1', createdAt: '2026-07-01T09:00:00Z', updatedAt: '2026-07-02T09:00:00Z',
    prohibited: false, requiresConformityAssessment: false, requiresTransparency: false,
    ...over
  });

  const compliantHigh = (over: Partial<AiSystemView> = {}): AiSystemView => view({
    riskClassification: 'HIGH',
    conformityAssessmentEvidenceUrl: 'https://exemple.tld/dossier',
    humanOversightDescription: 'Interruption possible par le médecin référent',
    transparencyMeasures: 'Information affichée au patient',
    requiresConformityAssessment: true, requiresTransparency: true,
    ...over
  });

  function dialogCloses(value: unknown): void {
    dialog.open.and.returnValue({
      afterClosed: () => of(value) as Observable<unknown>
    } as unknown as MatDialogRef<unknown>);
  }

  function build(system: AiSystemView | null, resolve?: Observable<AiSystemView | null>): void {
    svc.resolve.and.returnValue(resolve ?? of(system));
    fixture = TestBed.createComponent(AiSystemDetailComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }

  async function settle(): Promise<void> {
    await new Promise<void>(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  /**
   * `textContent` d'un bouton Material contient aussi la ligature de l'icône
   * (« editModifier ») : on lit le dernier <span> non vide, qui porte le libellé.
   */
  function toolbarLabels(): string[] {
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.toolbar button'))
      .map(button => Array.from(button.querySelectorAll('span'))
        .map(span => (span.textContent ?? '').trim())
        .filter(text => text.length > 0)
        .pop() ?? '');
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<AiSystemsService>('AiSystemsService', [
      'resolve', 'register', 'putInUse', 'decommission', 'delete'
    ]);
    svc.register.and.returnValue(of(view({ status: 'REGISTERED' })));
    svc.putInUse.and.returnValue(of(view({ status: 'IN_USE' })));
    svc.decommission.and.returnValue(of(view({ status: 'DECOMMISSIONED' })));
    svc.delete.and.returnValue(of(undefined));

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialogCloses(undefined);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    params$.next(convertToParamMap({ id: 'id-1' }));

    await TestBed.configureTestingModule({
      declarations: [AiSystemDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: AiSystemsService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        provideRouter([]),
        // Après `provideRouter` : le routeur fournit lui aussi `ActivatedRoute`,
        // et c'est le dernier fournisseur déclaré qui gagne.
        { provide: ActivatedRoute, useValue: { paramMap: params$.asObservable() } }
      ]
    }).compileComponents();
  });

  // ---- Chargement ------------------------------------------------------------

  it('résout la fiche depuis le paramètre de route et l\'affiche', () => {
    build(view());
    expect(svc.resolve).toHaveBeenCalledWith('id-1');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Triage urgences');
    expect(text).toContain('AISYS-TRIAGE');
    expect(text).toContain('Priorisation des passages aux urgences');
  });

  it('accepte une référence lisible comme paramètre de route', () => {
    params$.next(convertToParamMap({ id: 'AISYS-TRIAGE' }));
    build(view());
    expect(svc.resolve).toHaveBeenCalledWith('AISYS-TRIAGE');
  });

  it('annonce un identifiant invalide sans interroger le serveur', async () => {
    build(null);
    await settle();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Identifiant invalide.');
  });

  it('affiche un message sûr quand la fiche est introuvable', async () => {
    build(null, throwError(() => new HttpErrorResponse({ status: 404 })));
    await settle();
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('Ce système d\'IA est introuvable.');
  });

  // ---- Actions proposées selon l'état ----------------------------------------

  it('propose modification, enregistrement, abandon et suppression sur un brouillon', () => {
    build(view());
    const labels = toolbarLabels();
    expect(labels).toContain('Modifier');
    expect(labels).toContain('Enregistrer');
    expect(labels).toContain('Abandonner');
    expect(labels).toContain('Supprimer');
    expect(labels).not.toContain('Mettre en service');
  });

  it('ne propose la mise en service qu\'à un système enregistré et conforme', () => {
    build(compliantHigh({ status: 'REGISTERED' }));
    const labels = toolbarLabels();
    expect(labels).toContain('Mettre en service');
    expect(labels).not.toContain('Modifier');
    expect(labels).not.toContain('Supprimer');
  });

  it('retire la mise en service et explique l\'impasse quand une obligation manque', () => {
    build(view({ status: 'REGISTERED', riskClassification: 'HIGH', requiresConformityAssessment: true }));
    expect(toolbarLabels()).not.toContain('Mettre en service');
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Mise en service impossible');
    // La check-list nomme précisément ce qui manque.
    expect(text).toContain('Preuve d\'évaluation de conformité (Art. 43)');
  });

  it('n\'offre aucune mise sur le marché à une pratique interdite (Art. 5)', () => {
    build(view({ riskClassification: 'UNACCEPTABLE', prohibited: true }));
    const labels = toolbarLabels();
    expect(labels).not.toContain('Enregistrer');
    expect(labels).toContain('Abandonner');
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('Pratique interdite par l\'article 5');
  });

  it('ne propose que le retrait du service à un système en service', () => {
    build(compliantHigh({ status: 'IN_USE' }));
    expect(toolbarLabels()).toEqual(['Retirer du service']);
  });

  it('ne propose plus aucune transition sur un état terminal', () => {
    build(view({ status: 'DECOMMISSIONED' }));
    expect(toolbarLabels()).toEqual([]);
    build(view({ status: 'WITHDRAWN', withdrawalReason: 'Projet arrêté' }));
    expect(toolbarLabels()).toEqual([]);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Projet arrêté');
  });

  // ---- Transitions -----------------------------------------------------------

  it('enregistre après confirmation et recharge la fiche', () => {
    build(view());
    dialogCloses(true);
    const before = svc.resolve.calls.count();

    component.register(view());

    expect(svc.register).toHaveBeenCalledWith('id-1');
    expect(svc.resolve.calls.count()).toBe(before + 1);
    expect(snack.open).toHaveBeenCalled();
  });

  it('avertit que la fiche deviendra définitive quand des obligations manquent', () => {
    const incomplete = view({ riskClassification: 'HIGH', requiresConformityAssessment: true });
    build(incomplete);
    dialogCloses(true);

    component.register(incomplete);

    const data = dialog.open.calls.mostRecent().args[1] as
      { data: { message: string; destructive: boolean } };
    expect(data.data.message).toContain('ne pourra plus être mis en service');
    expect(data.data.destructive).toBeTrue();
  });

  it('n\'enregistre rien si la confirmation est refusée', () => {
    build(view());
    dialogCloses(false);

    component.register(view());

    expect(svc.register).not.toHaveBeenCalled();
  });

  it('met en service puis retire du service après confirmation', () => {
    build(compliantHigh({ status: 'REGISTERED' }));
    dialogCloses(true);

    component.putInUse(compliantHigh({ status: 'REGISTERED' }));
    expect(svc.putInUse).toHaveBeenCalledWith('id-1');

    component.decommission(compliantHigh({ status: 'IN_USE' }));
    expect(svc.decommission).toHaveBeenCalledWith('id-1');
  });

  it('refuse d\'appeler le serveur pour une transition que le domaine interdit', () => {
    build(view());
    dialogCloses(true);

    component.putInUse(view({ status: 'DRAFT' }));
    component.decommission(view({ status: 'REGISTERED' }));
    component.register(view({ status: 'IN_USE' }));

    expect(svc.putInUse).not.toHaveBeenCalled();
    expect(svc.decommission).not.toHaveBeenCalled();
    expect(svc.register).not.toHaveBeenCalled();
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('remonte un message sûr quand une transition est refusée par le serveur', () => {
    build(view());
    dialogCloses(true);
    svc.register.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));

    component.register(view());

    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('État incompatible — rechargez la page.');
    expect(component.pending).toBeFalse();
  });

  it('ignore une seconde action tant que la première est en vol', () => {
    build(view());
    dialogCloses(true);
    component.pending = true;

    component.register(view());
    component.edit(view());
    component.withdraw(view());
    component.remove(view());

    expect(dialog.open).not.toHaveBeenCalled();
    expect(svc.register).not.toHaveBeenCalled();
  });

  // ---- Édition, abandon, suppression ------------------------------------------

  it('ouvre le formulaire d\'édition sur le brouillon et recharge au retour', () => {
    build(view());
    dialogCloses(view({ name: 'Nouveau nom' }));
    const before = svc.resolve.calls.count();

    component.edit(view());

    expect(dialog.open.calls.mostRecent().args[0]).toBe(AiSystemFormDialogComponent);
    expect(svc.resolve.calls.count()).toBe(before + 1);
  });

  it('n\'ouvre pas l\'édition d\'une fiche déjà enregistrée', () => {
    build(view({ status: 'REGISTERED' }));

    component.edit(view({ status: 'REGISTERED' }));

    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('ouvre la saisie du motif d\'abandon et recharge au retour', () => {
    build(view());
    dialogCloses(view({ status: 'WITHDRAWN' }));
    const before = svc.resolve.calls.count();

    component.withdraw(view());

    expect(dialog.open.calls.mostRecent().args[0]).toBe(AiSystemWithdrawDialogComponent);
    expect(svc.resolve.calls.count()).toBe(before + 1);
  });

  it('ne recharge pas quand la saisie du motif est annulée', () => {
    build(view());
    dialogCloses(undefined);
    const before = svc.resolve.calls.count();

    component.withdraw(view());

    expect(svc.resolve.calls.count()).toBe(before);
  });

  it('supprime un brouillon après confirmation puis retourne au registre', () => {
    build(view());
    dialogCloses(true);
    const navigate = spyOn(router, 'navigate');

    component.remove(view());

    expect(dialog.open.calls.mostRecent().args[0]).toBe(ConfirmDialogComponent);
    expect(svc.delete).toHaveBeenCalledWith('id-1');
    expect(navigate).toHaveBeenCalledWith(['/ai-systems']);
  });

  it('ne supprime pas ce qui n\'est plus un brouillon', () => {
    build(view({ status: 'IN_USE' }));

    component.remove(view({ status: 'IN_USE' }));

    expect(svc.delete).not.toHaveBeenCalled();
  });

  it('remonte un message sûr quand la suppression échoue', () => {
    build(view());
    dialogCloses(true);
    svc.delete.and.returnValue(throwError(() => new HttpErrorResponse({ status: 500 })));
    const navigate = spyOn(router, 'navigate');

    component.remove(view());

    expect(navigate).not.toHaveBeenCalled();
    expect(snack.open.calls.mostRecent().args[0] as string)
      .toBe('Erreur serveur — réessayez dans un instant.');
  });

  // ---- Présentation ------------------------------------------------------------

  it('rend les rattachements RGPD cliquables vers leurs registres', () => {
    build(view({
      linkedDpiaId: '22222222-2222-4222-8222-222222222222',
      linkedProcessingActivityIds: ['33333333-3333-4333-8333-333333333333'],
      linkedAutomatedDecisionIds: ['44444444-4444-4444-8444-444444444444']
    }));
    const hrefs = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('a[href]'))
      .map(a => a.getAttribute('href'));
    expect(hrefs).toContain('/dpia/22222222-2222-4222-8222-222222222222');
    expect(hrefs).toContain('/ropa/33333333-3333-4333-8333-333333333333');
    expect(hrefs).toContain('/automated-decisions/44444444-4444-4444-8444-444444444444');
  });

  it('ouvre la preuve de conformité dans un onglet isolé', () => {
    build(compliantHigh({ status: 'REGISTERED' }));
    const link = (fixture.nativeElement as HTMLElement)
      .querySelector('a[href="https://exemple.tld/dossier"]');
    expect(link?.getAttribute('target')).toBe('_blank');
    expect(link?.getAttribute('rel')).toBe('noopener noreferrer');
  });

  it('affiche la check-list satisfaite d\'un système conforme', () => {
    build(compliantHigh({ status: 'REGISTERED' }));
    const items = (fixture.nativeElement as HTMLElement).querySelectorAll('.requirements li');
    expect(items.length).toBe(3);
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('.requirements li.ok').length).toBe(3);
  });

  it('dit qu\'aucune obligation ne s\'applique à un risque minimal', () => {
    build(view());
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('Aucune obligation spécifique');
  });

  it('identifie les lignes de check-list et de rattachement pour un rendu stable', () => {
    build(view());
    expect(component.trackByKey(0, { key: 'transparency', label: 'x', satisfied: false }))
      .toBe('transparency');
    expect(component.trackByValue(0, 'abc')).toBe('abc');
  });
});
