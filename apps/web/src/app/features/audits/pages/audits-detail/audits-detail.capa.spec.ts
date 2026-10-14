import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { AuthzService } from '../../../../core/authz/authz.service';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { OpenCapaRequest } from '../../../capa/capa.types';
import {
  CapaCreateDialogComponent, CapaCreateDialogData
} from '../../../capa/pages/capa-create-dialog/capa-create-dialog.component';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse, FindingResponse, FindingType } from '../../audits.types';
import { AuditsDetailComponent } from './audits-detail.component';

/**
 * « Créer une CAPA » sur un constat (ADR 0088) : LA fenêtre commune, source
 * verrouillée sur le constat, ouverte par la route d'audit qui garde le lien.
 */
describe('AuditsDetailComponent — CAPA d\'un constat', () => {

  const PLAN_ID = '33333333-3333-3333-3333-333333333333';
  const DEMANDE: OpenCapaRequest = {
    title: 'Reprendre la procédure', type: 'CORRECTIVE', criticity: 'HIGH',
    ownerId: 'kc-1', ownerName: 'a@b.c', dueDate: '2099-01-01'
  };

  let fixture: ComponentFixture<AuditsDetailComponent>;
  let component: AuditsDetailComponent;
  let audits: jasmine.SpyObj<AuditsService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const constat = (id: string, type: FindingType, over: Partial<FindingResponse> = {}): FindingResponse => ({
    id, planId: PLAN_ID, type, description: `Constat ${id}`, raisedBy: 'u1',
    raisedAt: '2026-08-01T00:00:00Z', createdAt: '', updatedAt: '', ...over
  });

  const plan = (findings: FindingResponse[]): AuditPlanResponse => ({
    id: PLAN_ID, reference: 'AUD-2026-0003', tenantId: 't1', title: 'Audit interne magasin',
    type: 'INTERNAL', status: 'IN_PROGRESS', leadAuditorId: 'u1',
    createdAt: '', updatedAt: '', findings
  } as unknown as AuditPlanResponse);

  beforeEach(async () => {
    audits = jasmine.createSpyObj<AuditsService>('AuditsService', ['getPlan', 'openFindingCapa']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    // La fenêtre rejoue la validation par la route fournie.
    dialog.open.and.callFake(((_c: unknown, config: { data: CapaCreateDialogData }) => ({
      afterClosed: () => config.data.open!(DEMANDE)
    })) as never);

    await TestBed.configureTestingModule({
      declarations: [AuditsDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: AuditsService, useValue: audits },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: AuthzService, useValue: { can: () => of(true) } },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'u1', tenantId: 't', displayName: 'Alice', roles: [] }) } },
        { provide: DirectoryService, useValue: { getByAccount: () => of({ id: 'i', accountId: 'u1', displayName: 'Alice', email: 'a@x.test', active: true }) } },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: PLAN_ID }) } } }
      ],
      // Les panneaux « Rapport » et « Documents » ont leurs propres bancs.
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
  });

  function setup(findings: FindingResponse[]): void {
    audits.getPlan.and.returnValue(of(plan(findings)));
    fixture = TestBed.createComponent(AuditsDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('ne propose la CAPA qu\'aux écarts qui n\'en ont pas', () => {
    setup([]);
    expect(component.canOpenFindingCapa(constat('1', 'MAJOR_NC'))).toBeTrue();
    expect(component.canOpenFindingCapa(constat('1', 'MINOR_NC'))).toBeTrue();
    expect(component.canOpenFindingCapa(constat('1', 'OBSERVATION'))).toBeFalse();
    expect(component.canOpenFindingCapa(constat('1', 'CONFORMITY'))).toBeFalse();
    expect(component.canOpenFindingCapa(constat('1', 'MAJOR_NC', { capaId: 'c1' }))).toBeFalse();
    expect(component.canOpenFindingCapa(constat('1', 'MAJOR_NC', { pendingSync: true }))).toBeFalse();
  });

  it('ouvre la fenêtre commune, source verrouillée sur le constat, puis relit l\'audit', () => {
    const second = constat('f2', 'MAJOR_NC', { clauseRef: '8.5.1' });
    setup([constat('f1', 'OBSERVATION'), second]);
    audits.openFindingCapa.and.returnValue(of({ findingId: 'f2', capaId: 'c9', sourceRef: 'AUD-2026-0003 / constat 2' }));
    audits.getPlan.calls.reset();

    component.openFindingCapa(plan([constat('f1', 'OBSERVATION'), second]), second);

    expect(dialog.open.calls.mostRecent().args[0]).toBe(CapaCreateDialogComponent);
    const data = dialog.open.calls.mostRecent().args[1]!.data as CapaCreateDialogData;
    expect(data.source).toEqual({ type: 'AUDIT', reference: 'AUD-2026-0003 / constat 2' });
    expect(data.prefill).toEqual(jasmine.objectContaining({
      title: 'Constat f2', type: 'CORRECTIVE', criticity: 'HIGH',
      description: 'Audit interne magasin — 8.5.1'
    }));
    expect(audits.openFindingCapa).toHaveBeenCalledWith(PLAN_ID, 'f2', DEMANDE);
    expect(audits.getPlan).toHaveBeenCalled();
  });

  it('un écart mineur sans clause propose une criticité moyenne', () => {
    const mineur = constat('f1', 'MINOR_NC');
    setup([mineur]);
    audits.openFindingCapa.and.returnValue(of({ findingId: 'f1', capaId: 'c9', sourceRef: '' }));

    component.openFindingCapa(plan([mineur]), mineur);

    const data = dialog.open.calls.mostRecent().args[1]!.data as CapaCreateDialogData;
    expect(data.prefill?.criticity).toBe('MEDIUM');
    expect(data.prefill?.description).toBe('Audit interne magasin');
  });

  it('montre « Créer une CAPA » sur un écart sans CAPA, un lien sinon — jamais l\'identifiant', async () => {
    setup([constat('f1', 'MAJOR_NC'), constat('f2', 'MINOR_NC', { capaId: '99999999-9999-9999-9999-999999999999' })]);
    await fixture.whenStable();
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelectorAll('[data-test="creer-capa"]').length).toBe(1);
    expect(el.querySelectorAll('[data-test="voir-capa"]').length).toBe(1);
    expect(el.textContent).not.toContain('99999999-9999');
  });
});
