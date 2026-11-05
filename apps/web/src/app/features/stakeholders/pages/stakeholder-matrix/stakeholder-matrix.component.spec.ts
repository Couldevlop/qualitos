import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StakeholdersService } from '../../stakeholders.service';
import { ImpactLevel, StakeholderRow } from '../../stakeholders.types';
import { StakeholderMatrixComponent } from './stakeholder-matrix.component';

/**
 * Coter est un geste de revue : on ajuste plusieurs lignes, on enregistre une
 * fois. Et seules les lignes RÉELLEMENT modifiées partent.
 */
describe('StakeholderMatrixComponent', () => {

  function row(id: string, quality: ImpactLevel, regulatory: ImpactLevel): StakeholderRow {
    return {
      id, name: `Partie ${id}`, category: 'CUSTOMERS', side: 'EXTERNAL',
      qualityImpact: quality, regulatoryImpact: regulatory,
      criticality: quality === 'HIGH' || regulatory === 'HIGH' ? 'HIGH' : quality,
      relevance: 'RELEVANT', lastReviewedOn: null, nextReviewOn: null,
      reviewOverdue: false, requirementCount: 0
    };
  }

  const ROWS = [row('a', 'LOW', 'LOW'), row('b', 'MEDIUM', 'LOW'), row('c', 'HIGH', 'MEDIUM')];

  let fixture: ComponentFixture<StakeholderMatrixComponent>;
  let component: StakeholderMatrixComponent;
  let svc: jasmine.SpyObj<StakeholdersService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function build(canManage = true): Promise<void> {
    svc = jasmine.createSpyObj<StakeholdersService>('StakeholdersService', ['rate']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [StakeholderMatrixComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: StakeholdersService, useValue: svc },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(StakeholderMatrixComponent);
    component = fixture.componentInstance;
    component.rows = ROWS;
    component.canManage = canManage;
    component.ngOnChanges({ rows: { previousValue: undefined, currentValue: ROWS,
                                    firstChange: true, isFirstChange: () => true } });
    fixture.detectChanges();
  }

  beforeEach(async () => build());

  it('n’envoie QUE les lignes recotées', () => {
    svc.rate.and.returnValue(of(ROWS));

    component.onLevel(ROWS[1], 'qualityImpact', 'HIGH');
    expect(component.pendingCount).toBe(1);

    component.save();

    expect(svc.rate).toHaveBeenCalledWith({
      b: { qualityImpact: 'HIGH', regulatoryImpact: 'LOW' }
    });
  });

  it('une note ramenée à sa valeur d’origine quitte le lot', () => {
    component.onLevel(ROWS[0], 'regulatoryImpact', 'HIGH');
    expect(component.pendingCount).toBe(1);

    component.onLevel(ROWS[0], 'regulatoryImpact', 'LOW');
    expect(component.pendingCount).toBe(0);
    expect(component.pending(ROWS[0])).toBeFalse();
  });

  it('garde les deux notes d’une ligne quand une seule change', () => {
    svc.rate.and.returnValue(of(ROWS));

    component.onLevel(ROWS[2], 'regulatoryImpact', 'HIGH');
    component.save();

    expect(svc.rate).toHaveBeenCalledWith({
      c: { qualityImpact: 'HIGH', regulatoryImpact: 'HIGH' }
    });
  });

  it('rend au registre les lignes que le serveur recalcule, et vide le lot', () => {
    const recalculees = [row('a', 'LOW', 'LOW')];
    svc.rate.and.returnValue(of(recalculees));
    const rendues: StakeholderRow[][] = [];
    component.rated.subscribe(rows => rendues.push(rows));

    component.onLevel(ROWS[0], 'qualityImpact', 'HIGH');
    component.save();

    expect(rendues).toEqual([recalculees]);
    expect(component.pendingCount).toBe(0);
  });

  it('sans ligne recotée, n’appelle pas le serveur', () => {
    component.save();
    expect(svc.rate).not.toHaveBeenCalled();
  });

  it('sur échec, garde le lot pour qu’on puisse réessayer', () => {
    svc.rate.and.returnValue(throwError(() => ({ status: 500 })));

    component.onLevel(ROWS[0], 'qualityImpact', 'HIGH');
    component.save();

    expect(component.pendingCount).toBe(1);
    expect(snack.open).toHaveBeenCalled();
    expect(component.saving).toBeFalse();
  });

  it('en lecture seule, ne cote rien : ni sélecteur, ni appel', async () => {
    await build(false);

    component.onLevel(ROWS[0], 'qualityImpact', 'HIGH');
    expect(component.pendingCount).toBe(0);

    component.save();
    expect(svc.rate).not.toHaveBeenCalled();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-test="lecture-seule"]')).not.toBeNull();
    expect(el.querySelector('mat-select')).toBeNull();
  });

  it('la criticité reste celle du serveur : elle n’est jamais saisie', () => {
    component.onLevel(ROWS[0], 'qualityImpact', 'HIGH');

    // La ligne affiche encore la criticité enregistrée, et signale le recalcul.
    expect(ROWS[0].criticality).toBe('LOW');
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-test="recalcul"]')).not.toBeNull();
  });

  it('un rechargement du registre abandonne les cotations en attente', () => {
    component.onLevel(ROWS[0], 'qualityImpact', 'HIGH');
    expect(component.pendingCount).toBe(1);

    const fraiches = [row('a', 'MEDIUM', 'LOW')];
    component.rows = fraiches;
    component.ngOnChanges({ rows: { previousValue: ROWS, currentValue: fraiches,
                                    firstChange: false, isFirstChange: () => false } });

    expect(component.pendingCount).toBe(0);
  });

  it('la grille compte les lignes telles qu’elles sont affichées', () => {
    // Lignes : réglementaire du plus élevé au plus faible ; colonnes : qualité.
    const avant = component.grid;
    expect(avant[2][0].count).toBe(1);   // a : qualité faible, réglementaire faible
    expect(avant[2][1].count).toBe(1);   // b : qualité moyenne, réglementaire faible
    expect(avant[1][2].count).toBe(1);   // c : qualité élevée, réglementaire moyen

    component.onLevel(ROWS[0], 'qualityImpact', 'HIGH');
    const apres = component.grid;
    expect(apres[2][0].count).toBe(0);
    expect(apres[2][2].count).toBe(1);
  });
});
