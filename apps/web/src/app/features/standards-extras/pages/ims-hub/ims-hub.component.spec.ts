import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StandardsExtrasService } from '../../standards-extras.service';
import {
  AnchorableEvent, AnchorBatchResult, AnchorVerification, CoverageOverview, MockAuditReport,
  StandardAdoption, StandardCatalogEntry
} from '../../standards-extras.types';
import { ImsHubComponent } from './ims-hub.component';

describe('ImsHubComponent', () => {
  let component: ImsHubComponent;
  let fixture: ComponentFixture<ImsHubComponent>;
  let svc: jasmine.SpyObj<StandardsExtrasService>;
  let navigate: jasmine.Spy;
  let user: AuthUser;

  const catalog: StandardCatalogEntry[] = [
    { id: '1', code: 'iso-9001', fullName: 'Management de la qualité', family: 'HLS' },
    { id: '2', code: 'iso-14001', fullName: 'Management environnemental', family: 'HLS' }
  ];

  const adoptions: StandardAdoption[] = [
    { id: 'a1', standardId: 's1', standardCode: 'iso-9001',
      standardName: 'Management de la qualité', status: 'IN_PROGRESS' }
  ];

  /** Deux clauses : l'une mutualisée sur iso-14001, l'autre spécifique. */
  const overview: CoverageOverview = {
    matrix: {
      tenantId: 't1', standardCodes: ['iso-9001', 'iso-14001'], cells: [],
      totalSourceClauses: 2, totalMappings: 2, reuseRatioPercent: 50
    },
    columns: ['iso-9001', 'iso-14001'],
    rows: [
      {
        sourceStandardCode: 'iso-9001', sourceClauseCode: '5.2', sharedCount: 1,
        cells: [
          { targetStandardCode: 'iso-9001', coverages: [], shared: false, self: true },
          { targetStandardCode: 'iso-14001', shared: true, self: false,
            coverages: [{ clauseCode: '5.2', relation: 'EQUIVALENT', confidence: 95 }] }
        ]
      },
      {
        sourceStandardCode: 'iso-9001', sourceClauseCode: '8.5', sharedCount: 0,
        cells: [
          { targetStandardCode: 'iso-9001', coverages: [], shared: false, self: true },
          { targetStandardCode: 'iso-14001', shared: false, self: false,
            coverages: [{ clauseCode: '8.1', relation: 'RELATED', confidence: 30 }] }
        ]
      }
    ],
    sharedClauseCount: 1
  };

  const report: MockAuditReport = {
    id: 'r1', adoptionId: 'a1', standardId: 's1', standardCode: 'iso-9001',
    standardName: 'Management de la qualité', readiness: 72, majorCount: 1, minorCount: 2,
    observationCount: 3, questionCount: 30, questions: [], gaps: [], remediationPlan: [],
    aiProvider: 'mistral', createdByUserId: 'u1', createdAt: '2026-07-01T10:00:00Z'
  };

  const events: AnchorableEvent[] = [
    { id: 'e1', sequenceNo: 42, occurredAt: '2026-07-01T09:00:00Z', action: 'CAPA_CREATED',
      resourceType: 'CAPA', integrityHash: 'a'.repeat(64), blockchainTxRef: null }
  ];

  const verified: AnchorVerification = {
    status: 'VERIFIED', detail: 'Intégrité confirmée.', txRef: 'tx-1', merkleRoot: 'root-1'
  };

  const emptyBatch: AnchorBatchResult = {
    tenantId: 't1', batchSize: 0, merkleRoot: null, blockchainTxRef: null, eventIds: [],
    firstSequenceNo: 0, lastSequenceNo: 0, anchoredAt: '2026-07-01T10:00:00Z'
  };

  const build = async (): Promise<void> => {
    await TestBed.configureTestingModule({
      declarations: [ImsHubComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: StandardsExtrasService, useValue: svc },
        { provide: AuthService, useValue: { snapshot: () => user } },
        provideRouter([])
      ]
    }).compileComponents();

    navigate = spyOn(TestBed.inject(Router), 'navigate').and.returnValue(Promise.resolve(true));
    fixture = TestBed.createComponent(ImsHubComponent);
    component = fixture.componentInstance;
  };

  beforeEach(() => {
    user = { userId: 'u-1', tenantId: 't-1', displayName: 'D', roles: ['quality_manager'] };
    svc = jasmine.createSpyObj<StandardsExtrasService>('StandardsExtrasService', [
      'selectableStandards', 'coverageOverview', 'adoptions', 'runMockAudit',
      'mockAuditHistory', 'anchorBatch', 'verifyAnchor', 'recentAuditEvents'
    ]);
    svc.selectableStandards.and.returnValue(of(catalog));
    svc.coverageOverview.and.returnValue(of(overview));
    svc.adoptions.and.returnValue(of(adoptions));
    svc.mockAuditHistory.and.returnValue(of([report]));
    svc.recentAuditEvents.and.returnValue(of(events));
    svc.anchorBatch.and.returnValue(of(emptyBatch));
    svc.verifyAnchor.and.returnValue(of(verified));
    svc.runMockAudit.and.returnValue(of(report));
  });

  // ---- Matrice de co-couverture ----------------------------------------------

  it('dessine une ligne par clause source et une cellule par norme comparée', async () => {
    await build();
    fixture.detectChanges();
    const rows = (fixture.nativeElement as HTMLElement).querySelectorAll('.matrix tbody tr');
    expect(rows.length).toBe(2);
    expect(rows[0].querySelectorAll('td').length).toBe(3); // 2 normes + colonne mutualisation
  });

  it('sans sélection, interroge le serveur sur les normes adoptées', async () => {
    await build();
    fixture.detectChanges();
    expect(svc.coverageOverview).toHaveBeenCalledWith([]);
  });

  it('met en avant les clauses mutualisées et sait n\'afficher qu\'elles', async () => {
    await build();
    fixture.detectChanges();
    expect(component.visibleRows.length).toBe(2);

    component.toggleSharedOnly(true);
    fixture.detectChanges();
    expect(component.visibleRows.map(r => r.sourceClauseCode)).toEqual(['5.2']);

    const highlighted = (fixture.nativeElement as HTMLElement)
      .querySelectorAll('.matrix tbody tr.row--shared');
    expect(highlighted.length).toBe(1);
  });

  it('ne relance pas la requête si la sélection de normes n\'a pas changé', async () => {
    await build();
    fixture.detectChanges();
    svc.coverageOverview.calls.reset();

    component.onStandardsClosed();
    expect(svc.coverageOverview).not.toHaveBeenCalled();

    component.codesControl.setValue(['iso-9001']);
    component.onStandardsClosed();
    expect(svc.coverageOverview).toHaveBeenCalledWith(['iso-9001']);
  });

  it('revient aux normes adoptées quand on efface la sélection', async () => {
    await build();
    fixture.detectChanges();
    component.codesControl.setValue(['iso-9001']);
    component.clearStandards();
    expect(component.codesControl.value).toEqual([]);
    expect(svc.coverageOverview).toHaveBeenCalledWith([]);
  });

  it('affiche un message d\'erreur exploitable si la matrice est refusée', async () => {
    svc.coverageOverview.and.returnValue(throwError(() => ({ status: 403 })));
    await build();
    fixture.detectChanges();
    const banner = (fixture.nativeElement as HTMLElement).querySelector('.banner-error');
    expect(banner?.textContent).toContain('droits');
    expect(component.coverage).toBeNull();
  });

  it('calcule les compteurs de tête à partir de la matrice, pas à chaque rendu', async () => {
    await build();
    fixture.detectChanges();
    expect(component.tiles.length).toBe(5);
    expect(component.tiles[2].value).toBe('1');   // clauses mutualisables
    expect(component.tiles[3].value).toBe('50.0 %');
  });

  // ---- Audit blanc IA ---------------------------------------------------------

  it('présélectionne la première norme adoptée et charge son historique', async () => {
    await build();
    fixture.detectChanges();
    expect(component.adoptionControl.value).toBe('a1');
    expect(svc.mockAuditHistory).toHaveBeenCalledWith('a1');
    expect(component.history.length).toBe(1);
  });

  it('ouvre le rapport produit dès la fin de l\'audit blanc IA', async () => {
    await build();
    fixture.detectChanges();
    component.runAudit();
    expect(svc.runMockAudit).toHaveBeenCalledWith('a1');
    expect(navigate).toHaveBeenCalledWith(
      ['audit-blanc-ia', 'a1', 'r1'], jasmine.objectContaining({ relativeTo: jasmine.anything() }));
  });

  it('explique l\'absence de clause exploitable plutôt qu\'un conflit générique', async () => {
    svc.runMockAudit.and.returnValue(throwError(() => ({ status: 409 })));
    await build();
    fixture.detectChanges();
    component.runAudit();
    let message = '';
    component.auditError$.subscribe(err => (message = err ?? ''));
    expect(message).toContain('clause exploitable');
  });

  it('masque le lancement d\'audit pour un rôle non habilité', async () => {
    user = { userId: 'u-1', tenantId: 't-1', displayName: 'D', roles: ['user'] };
    await build();
    fixture.detectChanges();
    expect(component.canRunAudit).toBeFalse();
  });

  // ---- Ancrage blockchain ------------------------------------------------------

  it('borne le lot d\'ancrage et signale qu\'il n\'y avait rien à ancrer', async () => {
    await build();
    fixture.detectChanges();
    component.batchControl.setValue(250);
    component.runAnchoring();
    expect(svc.anchorBatch).toHaveBeenCalledWith(250);
    expect(component.batchResult?.batchSize).toBe(0);
    // Le lot ayant pu modifier les txRef, la liste est rechargée.
    expect(svc.recentAuditEvents).toHaveBeenCalledTimes(2);
  });

  it('refuse d\'ancrer un lot hors des bornes acceptées par le serveur', async () => {
    await build();
    fixture.detectChanges();
    svc.anchorBatch.calls.reset();
    component.batchControl.setValue(5000);
    component.runAnchoring();
    expect(svc.anchorBatch).not.toHaveBeenCalled();
    expect(component.batchControl.touched).toBeTrue();
  });

  it('reprend le hash d\'un événement listé pour le vérifier', async () => {
    await build();
    fixture.detectChanges();
    component.verifyEvent(events[0]);
    expect(component.hashControl.value).toBe('a'.repeat(64));
    expect(svc.verifyAnchor).toHaveBeenCalledWith('a'.repeat(64));
    expect(component.verification?.status).toBe('VERIFIED');
  });

  it('ne vérifie pas un hash qui n\'a pas la forme d\'un SHA-256', async () => {
    await build();
    fixture.detectChanges();
    svc.verifyAnchor.calls.reset();
    component.hashControl.setValue('pas-un-hash');
    component.verifyHash();
    expect(svc.verifyAnchor).not.toHaveBeenCalled();
    expect(component.hashControl.touched).toBeTrue();
  });

  it('masque le déclenchement d\'ancrage pour un rôle non habilité', async () => {
    user = { userId: 'u-1', tenantId: 't-1', displayName: 'D', roles: ['auditor'] };
    await build();
    fixture.detectChanges();
    expect(component.canAnchor).toBeFalse();
    expect(component.canRunAudit).toBeTrue();
  });

  // ---- Résilience aux refus du serveur ----------------------------------------

  it('reste utilisable sur les normes adoptées quand le catalogue est refusé', async () => {
    // Le sélecteur n'est qu'un confort : son échec ne doit pas emporter la
    // matrice, qui retombe sur le comportement serveur par défaut.
    svc.selectableStandards.and.returnValue(throwError(() => ({ status: 403 })));
    await build();
    fixture.detectChanges();

    expect(component.standards).toEqual([]);
    expect(component.coverage).not.toBeNull();
  });

  it('signale l\'échec de chargement des normes adoptées', async () => {
    svc.adoptions.and.returnValue(throwError(() => ({ status: 500 })));
    await build();
    fixture.detectChanges();

    expect(component.adoptions).toEqual([]);
    expect(await firstError(component.auditError$)).toContain('Erreur serveur');
  });

  it('vide l\'historique et l\'explique quand son chargement échoue', async () => {
    await build();
    fixture.detectChanges();
    expect(component.history.length).toBe(1);

    svc.mockAuditHistory.and.returnValue(throwError(() => ({ status: 500 })));
    component.loadHistory();

    expect(component.history).toEqual([]);
    expect(await firstError(component.auditError$)).toBeTruthy();
  });

  it('n\'interroge pas l\'historique sans norme adoptée sélectionnée', async () => {
    svc.adoptions.and.returnValue(of([]));
    await build();
    fixture.detectChanges();
    svc.mockAuditHistory.calls.reset();

    component.loadHistory();

    expect(svc.mockAuditHistory).not.toHaveBeenCalled();
    expect(component.history).toEqual([]);
  });

  it('vide la liste des événements et l\'explique quand elle échoue', async () => {
    svc.recentAuditEvents.and.returnValue(throwError(() => ({ status: 500 })));
    await build();
    fixture.detectChanges();

    expect(component.events).toEqual([]);
    expect(await firstError(component.anchorError$)).toBeTruthy();
  });

  it('signale l\'échec du lot d\'ancrage sans effacer le résultat précédent', async () => {
    await build();
    fixture.detectChanges();
    svc.anchorBatch.and.returnValue(throwError(() => ({ status: 503 })));

    component.runAnchoring();

    expect(component.anchoring).toBeFalse();
    expect(await firstError(component.anchorError$)).toBeTruthy();
  });

  it('efface la vérification précédente quand une nouvelle échoue', async () => {
    await build();
    fixture.detectChanges();
    component.hashControl.setValue('a'.repeat(64));
    component.verifyHash();
    expect(component.verification).not.toBeNull();

    svc.verifyAnchor.and.returnValue(throwError(() => ({ status: 500 })));
    component.verifyHash();

    // Laisser à l'écran un « intégrité confirmée » périmé après un échec serait
    // une affirmation d'intégrité non vérifiée.
    expect(component.verification).toBeNull();
    expect(await firstError(component.anchorError$)).toBeTruthy();
  });

  it('n\'exécute pas deux audits en parallèle', async () => {
    await build();
    fixture.detectChanges();
    component.running = true;
    svc.runMockAudit.calls.reset();

    component.runAudit();

    expect(svc.runMockAudit).not.toHaveBeenCalled();
  });

  it('n\'exécute pas d\'audit sans norme adoptée sélectionnée', async () => {
    svc.adoptions.and.returnValue(of([]));
    await build();
    fixture.detectChanges();
    svc.runMockAudit.calls.reset();

    component.runAudit();

    expect(svc.runMockAudit).not.toHaveBeenCalled();
  });

  it('rend un message générique pour un échec d\'audit qui n\'est pas un conflit', async () => {
    await build();
    fixture.detectChanges();
    svc.runMockAudit.and.returnValue(throwError(() => ({ status: 500 })));

    component.runAudit();

    const message = await firstError(component.auditError$);
    expect(message).toBeTruthy();
    expect(message).not.toContain('aucune clause exploitable');
  });

  // ---- Libellés et tonalités ---------------------------------------------------

  it('nomme et colore chaque relation de couverture', async () => {
    await build();

    expect(component.relationLabel('EQUIVALENT')).toContain('Équivalente');
    expect(component.relationLabel('COVERS')).toContain('Couvre');
    expect(component.relationLabel('RELATED')).toContain('Liée');
    expect(component.relationLabel('REFERENCES')).toContain('Cite');

    // La tonalité hiérarchise : une équivalence vaut mieux qu'une simple
    // citation, et l'utilisateur doit le voir sans lire le libellé.
    expect(component.relationTone('EQUIVALENT')).toBe('success');
    expect(component.relationTone('COVERS')).toBe('accent');
    expect(component.relationTone('RELATED')).toBe('warn');
    expect(component.relationTone('REFERENCES')).toBe('neutral');
  });

  it('compose une infobulle qui donne la norme, la clause, la relation et la confiance', async () => {
    await build();

    const tooltip = component.coverageTooltip('iso-14001', '5.2', 'EQUIVALENT', 90);

    expect(tooltip).toContain('iso-14001');
    expect(tooltip).toContain('5.2');
    expect(tooltip).toContain('Équivalente');
    expect(tooltip).toContain('90');
  });

  it('nomme chaque criticité de constat', async () => {
    await build();

    expect(component.criticalityLabel('MAJOR')).toContain('majeure');
    expect(component.criticalityLabel('MINOR')).toContain('mineure');
    expect(component.criticalityLabel('OBSERVATION')).toContain('Observation');
  });

  it('gradue la tonalité du taux de préparation par paliers', async () => {
    await build();

    expect(component.readinessTone(80)).toBe('success');
    expect(component.readinessTone(79)).toBe('warn');
    expect(component.readinessTone(50)).toBe('warn');
    expect(component.readinessTone(49)).toBe('danger');
    expect(component.readinessLabel(72.4)).toBe('72 %');
  });

  it('nomme et colore chaque état d\'ancrage', async () => {
    await build();

    expect(component.anchorStatusLabel('VERIFIED')).toContain('confirmée');
    expect(component.anchorStatusLabel('TAMPERED')).toContain('Altération');
    expect(component.anchorStatusLabel('NOT_ANCHORED')).toContain('Pas encore ancré');

    // Une altération détectée doit sauter aux yeux : c'est une preuve corrompue.
    expect(component.anchorStatusTone('VERIFIED')).toBe('success');
    expect(component.anchorStatusTone('TAMPERED')).toBe('danger');
    expect(component.anchorStatusTone('NOT_ANCHORED')).toBe('neutral');
  });

  it('signale qu\'une seule norme comparée ne peut rien mutualiser', async () => {
    await build();
    fixture.detectChanges();
    expect(component.singleStandard).toBeFalse();

    svc.coverageOverview.and.returnValue(of({
      ...overview, columns: ['iso-9001']
    } as CoverageOverview));
    component.loadCoverage();

    expect(component.singleStandard).toBeTrue();
  });

  it('donne des clés de suivi stables aux listes rendues', async () => {
    await build();
    fixture.detectChanges();

    const row = component.visibleRows[0];
    expect(component.trackByRow(0, row)).toBe('iso-9001 5.2');
    expect(component.trackByCell(0, row.cells[0])).toBe('iso-9001');
    expect(component.trackByCode(0, 'iso-9001')).toBe('iso-9001');
    expect(component.trackByStandard(0, catalog[0])).toBe('1');
    expect(component.trackByReport(0, report)).toBe('r1');
    expect(component.trackByAdoption(0, adoptions[0])).toBe('a1');
    expect(component.trackByEvent(0, events[0])).toBe('e1');
    expect(component.trackByTile(0, component.tiles[0])).toBe(component.tiles[0].label);
  });

  it('expose l\'adoption sélectionnée, et rien quand la sélection ne correspond à aucune', async () => {
    await build();
    fixture.detectChanges();
    expect(component.selectedAdoption?.id).toBe('a1');

    component.adoptionControl.setValue('adoption-inconnue');
    expect(component.selectedAdoption).toBeNull();
  });

  it('libelle le compteur de mutualisation d\'une ligne', async () => {
    await build();
    fixture.detectChanges();

    expect(component.sharedLabel(component.visibleRows[0])).toContain('1');
  });
});

/**
 * Première valeur non nulle d'un flux d'erreur.
 *
 * On ne référence pas l'abonnement depuis son propre rappel : ces flux sont des
 * `BehaviorSubject`, qui émettent SYNCHRONEMENT à la souscription — la variable
 * ne serait pas encore initialisée.
 */
async function firstError(source: Observable<string | null>): Promise<string | null> {
  let captured: string | null = null;
  const sub = source.subscribe(value => {
    if (value !== null && captured === null) {
      captured = value;
    }
  });
  await new Promise<void>(resolve => setTimeout(resolve));
  sub.unsubscribe();
  return captured;
}
