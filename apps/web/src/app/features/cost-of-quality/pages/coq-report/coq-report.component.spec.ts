import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpRequest } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { environment } from '../../../../../environments/environment';
import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CoqBlock, CoqLabel, CoqLine, CoqReport } from '../../cost-of-quality.types';
import { CoqEntryDialogComponent, CoqEntryDialogResult } from '../coq-entry-dialog/coq-entry-dialog.component';
import { CoqReportComponent } from './coq-report.component';

/**
 * L'écran du coût de la qualité.
 *
 * <p>Ce qui se vérifie : l'affichage des totaux tels que le serveur les rend,
 * la navigation mois / année et ses paramètres, la saisie par la fenêtre (avec
 * création préalable d'un libellé tapé en texte libre), la correction du
 * montant dans la liste, la suppression confirmée, et la lecture seule pour
 * qui n'a pas de rôle de pilotage.
 */
describe('CoqReportComponent', () => {

  const endpoint = `${environment.apiBaseUrl}/api/v1/cost-of-quality`;

  let fixture: ComponentFixture<CoqReportComponent>;
  let component: CoqReportComponent;
  let http: HttpTestingController;
  let dialog: MatDialog;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const LIBELLES: CoqLabel[] = [
    { id: 'l-form', category: 'PREVENTION', code: 'PREVENTION_TRAINING', name: 'Formation', partControl: false, builtIn: true },
    { id: 'l-rebut', category: 'INTERNAL_FAILURE', code: 'INTERNAL_SCRAP', name: 'Rebuts', partControl: true, builtIn: true }
  ];

  function ligne(overrides: Partial<CoqLine> = {}): CoqLine {
    return {
      entryId: 'e1', labelId: 'l-rebut', labelCode: 'INTERNAL_SCRAP', labelName: 'Rebuts',
      partControl: true, amount: 1263, entryCount: 1, responsible: 'Mme Diallo',
      imputationDate: '2026-09-15', comment: null, partReference: 'P-4410', partQuantity: 12,
      lot: 'L-2609', receivedOrMadeOn: '2026-09-12', ...overrides
    };
  }

  function bloc(category: CoqBlock['category'], lines: CoqLine[], total: number): CoqBlock {
    return { category, lines, total };
  }

  function rapport(overrides: Partial<CoqReport> = {}): CoqReport {
    return {
      year: 2026, month: 9, currency: 'EUR',
      blocks: [
        bloc('PREVENTION', [ligne({
          entryId: null, labelId: 'l-form', labelCode: 'PREVENTION_TRAINING', labelName: 'Formation',
          partControl: false, amount: 0, entryCount: 0, responsible: null, imputationDate: null,
          partReference: null, partQuantity: null, lot: null, receivedOrMadeOn: null
        })], 0),
        bloc('APPRAISAL', [], 1717),
        bloc('INTERNAL_FAILURE', [ligne()], 1263),
        bloc('EXTERNAL_FAILURE', [], 1615)
      ],
      conformanceTotal: 1717, nonConformanceTotal: 2878, total: 4595, ratio: 0.6, months: [],
      ...overrides
    };
  }

  const estRapport = (annee: number, mois: number | null) => (req: HttpRequest<unknown>): boolean =>
    req.url === endpoint && req.method === 'GET'
    && req.params.get('year') === String(annee)
    && req.params.get('month') === (mois === null ? null : String(mois));

  function servir(r: CoqReport = rapport()): void {
    http.expectOne(`${endpoint}/labels`).flush(LIBELLES);
    http.expectOne(estRapport(component.year, component.month)).flush(r);
    fixture.detectChanges();
  }

  async function setup(roles: string[] = ['QUALITY_MANAGER']): Promise<void> {
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [CoqReportComponent],
      imports: [SharedModule, UiModule, MatButtonToggleModule, NoopAnimationsModule],
      providers: [
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: { hasAnyRole: (a: string[]) => a.some(r => roles.includes(r)) } }
      ]
    }).compileComponents();

    http = TestBed.inject(HttpTestingController);
    dialog = TestBed.inject(MatDialog);
    fixture = TestBed.createComponent(CoqReportComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function repondre<T>(saisie: T | undefined): jasmine.Spy {
    return spyOn(dialog, 'open').and.returnValue({ afterClosed: () => of(saisie) } as MatDialogRef<unknown>);
  }

  afterEach(() => http.verify());

  it('affiche le total, les deux moitiés et le ratio que rend le serveur', async () => {
    await setup();
    servir();

    expect(hote().querySelector('[data-test="total"]')!.textContent).toContain('4');
    expect(hote().querySelector('[data-test="total"]')!.textContent).toContain('595');
    expect(hote().querySelector('[data-test="ratio"]')!.textContent).toContain('0.60');
    expect(hote().querySelectorAll('.bloc').length).toBe(4);
    expect(component.share(1717)).toBeCloseTo(37.4, 1);
  });

  it('sans perte, dit que le ratio ne se calcule pas', async () => {
    await setup();
    servir(rapport({ ratio: null, nonConformanceTotal: 0 }));

    expect(hote().querySelector('[data-test="ratio"]')).toBeNull();
    expect(hote().textContent).toContain('ne se calcule pas');
  });

  it('traduit un libellé livré et montre le lot d’une ligne de pièces', async () => {
    await setup();
    servir();

    const lignes = Array.from(hote().querySelectorAll('[data-test="ligne"]'));
    expect(lignes[0].textContent).toContain('Formation qualité du personnel');
    expect(lignes[1].textContent).toContain('L-2609');
    expect(lignes[0].classList).toContain('ligne--vide');
  });

  it('le mois suivant et le précédent changent de mois, et d’année au bord', async () => {
    await setup();
    servir();
    component.month = 12;

    component.suivant();
    expect(component.month).toBe(1);
    http.expectOne(estRapport(component.year, 1)).flush(rapport());

    component.precedent();
    expect(component.month).toBe(12);
    http.expectOne(estRapport(component.year, 12)).flush(rapport());
  });

  it('la vue année demande l’année entière, sans mois, et montre l’histogramme', async () => {
    await setup();
    servir();

    component.setMode('year');
    const mois = Array.from({ length: 12 }, (_, i) => ({ month: i + 1, conformance: i * 10, nonConformance: 5 }));
    http.expectOne(estRapport(component.year, null)).flush(rapport({ month: null, months: mois }));
    fixture.detectChanges();

    expect(hote().querySelector('[data-test="histogramme"]')).not.toBeNull();
    expect(hote().querySelectorAll('.histogramme__colonne').length).toBe(12);
    expect(hote().querySelector('[data-test="ajouter"]')).toBeNull();
    expect(hote().querySelector('[data-test="montant-ligne"]')).toBeNull();
    expect(component.barHeight(115)).toBe(100);

    component.suivant();
    http.expectOne(estRapport(component.year, null)).flush(rapport({ month: null }));
    component.setMode('year');
    http.expectNone(() => true);
  });

  it('ajouter une ligne ouvre la fenêtre de la famille, puis enregistre et recharge', async () => {
    await setup();
    servir();
    const resultat: CoqEntryDialogResult = {
      labelId: 'l-form',
      entry: { amount: 180, responsible: 'M. Alaoui', imputationDate: '2026-09-03' }
    };
    const ouverture = repondre(resultat);

    component.ajouter(component.report!.blocks[0]);

    const data = ouverture.calls.mostRecent().args[1]!.data;
    expect(ouverture.calls.mostRecent().args[0]).toBe(CoqEntryDialogComponent);
    expect(data.category).toBe('PREVENTION');
    expect(data.labels.map((l: CoqLabel) => l.id)).toEqual(['l-form']);
    expect(data.readOnly).toBeFalse();
    // Ajouter une ligne : le curseur va au libellé, où la liste s'ouvre.
    expect(ouverture.calls.mostRecent().args[1]!.autoFocus).toBe('first-tabbable');

    const post = http.expectOne({ url: `${endpoint}/entries`, method: 'POST' });
    expect(post.request.body).toEqual({ ...resultat.entry, labelId: 'l-form' });
    post.flush(ligne());
    http.expectOne(estRapport(component.year, component.month)).flush(rapport());
  });

  it('un libellé tapé en texte libre est créé AVANT la ligne, puis la liste se recharge', async () => {
    await setup();
    servir();
    repondre<CoqEntryDialogResult>({
      newLabel: { name: 'Tri 100 %', partControl: true },
      entry: { amount: 80, responsible: 'X', imputationDate: '2026-09-04', partReference: 'P', partQuantity: 3,
        lot: 'L', receivedOrMadeOn: '2026-09-01' }
    });

    component.ajouter(component.report!.blocks[1]);

    const creation = http.expectOne({ url: `${endpoint}/labels`, method: 'POST' });
    expect(creation.request.body).toEqual({ category: 'APPRAISAL', name: 'Tri 100 %', partControl: true });
    creation.flush({ id: 'l-tri', category: 'APPRAISAL', code: null, name: 'Tri 100 %', partControl: true, builtIn: false });

    const post = http.expectOne({ url: `${endpoint}/entries`, method: 'POST' });
    expect(post.request.body.labelId).toBe('l-tri');
    post.flush(ligne());
    http.expectOne({ url: `${endpoint}/labels`, method: 'GET' }).flush(LIBELLES);
    http.expectOne(estRapport(component.year, component.month)).flush(rapport());
  });

  it('ouvrir une ligne existante la corrige par PUT', async () => {
    await setup();
    servir();
    repondre<CoqEntryDialogResult>({
      labelId: 'l-rebut',
      entry: { amount: 999, responsible: 'Mme Diallo', imputationDate: '2026-09-15' }
    });

    const b = component.report!.blocks[2];
    component.ouvrir(b, b.lines[0]);
    expect((dialog.open as jasmine.Spy).calls.mostRecent().args[1]!.autoFocus)
      .toBe('input[data-test="montant"]');

    http.expectOne({ url: `${endpoint}/entries/e1`, method: 'PUT' }).flush(ligne());
    http.expectOne(estRapport(component.year, component.month)).flush(rapport());
  });

  it('cliquer une ligne à zéro ouvre la saisie sur SON libellé', async () => {
    await setup();
    servir();
    const ouverture = repondre(undefined);

    const b = component.report!.blocks[0];
    component.ouvrir(b, b.lines[0]);

    const data = ouverture.calls.mostRecent().args[1]!.data;
    expect(data.presetLabelId).toBe('l-form');
    expect(data.line).toBeUndefined();
    // Le libellé est déjà connu : le curseur va au montant, la liste ne se déroule pas.
    expect(ouverture.calls.mostRecent().args[1]!.autoFocus).toBe('input[data-test="montant"]');
  });

  it('corriger le montant dans la liste renvoie la ligne entière avec le nouveau montant', async () => {
    await setup();
    servir();

    component.changerMontant(component.report!.blocks[2].lines[0], '1500.5');

    const put = http.expectOne({ url: `${endpoint}/entries/e1`, method: 'PUT' });
    expect(put.request.body).toEqual(jasmine.objectContaining({
      labelId: 'l-rebut', amount: 1500.5, responsible: 'Mme Diallo', lot: 'L-2609', partQuantity: 12
    }));
    put.flush(ligne());
    http.expectOne(estRapport(component.year, component.month)).flush(rapport());
  });

  it('un montant inchangé n’envoie rien ; un montant invalide recharge sans écrire', async () => {
    await setup();
    servir();
    const l = component.report!.blocks[2].lines[0];

    component.changerMontant(l, '1263');
    component.changerMontant(l, '-3');

    http.expectOne(estRapport(component.year, component.month)).flush(rapport());
    http.expectNone({ method: 'PUT' });
  });

  it('supprimer demande confirmation, puis supprime', async () => {
    await setup();
    servir();
    repondre(true);

    component.supprimer(component.report!.blocks[2].lines[0]);

    http.expectOne({ url: `${endpoint}/entries/e1`, method: 'DELETE' }).flush(null);
    http.expectOne(estRapport(component.year, component.month)).flush(rapport());
  });

  it('une suppression refusée n’envoie rien', async () => {
    await setup();
    servir();
    repondre(false);

    component.supprimer(component.report!.blocks[2].lines[0]);
    http.expectNone({ method: 'DELETE' });
  });

  it('changer de devise l’enregistre puis recharge', async () => {
    await setup();
    servir();

    component.changerDevise('USD');
    http.expectOne({ url: `${endpoint}/currency`, method: 'PUT' }).flush({ currency: 'USD' });
    http.expectOne(estRapport(component.year, component.month)).flush(rapport({ currency: 'USD' }));

    component.changerDevise('USD');
    http.expectNone({ method: 'PUT' });
  });

  it('sans rôle de pilotage : ni ajout, ni saisie dans la liste, ni suppression ; la fenêtre s’ouvre en lecture', async () => {
    await setup(['USER']);
    servir();

    expect(hote().querySelector('[data-test="ajouter"]')).toBeNull();
    expect(hote().querySelector('[data-test="montant-ligne"]')).toBeNull();
    expect(hote().querySelector('[data-test="supprimer"]')).toBeNull();

    const ouverture = repondre(undefined);
    const b = component.report!.blocks[2];
    component.ouvrir(b, b.lines[0]);
    expect(ouverture.calls.mostRecent().args[1]!.data.readOnly).toBeTrue();

    // Une ligne à zéro n'a rien à montrer : elle ne s'ouvre pas.
    ouverture.calls.reset();
    component.ouvrir(component.report!.blocks[0], component.report!.blocks[0].lines[0]);
    expect(ouverture).not.toHaveBeenCalled();
  });

  it('un échec de chargement le dit', async () => {
    await setup();
    http.expectOne(`${endpoint}/labels`).flush(LIBELLES);
    http.expectOne(estRapport(component.year, component.month))
      .flush({}, { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();

    expect(component.failed).toBeTrue();
    expect(hote().querySelector('.echec')).not.toBeNull();
  });
});
