import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CalibrationService } from '../../calibration.service';
import {
  CalibrationOverview, EquipmentResponse, EquipmentRow, PlanResponse
} from '../../calibration.types';
import { CalibrationListComponent } from './calibration-list.component';

describe('CalibrationListComponent', () => {
  let fixture: ComponentFixture<CalibrationListComponent>;
  let component: CalibrationListComponent;
  let svc: jasmine.SpyObj<CalibrationService>;
  let dialogResult: EquipmentResponse | undefined;
  let router: Router;

  const equipment = (id: string, over: Partial<EquipmentResponse> = {}): EquipmentResponse => ({
    id, tenantId: 't1', code: 'EQ-' + id, name: 'Instrument ' + id,
    manufacturer: null, model: null, serialNumber: null, location: 'Atelier A',
    status: 'ACTIVE', critical: false, iotDeviceId: null, ownerUserId: null,
    createdBy: 'u1', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over
  });

  const plan = (equipmentId: string, over: Partial<PlanResponse> = {}): PlanResponse => ({
    id: 'p-' + equipmentId, tenantId: 't1', equipmentId, frequencyMonths: 12,
    procedureReference: null, tolerance: null, accreditationRef: null,
    lastCalibratedOn: null, nextDueOn: '2026-02-01', overdue: true,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const overview = (rows: EquipmentRow[]): CalibrationOverview => ({
    page: {
      content: rows.map(r => r.equipment), totalElements: rows.length,
      totalPages: 1, number: 0, size: 25
    },
    rows,
    overdueCount: rows.filter(r => r.due === 'OVERDUE').length,
    dueSoonCount: rows.filter(r => r.due === 'DUE_SOON').length
  });

  const defaultRows: EquipmentRow[] = [
    { equipment: equipment('e1', { critical: true }), plan: plan('e1'), due: 'OVERDUE' },
    {
      equipment: equipment('e2'),
      plan: plan('e2', { overdue: false, nextDueOn: '2026-08-20' }), due: 'DUE_SOON'
    },
    { equipment: equipment('e3', { location: null }), plan: null, due: 'OK' }
  ];

  function build(result: Observable<CalibrationOverview> = of(overview(defaultRows))): void {
    svc.overview.and.returnValue(result);
    fixture = TestBed.createComponent(CalibrationListComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }

  /**
   * `deferredView` livre les états de chargement/erreur via `asyncScheduler`
   * (macrotâche) : il faut laisser passer un tour de boucle avant de les lire.
   * `fakeAsync` ne convient pas ici — Material laisse des timers en attente.
   */
  async function settle(): Promise<void> {
    await new Promise<void>(resolve => setTimeout(resolve));
    fixture.detectChanges();
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<CalibrationService>('CalibrationService', ['overview']);
    dialogResult = undefined;

    await TestBed.configureTestingModule({
      declarations: [CalibrationListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CalibrationService, useValue: svc },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(dialogResult) }) } },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']) },
        provideRouter([])
      ]
    }).compileComponents();
  });

  it('rend une ligne par équipement et les trois tuiles de synthèse', () => {
    build();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('tbody tr').length).toBe(3);
    expect(el.querySelectorAll('.tile').length).toBe(3);
    expect(el.querySelectorAll('.tile')[1].textContent).toContain('1');
  });

  it('signale visuellement la ligne dont la calibration est dépassée', () => {
    build();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('tr.row--overdue').length).toBe(1);
    expect(el.querySelector('tr.row--overdue')?.textContent).toContain('En retard');
  });

  it('affiche le marqueur « critique » sur les instruments critiques', () => {
    build();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelectorAll('.critical').length).toBe(1);
  });

  it('propose un lien vers la fiche de chaque équipement', () => {
    build();
    const links = (fixture.nativeElement as HTMLElement).querySelectorAll('td a[href]');
    expect(links.length).toBe(3);
    expect(links[0].getAttribute('href')).toContain('/calibration/');
  });

  it('affiche l’état vide quand aucun équipement ne correspond au filtre', async () => {
    build(of(overview([])));
    await settle();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.empty')).toBeTruthy();
    expect(el.querySelectorAll('tbody tr').length).toBe(0);
  });

  it('affiche un bandeau d’erreur si le chargement échoue', async () => {
    build(throwError(() => ({ status: 500 })));
    await settle();
    const banner = (fixture.nativeElement as HTMLElement).querySelector('.banner-error');
    expect(banner).toBeTruthy();
    expect(banner?.textContent).toContain('Erreur serveur');
  });

  it('recharge à la première page quand le filtre de statut change', () => {
    build();
    component.pageIndex = 3;
    component.onStatusChange('RETIRED');
    expect(component.pageIndex).toBe(0);
    expect(svc.overview).toHaveBeenCalledWith('RETIRED', 0, 25, 60);
  });

  it('n’envoie aucun statut quand le filtre est remis à « tous »', () => {
    build();
    component.onStatusChange('');
    expect(svc.overview).toHaveBeenCalledWith(null, 0, 25, 60);
  });

  it('recharge avec le nouvel horizon d’échéance', () => {
    build();
    component.onHorizonChange(30);
    expect(component.horizonDays).toBe(30);
    expect(svc.overview).toHaveBeenCalledWith(null, 0, 25, 30);
  });

  it('recharge à la pagination', () => {
    build();
    component.onPage({ pageIndex: 2, pageSize: 50, length: 120 });
    expect(svc.overview).toHaveBeenCalledWith(null, 2, 50, 60);
  });

  it('rafraîchit la vue à la demande', () => {
    build();
    const calls = svc.overview.calls.count();
    component.refresh();
    expect(svc.overview.calls.count()).toBe(calls + 1);
  });

  it('ouvre la fiche du nouvel équipement après création', () => {
    build();
    dialogResult = equipment('e9');
    const nav = spyOn(router, 'navigate');
    component.create();
    expect(nav).toHaveBeenCalledWith(['/calibration', 'e9']);
  });

  it('ne navigue pas si la création est annulée', () => {
    build();
    dialogResult = undefined;
    const nav = spyOn(router, 'navigate');
    component.create();
    expect(nav).not.toHaveBeenCalled();
  });

  it('mappe les états d’échéance sur un libellé et une tonalité', () => {
    build();
    expect(component.dueTone('OVERDUE')).toBe('danger');
    expect(component.dueTone('DUE_SOON')).toBe('warn');
    expect(component.dueTone('OK')).toBe('neutral');
    expect(component.dueLabel('OVERDUE')).toBe('En retard');
    expect(component.dueLabel('DUE_SOON')).toBe('Échéance proche');
    expect(component.dueLabel('OK')).toBe('À jour');
  });

  it('mappe les statuts d’équipement sur un libellé et une tonalité', () => {
    build();
    expect(component.statusLabel('ACTIVE')).toBe('En service');
    expect(component.statusLabel('OUT_OF_SERVICE')).toBe('Hors service');
    expect(component.statusLabel('RETIRED')).toBe('Réformé');
    expect(component.statusTone('ACTIVE')).toBe('success');
    expect(component.statusTone('OUT_OF_SERVICE')).toBe('warn');
    expect(component.statusTone('RETIRED')).toBe('neutral');
  });

  it('identifie les lignes par l’identifiant d’équipement', () => {
    build();
    expect(component.trackById(0, defaultRows[0])).toBe('e1');
  });
});
