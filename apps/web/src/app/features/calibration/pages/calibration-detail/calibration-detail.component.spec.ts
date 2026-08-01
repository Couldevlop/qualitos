import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { Observable, of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CalibrationService } from '../../calibration.service';
import {
  EquipmentDetail, EquipmentResponse, EquipmentSummary, MsaResponse, PlanResponse, RecordResponse
} from '../../calibration.types';
import { CalibrationDetailComponent } from './calibration-detail.component';

describe('CalibrationDetailComponent', () => {
  let fixture: ComponentFixture<CalibrationDetailComponent>;
  let component: CalibrationDetailComponent;
  let svc: jasmine.SpyObj<CalibrationService>;
  let router: Router;
  /** Valeur renvoyée par le prochain dialogue ouvert (confirmation ou formulaire). */
  let dialogResult: unknown;
  let roles: string[];

  const equipment = (over: Partial<EquipmentResponse> = {}): EquipmentResponse => ({
    id: 'e1', tenantId: 't1', code: 'CAL-001', name: 'Pied à coulisse',
    manufacturer: 'Mitutoyo', model: 'CD-15', serialNumber: 'SN-42', location: 'Atelier A',
    status: 'ACTIVE', critical: false, iotDeviceId: null, ownerUserId: null,
    createdBy: 'u1', createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z',
    ...over
  });

  const summary = (over: Partial<EquipmentSummary> = {}): EquipmentSummary => ({
    equipmentId: 'e1', status: 'ACTIVE', critical: false,
    lastCalibratedOn: '2026-05-01', nextDueOn: '2027-05-01', overdue: false,
    lastResult: 'PASS', passRecords: 2, failRecords: 1, conditionalRecords: 0,
    msaPass: 1, msaFail: 0, ...over
  });

  const plan = (over: Partial<PlanResponse> = {}): PlanResponse => ({
    id: 'p1', tenantId: 't1', equipmentId: 'e1', frequencyMonths: 12,
    procedureReference: 'MO-CAL-014', tolerance: '± 0,02 mm', accreditationRef: 'COFRAC 2-1234',
    lastCalibratedOn: '2026-05-01', nextDueOn: '2027-05-01', overdue: false,
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const record = (over: Partial<RecordResponse> = {}): RecordResponse => ({
    id: 'r1', tenantId: 't1', equipmentId: 'e1', performedOn: '2026-05-01',
    performedByUserId: 'u1', performedByOrg: null, result: 'PASS',
    measurements: '0,01 mm sur 3 points', certificateReference: 'CERT-2026-77',
    nextDueOverride: null, createdAt: '2026-05-01T00:00:00Z', ...over
  });

  const msa = (over: Partial<MsaResponse> = {}): MsaResponse => ({
    id: 'm1', tenantId: 't1', equipmentId: 'e1', type: 'GAGE_R_R', performedOn: '2026-05-02',
    studyValue: 12.5, passingThreshold: 30, result: 'PASS', notes: null,
    createdBy: 'u1', createdAt: '2026-05-02T00:00:00Z', ...over
  });

  const detail = (over: Partial<EquipmentDetail> = {}): EquipmentDetail => ({
    equipment: equipment(), summary: summary(), plan: plan(),
    records: [record()], msa: [msa()], ...over
  });

  function build(result: Observable<EquipmentDetail> = of(detail())): void {
    svc.detail.and.returnValue(result);
    fixture = TestBed.createComponent(CalibrationDetailComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  }

  function textOf(selector: string): string {
    return ((fixture.nativeElement as HTMLElement).querySelector(selector)?.textContent ?? '').trim();
  }

  function buttonWith(label: string): HTMLButtonElement | null {
    const buttons = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button')) as HTMLButtonElement[];
    return buttons.find(b => (b.textContent ?? '').includes(label)) ?? null;
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<CalibrationService>('CalibrationService',
      ['detail', 'setStatus', 'deleteEquipment', 'deletePlan']);
    svc.setStatus.and.returnValue(of(equipment({ status: 'OUT_OF_SERVICE' })));
    svc.deleteEquipment.and.returnValue(of(void 0));
    svc.deletePlan.and.returnValue(of(void 0));
    dialogResult = true;
    roles = ['quality_manager'];

    await TestBed.configureTestingModule({
      declarations: [CalibrationDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CalibrationService, useValue: svc },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(dialogResult) }) } },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']) },
        {
          provide: AuthService,
          useValue: { snapshot: () => ({ userId: 'u1', tenantId: 't1', displayName: 'D', roles }) }
        },
        provideRouter([]),
        // Après `provideRouter` : celui-ci fournit aussi ActivatedRoute (racine, sans
        // paramètre) et écraserait le double si l'ordre était inversé.
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'e1' })) } }
      ]
    }).compileComponents();
  });

  // ---- Rendu ----------------------------------------------------------------

  it('charge la fiche de l’équipement de la route', () => {
    build();
    expect(svc.detail).toHaveBeenCalledWith('e1');
    expect(component.equipmentId).toBe('e1');
    expect(textOf('h1')).toBe('Pied à coulisse');
  });

  it('affiche le plan, les enregistrements et les études MSA', () => {
    build();
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.plan')).toBeTruthy();
    expect(el.textContent).toContain('MO-CAL-014');
    // 1 ligne d'enregistrement + 1 ligne MSA
    expect(el.querySelectorAll('tbody tr').length).toBe(2);
    expect(el.querySelectorAll('.empty').length).toBe(0);
  });

  it('affiche l’état vide et l’action de création quand aucun plan n’existe', () => {
    build(of(detail({ plan: null })));
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.plan')).toBeNull();
    expect(buttonWith('Définir un plan')).toBeTruthy();
    expect(buttonWith('Modifier le plan')).toBeNull();
  });

  it('affiche les états vides des historiques', () => {
    build(of(detail({ records: [], msa: [] })));
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('Aucune calibration enregistrée');
    expect(el.textContent).toContain('Aucune étude MSA');
  });

  it('signale une échéance dépassée dans la synthèse', () => {
    build(of(detail({ summary: summary({ overdue: true }), plan: plan({ overdue: true }) })));
    const el: HTMLElement = fixture.nativeElement;
    expect(el.querySelector('.tile[data-tone="danger"]')).toBeTruthy();
    expect(el.textContent).toContain('En retard');
  });

  it('affiche un bandeau d’erreur si la fiche est introuvable', async () => {
    build(throwError(() => ({ status: 404 })));
    // `deferredView` livre l'erreur via `asyncScheduler` : un tour de boucle suffit.
    // `fakeAsync` ne convient pas — Material laisse des timers en attente.
    await new Promise<void>(resolve => setTimeout(resolve));
    fixture.detectChanges();
    expect(textOf('.banner-error')).toContain('Équipement introuvable');
  });

  // ---- Actions proposées selon l'état ------------------------------------------

  it('propose la saisie de calibration uniquement sur un instrument en service', () => {
    build();
    expect(buttonWith('Enregistrer une calibration')).toBeTruthy();

    build(of(detail({ equipment: equipment({ status: 'OUT_OF_SERVICE' }) })));
    expect(buttonWith('Enregistrer une calibration')).toBeNull();
    expect((fixture.nativeElement as HTMLElement).textContent)
      .toContain('Remettez l\'instrument en service');
  });

  it('fige toutes les actions d’un instrument réformé', () => {
    build(of(detail({ equipment: equipment({ status: 'RETIRED' }) })));
    expect(buttonWith('Modifier')).toBeNull();
    expect(buttonWith('Réformer')).toBeNull();
    expect(buttonWith('Définir un plan')).toBeNull();
    expect(buttonWith('Ajouter une étude')).toBeNull();
    expect((fixture.nativeElement as HTMLElement).querySelector('.retired-note')).toBeTruthy();
  });

  it('bloque la remise en service d’un instrument critique sans calibration conforme', () => {
    build(of(detail({
      equipment: equipment({ status: 'OUT_OF_SERVICE', critical: true }),
      summary: summary({ status: 'OUT_OF_SERVICE', critical: true, lastResult: 'FAIL' })
    })));
    const btn = buttonWith('Remettre en service');
    expect(btn).toBeTruthy();
    expect(btn!.disabled).toBeTrue();
  });

  it('autorise la remise en service après une calibration conforme', () => {
    build(of(detail({
      equipment: equipment({ status: 'OUT_OF_SERVICE', critical: true }),
      summary: summary({ status: 'OUT_OF_SERVICE', critical: true, lastResult: 'PASS' })
    })));
    expect(buttonWith('Remettre en service')!.disabled).toBeFalse();
  });

  it('masque la suppression du plan d’un instrument critique', () => {
    build(of(detail({ equipment: equipment({ critical: true }) })));
    expect(buttonWith('Supprimer le plan')).toBeNull();
    expect(component.canDeletePlan(detail({ equipment: equipment({ critical: true }) }))).toBeFalse();
  });

  it('masque les suppressions pour un rôle sans droit', async () => {
    roles = ['user'];
    build();
    expect(component.canDelete).toBeFalse();
    expect(buttonWith('Supprimer')).toBeNull();
  });

  // ---- Transitions d'état ---------------------------------------------------------

  it('met hors service après confirmation', () => {
    build();
    component.outOfService(detail());
    expect(svc.setStatus).toHaveBeenCalledWith('e1', 'OUT_OF_SERVICE');
  });

  it('n’applique rien si la confirmation est refusée', () => {
    build();
    dialogResult = false;
    component.retire(detail());
    expect(svc.setStatus).not.toHaveBeenCalled();
  });

  it('réforme après confirmation', () => {
    build();
    component.retire(detail());
    expect(svc.setStatus).toHaveBeenCalledWith('e1', 'RETIRED');
  });

  it('remet en service sans confirmation (action réversible)', () => {
    build();
    component.returnToService(detail({ equipment: equipment({ status: 'OUT_OF_SERVICE' }) }));
    expect(svc.setStatus).toHaveBeenCalledWith('e1', 'ACTIVE');
  });

  it('affiche l’échec d’une transition refusée par le serveur', () => {
    build();
    const snack = TestBed.inject(MatSnackBar) as jasmine.SpyObj<MatSnackBar>;
    svc.setStatus.and.returnValue(throwError(() => ({ status: 409 })));
    component.returnToService(detail());
    expect(snack.open).toHaveBeenCalled();
    expect(component.pending).toBeFalse();
  });

  // ---- Suppressions -----------------------------------------------------------------

  it('supprime l’équipement puis revient à la liste', () => {
    build();
    const nav = spyOn(router, 'navigate');
    component.remove(detail());
    expect(svc.deleteEquipment).toHaveBeenCalledWith('e1');
    expect(nav).toHaveBeenCalledWith(['/calibration']);
  });

  it('ne supprime pas l’équipement si la confirmation est refusée', () => {
    build();
    dialogResult = false;
    component.remove(detail());
    expect(svc.deleteEquipment).not.toHaveBeenCalled();
  });

  it('supprime le plan puis recharge la fiche', () => {
    build();
    const calls = svc.detail.calls.count();
    component.removePlan(detail());
    expect(svc.deletePlan).toHaveBeenCalledWith('e1');
    expect(svc.detail.calls.count()).toBe(calls + 1);
  });

  // ---- Dialogues d'édition -------------------------------------------------------------

  it('recharge après une édition confirmée', () => {
    build();
    const calls = svc.detail.calls.count();
    component.edit(detail());
    component.editPlan(detail());
    component.addRecord(detail());
    component.addMsa(detail());
    expect(svc.detail.calls.count()).toBe(calls + 4);
  });

  it('ne recharge pas quand un dialogue est annulé', () => {
    build();
    dialogResult = undefined;
    const calls = svc.detail.calls.count();
    component.edit(detail());
    component.addRecord(detail());
    expect(svc.detail.calls.count()).toBe(calls);
  });

  // ---- Présentation ----------------------------------------------------------------------

  it('mappe les résultats de calibration sur un libellé et une tonalité', () => {
    build();
    expect(component.resultLabel('PASS')).toBe('Conforme');
    expect(component.resultLabel('CONDITIONAL')).toBe('Conforme sous condition');
    expect(component.resultLabel('FAIL')).toBe('Non conforme');
    expect(component.resultTone('PASS')).toBe('success');
    expect(component.resultTone('CONDITIONAL')).toBe('warn');
    expect(component.resultTone('FAIL')).toBe('danger');
    expect(component.resultTone(null)).toBe('neutral');
  });

  it('mappe les statuts d’équipement', () => {
    build();
    expect(component.statusLabel('ACTIVE')).toBe('En service');
    expect(component.statusLabel('OUT_OF_SERVICE')).toBe('Hors service');
    expect(component.statusLabel('RETIRED')).toBe('Réformé');
    expect(component.statusTone('ACTIVE')).toBe('success');
    expect(component.statusTone('OUT_OF_SERVICE')).toBe('warn');
    expect(component.statusTone('RETIRED')).toBe('neutral');
  });

  it('mappe les types et verdicts MSA', () => {
    build();
    expect(component.msaTypeLabel('GAGE_R_R')).toContain('R&R');
    expect(component.msaTypeLabel('BIAS')).toContain('Justesse');
    expect(component.msaTypeLabel('LINEARITY')).toBe('Linéarité');
    expect(component.msaTypeLabel('STABILITY')).toBe('Stabilité');
    expect(component.msaResultLabel('PASS')).toBe('Acceptable');
    expect(component.msaResultLabel('MARGINAL')).toBe('Marginal');
    expect(component.msaResultLabel('FAIL')).toBe('Inacceptable');
    expect(component.msaResultTone('PASS')).toBe('success');
    expect(component.msaResultTone('MARGINAL')).toBe('warn');
    expect(component.msaResultTone('FAIL')).toBe('danger');
  });
});
