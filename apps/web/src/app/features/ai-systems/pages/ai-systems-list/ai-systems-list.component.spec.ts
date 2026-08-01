import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AiSystemsService } from '../../ai-systems.service';
import { AiSystemFilter, AiSystemRegistry, AiSystemView } from '../../ai-systems.types';
import { AiSystemFormDialogComponent } from '../ai-system-form-dialog/ai-system-form-dialog.component';
import { AiSystemsListComponent } from './ai-systems-list.component';

describe('AiSystemsListComponent', () => {
  let fixture: ComponentFixture<AiSystemsListComponent>;
  let component: AiSystemsListComponent;
  let svc: jasmine.SpyObj<AiSystemsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let router: Router;

  const view = (over: Partial<AiSystemView> = {}): AiSystemView => ({
    id: 'id-1', tenantId: 't-1', reference: 'AISYS-TRIAGE', name: 'Triage urgences',
    description: null, providerName: 'MedAI', intendedPurpose: 'Priorisation des passages',
    riskClassification: 'HIGH', role: 'DEPLOYER', generalPurpose: false, status: 'DRAFT',
    conformityAssessmentEvidenceUrl: null, ceMarkingNumber: null,
    humanOversightDescription: null, transparencyMeasures: null, dataGovernanceNotes: null,
    linkedDpiaId: null, linkedProcessingActivityIds: [], linkedAutomatedDecisionIds: [],
    effectiveFrom: null, effectiveTo: null, withdrawalReason: null,
    createdByUserId: 'u-1', createdAt: '2026-07-01T09:00:00Z', updatedAt: '2026-07-02T09:00:00Z',
    prohibited: false, requiresConformityAssessment: true, requiresTransparency: true,
    ...over
  });

  const defaults: AiSystemView[] = [
    view({ id: 'a', reference: 'AISYS-BANNI', name: 'Notation sociale', riskClassification: 'UNACCEPTABLE', prohibited: true }),
    view({ id: 'b', reference: 'AISYS-TRIAGE' }),
    view({ id: 'c', reference: 'AISYS-CHAT', name: 'Assistant RH', riskClassification: 'LIMITED', status: 'IN_USE', providerName: null, transparencyMeasures: 'Bandeau' }),
    view({ id: 'd', reference: 'AISYS-OCR', name: 'Lecture de factures', riskClassification: 'MINIMAL_OR_NO', status: 'REGISTERED' })
  ];

  function registryOf(rows: AiSystemView[], all: AiSystemView[] = rows): AiSystemRegistry {
    return { all, rows };
  }

  function dialogCloses(value: unknown): void {
    dialog.open.and.returnValue({
      afterClosed: () => of(value) as Observable<unknown>
    } as unknown as MatDialogRef<unknown>);
  }

  /**
   * `deferredView` livre chargement et erreur via `asyncScheduler` : il faut céder
   * la main une fois avant de lire le DOM qui en dépend.
   */
  async function settle(): Promise<void> {
    await new Promise<void>(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  function lastFilter(): AiSystemFilter {
    return svc.registry.calls.mostRecent().args[0];
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<AiSystemsService>('AiSystemsService', ['registry']);
    svc.registry.and.returnValue(of(registryOf(defaults)));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialogCloses(undefined);

    await TestBed.configureTestingModule({
      declarations: [AiSystemsListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: AiSystemsService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AiSystemsListComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  });

  // ---- Chargement ------------------------------------------------------------

  it('charge le registre entier au premier affichage', () => {
    fixture.detectChanges();
    expect(svc.registry).toHaveBeenCalledOnceWith({ status: null, risk: null });
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(4);
  });

  it('affiche la classification en clair, jamais la constante serveur', () => {
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Haut risque');
    expect(text).toContain('Risque inacceptable');
    expect(text).not.toContain('MINIMAL_OR_NO');
  });

  it('propose un lien vers la fiche de chaque système', () => {
    fixture.detectChanges();
    const links = (fixture.nativeElement as HTMLElement).querySelectorAll('td a[href]');
    expect(links.length).toBe(4);
    expect(links[0].getAttribute('href')).toBe('/ai-systems/a');
  });

  it('signale les fiches qui exigent une décision', () => {
    fixture.detectChanges();
    // Le système interdit, plus celui enregistré à haut risque sans obligation documentée.
    expect(component.needsAttention(defaults[0])).toBeTrue();
    expect(component.needsAttention(view({ status: 'REGISTERED', riskClassification: 'HIGH' }))).toBeTrue();
    expect(component.needsAttention(defaults[3])).toBeFalse();
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('.attention').length).toBe(1);
  });

  it('affiche un message sûr et vide le tableau quand le chargement échoue', async () => {
    svc.registry.and.returnValue(throwError(() => new HttpErrorResponse({ status: 403 })));
    fixture.detectChanges();
    await settle();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Vous n\'avez pas les droits pour cette action.');
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length).toBe(0);
  });

  // ---- Compteurs -------------------------------------------------------------

  it('compte sur le registre entier, pas sur le résultat filtré', (done) => {
    svc.registry.and.returnValue(of(registryOf([defaults[0]], defaults)));
    fixture.detectChanges();

    component.tiles$.subscribe(tiles => {
      expect(tiles.map(t => t.value)).toEqual([4, 1, 1, 2, 1]);
      done();
    });
  });

  it('un compteur cliqué devient un filtre serveur', () => {
    fixture.detectChanges();

    component.applyTile('high');
    expect(lastFilter()).toEqual({ status: null, risk: 'HIGH' });

    component.applyTile('in-use');
    expect(lastFilter()).toEqual({ status: 'IN_USE', risk: null });

    component.applyTile('prohibited');
    expect(lastFilter()).toEqual({ status: null, risk: 'UNACCEPTABLE' });

    component.applyTile('draft');
    expect(lastFilter()).toEqual({ status: 'DRAFT', risk: null });

    component.applyTile('all');
    expect(lastFilter()).toEqual({ status: null, risk: null });
  });

  it('marque le compteur actif pour que le filtre en cours se voie', (done) => {
    fixture.detectChanges();
    component.applyTile('high');

    component.tiles$.subscribe(tiles => {
      expect(tiles.find(t => t.key === 'high')?.active).toBeTrue();
      expect(tiles.find(t => t.key === 'all')?.active).toBeFalse();
      done();
    });
  });

  // ---- Filtres ---------------------------------------------------------------

  it('recharge le registre à chaque changement de filtre', () => {
    fixture.detectChanges();
    const before = svc.registry.calls.count();

    component.statusFilter.setValue('WITHDRAWN');
    component.riskFilter.setValue('LIMITED');

    expect(svc.registry.calls.count()).toBe(before + 2);
    expect(lastFilter()).toEqual({ status: 'WITHDRAWN', risk: 'LIMITED' });
  });

  it('filtre le texte localement, sans repartir au serveur', (done) => {
    fixture.detectChanges();
    const before = svc.registry.calls.count();

    component.search.setValue('  facture ');

    expect(svc.registry.calls.count()).toBe(before);
    component.rows$.subscribe(rows => {
      expect(rows.map(r => r.id)).toEqual(['d']);
      done();
    });
  });

  it('cherche aussi sur la référence et le fournisseur', (done) => {
    fixture.detectChanges();
    component.search.setValue('medai');
    component.rows$.subscribe(rows => {
      expect(rows.map(r => r.reference)).toEqual(['AISYS-BANNI', 'AISYS-TRIAGE', 'AISYS-OCR']);
      done();
    });
  });

  it('n\'annonce un filtre actif que s\'il y en a un', () => {
    fixture.detectChanges();
    expect(component.hasFilters()).toBeFalse();

    component.search.setValue('   ');
    expect(component.hasFilters()).toBeFalse();

    component.search.setValue('chat');
    expect(component.hasFilters()).toBeTrue();

    component.resetFilters();
    expect(component.hasFilters()).toBeFalse();
    expect(lastFilter()).toEqual({ status: null, risk: null });
  });

  it('rafraîchit à la demande sans changer les filtres', () => {
    fixture.detectChanges();
    component.statusFilter.setValue('DRAFT');
    const before = svc.registry.calls.count();

    component.refresh();

    expect(svc.registry.calls.count()).toBe(before + 1);
    expect(lastFilter()).toEqual({ status: 'DRAFT', risk: null });
  });

  // ---- États vides -----------------------------------------------------------

  it('distingue un registre vide d\'un filtre trop étroit', async () => {
    svc.registry.and.returnValue(of(registryOf([])));
    fixture.detectChanges();
    await settle();
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('Aucun système d\'IA n\'est encore déclaré');

    svc.registry.and.returnValue(of(registryOf([], defaults)));
    component.refresh();
    await settle();
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('Aucun système ne correspond à ces critères.');
  });

  // ---- Création --------------------------------------------------------------

  it('ouvre la déclaration puis va sur la fiche créée', () => {
    const created = view({ id: 'neuf' });
    dialogCloses(created);
    const navigate = spyOn(router, 'navigate');
    fixture.detectChanges();

    component.create();

    expect(dialog.open.calls.mostRecent().args[0]).toBe(AiSystemFormDialogComponent);
    expect(navigate).toHaveBeenCalledWith(['/ai-systems', 'neuf']);
  });

  it('ne navigue pas quand la déclaration est annulée', () => {
    dialogCloses(undefined);
    const navigate = spyOn(router, 'navigate');
    fixture.detectChanges();

    component.create();

    expect(navigate).not.toHaveBeenCalled();
  });

  it('identifie les lignes par leur identifiant pour éviter de recréer le DOM', () => {
    expect(component.trackById(0, defaults[0])).toBe('a');
  });
});
