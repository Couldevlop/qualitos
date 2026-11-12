import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthzService } from '../../core/authz/authz.service';
import { SharedModule } from '../../shared/shared.module';
import { UiModule } from '../../shared/ui/ui.module';
import { ForecastSharedModule } from '../forecast/forecast-shared.module';
import { ForecastService } from '../forecast/forecast.service';
import { KpisService } from './kpis.service';
import { KpiResponse } from './kpis.types';
import { KpisDetailComponent } from './pages/kpis-detail/kpis-detail.component';
import { KpisListComponent } from './pages/kpis-list/kpis-list.component';

/**
 * Ce que les écrans d'indicateurs proposent, selon les droits.
 *
 * <p>Les routes d'écriture exigent désormais `kpi.manage` (définir l'indicateur,
 * sa cible, ses seuils, son cycle de vie) et `kpi.measure` (consigner un relevé).
 * L'écran doit s'aligner : un bouton proposé à qui sera refusé est un bouton qui
 * mentira au clic — c'est le défaut que l'alias de rôle du directeur qualité
 * avait déjà révélé, en sens inverse.
 *
 * <p>Deux droits et non un, parce que les deux gestes ne sont pas de même
 * nature : déplacer une CIBLE est une décision, consigner un RELEVÉ est un fait.
 * Un opérateur doit pouvoir dire ce qu'il a mesuré sans pouvoir déplacer la barre.
 */
describe('Indicateurs — ce que les droits laissent voir', () => {

  const ID = '11111111-1111-1111-1111-111111111111';

  const indicateur = (over: Partial<KpiResponse> = {}): KpiResponse => ({
    id: ID, tenantId: 't1', code: 'NC-PPM', name: 'Taux de NC client',
    category: 'Qualité', unit: 'ppm', direction: 'LOWER_IS_BETTER',
    frequency: 'MONTHLY', status: 'ACTIVE',
    createdAt: '2026-10-01T00:00:00Z', updatedAt: '2026-10-01T00:00:00Z',
    ...over
  } as KpiResponse);

  function droits(accordes: string[]): Pick<AuthzService, 'can'> {
    return { can: (code: string) => of(accordes.includes(code)) };
  }

  // ---------- la liste ----------

  async function liste(accordes: string[]): Promise<ComponentFixture<KpisListComponent>> {
    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      declarations: [KpisListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: AuthzService, useValue: droits(accordes) },
        { provide: KpisService, useValue: { list: () => of({ content: [], totalElements: 0 }) } },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(null) }) } },
        provideRouter([])
      ]
    }).compileComponents();
    const f = TestBed.createComponent(KpisListComponent);
    f.detectChanges();
    return f;
  }

  it('ne propose pas de créer un indicateur sans le droit de le définir', async () => {
    const f = await liste([]);
    expect(f.componentInstance.canManage).toBeFalse();
    expect((f.nativeElement as HTMLElement).textContent).not.toContain('Nouveau KPI');
  });

  it('le propose au pilotage', async () => {
    const f = await liste(['kpi.manage']);
    expect(f.componentInstance.canManage).toBeTrue();
    expect((f.nativeElement as HTMLElement).textContent).toContain('Nouveau KPI');
  });

  // ---------- la fiche ----------

  async function fiche(accordes: string[]): Promise<ComponentFixture<KpisDetailComponent>> {
    TestBed.resetTestingModule();
    const svc = {
      get: () => of(indicateur()),
      currentStatus: () => of(null),
      trend: () => of(null),
      listMeasurements: () => of({ content: [], totalElements: 0 })
    };
    await TestBed.configureTestingModule({
      declarations: [KpisDetailComponent],
      // La fiche porte le panneau de prevision : sans son module, le gabarit ne
      // compile pas et l'on ne verrait jamais les boutons qu'on vient juger.
      imports: [SharedModule, UiModule, ForecastSharedModule, NoopAnimationsModule],
      providers: [
        { provide: AuthzService, useValue: droits(accordes) },
        { provide: KpisService, useValue: svc },
        { provide: ForecastService, useValue: { forecast: () => of(null) } },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(null) }) } },
        { provide: MatSnackBar, useValue: { open: () => undefined } },
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: ID })) } }
      ]
    }).compileComponents();
    const f = TestBed.createComponent(KpisDetailComponent);
    f.detectChanges();
    return f;
  }

  it('se consulte sans rien pouvoir y changer, sans aucun des deux droits', async () => {
    const f = await fiche([]);
    const hote = f.nativeElement as HTMLElement;

    // La fiche RESTE lisible : savoir où en est ce sur quoi on est jugé n'est le
    // secret de personne. Seuls les gestes disparaissent.
    expect(hote.textContent).toContain('Taux de NC client');
    const barre = hote.querySelector('.toolbar')?.textContent ?? '';
    for (const geste of ['Modifier', 'Mesurer', 'Rouvrir', 'Archiver', 'Supprimer']) {
      expect(barre).withContext(geste).not.toContain(geste);
    }

    // Et l'écran n'envoie pas chercher ce qui n'y est pas : l'invite de l'état
    // vide disait « Cliquez "Mesurer" » à qui n'a pas le bouton.
    expect(hote.textContent).not.toContain('Cliquez "Mesurer"');
  });

  /**
   * Le cas qui justifie deux droits plutôt qu'un : l'opérateur dit ce qu'il a
   * mesuré, et ne touche pas à la barre qu'on lui fixe.
   */
  it('laisse mesurer sans laisser redéfinir', async () => {
    const f = await fiche(['kpi.measure']);
    const texte = (f.nativeElement as HTMLElement).textContent ?? '';

    expect(f.componentInstance.canMeasure).toBeTrue();
    expect(f.componentInstance.canManage).toBeFalse();
    expect(texte).toContain('Mesurer');
    expect(texte).not.toContain('Modifier');
    expect(texte).not.toContain('Archiver');
  });

  it('ouvre tous les gestes au pilotage', async () => {
    const f = await fiche(['kpi.manage', 'kpi.measure']);
    const texte = (f.nativeElement as HTMLElement).textContent ?? '';

    expect(texte).toContain('Modifier');
    expect(texte).toContain('Mesurer');
    expect(texte).toContain('Archiver');
    expect(texte).toContain('Supprimer');
  });
});
