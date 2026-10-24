import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, fakeAsync, flush, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse, AuditStatus, ChecklistItemResponse } from '../../audits.types';
import { AuditsDetailComponent } from './audits-detail.component';

/**
 * Détail d'un audit — non-régression « impossible d'ajouter un constat » et
 * « d'où vient lead auditor ».
 *
 * Le serveur n'accepte un constat que sur un audit EN COURS. Le bouton restait
 * actif sur un audit planifié (l'état de tout audit qui vient d'être créé) et le
 * refus revenait en message générique. Il est désormais barré, la raison écrite
 * à côté, avec le geste qui la lève. L'auditeur responsable s'affiche par son
 * nom, jamais par l'identifiant de son compte.
 */
describe('AuditsDetailComponent', () => {
  const ID = '11111111-2222-3333-4444-555555555555';
  let fixture: ComponentFixture<AuditsDetailComponent>;
  let component: AuditsDetailComponent;
  let svc: jasmine.SpyObj<AuditsService>;
  let session: AuthUser | null;
  let plan: AuditPlanResponse;
  let directory: jasmine.SpyObj<DirectoryService>;

  const question = { id: 'q1', planId: ID, question: 'Q ?', createdAt: '', updatedAt: '' } as ChecklistItemResponse;

  function makePlan(status: AuditStatus, over: Partial<AuditPlanResponse> = {}): AuditPlanResponse {
    return {
      id: ID, reference: 'AUD-2026-0001', tenantId: 't', title: 'Audit interne', type: 'INTERNAL',
      status, leadAuditorId: 'u1', createdAt: '', updatedAt: '', checklist: [question], findings: [],
      ...over
    };
  }

  function render(p: AuditPlanResponse): void {
    plan = p;
    svc.getPlan.and.callFake(() => of(plan));
    fixture = TestBed.createComponent(AuditsDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  function el(test: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(`[data-test="${test}"]`);
  }

  beforeEach(async () => {
    session = { userId: 'u1', tenantId: 't', displayName: 'Alice Martin', roles: ['quality_manager'] };
    svc = jasmine.createSpyObj<AuditsService>('AuditsService', ['getPlan', 'startPlan']);
    directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['getByAccount']);
    directory.getByAccount.and.callFake(id => id === 'kc-claire'
      ? of({ id: 'i3', accountId: 'kc-claire', displayName: 'Claire Roux', email: 'claire@exemple.test', active: true })
      : throwError(() => ({ status: 404 })));

    await TestBed.configureTestingModule({
      declarations: [AuditsDetailComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: ID }) } } },
        { provide: AuditsService, useValue: svc },
        { provide: MatDialog, useValue: jasmine.createSpyObj('MatDialog', ['open']) },
        { provide: MatSnackBar, useValue: jasmine.createSpyObj('MatSnackBar', ['open']) },
        { provide: AuthService, useValue: { snapshot: () => session } },
        { provide: DirectoryService, useValue: directory }
      ],
      // Les panneaux « Rapport » et « Documents » ont leurs propres bancs.
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  });

  // ---- règle d'ajout d'un constat ---------------------------------------------

  it('audit planifié : « Ajouter un constat » barré, raison et démarrage proposés', fakeAsync(() => {
    render(makePlan('PLANNED'));
    tick(); fixture.detectChanges();

    const button = el('ajouter-constat') as HTMLButtonElement;
    expect(button.disabled).toBeTrue();
    expect(el('raison-constat')?.textContent).toContain('démarrez-le');
    expect(el('demarrer-depuis-constats')).not.toBeNull();
    flush();
  }));

  it('audit planifié sans checklist : la raison dit d\'ajouter une question d\'abord', fakeAsync(() => {
    render(makePlan('PLANNED', { checklist: [] }));
    tick(); fixture.detectChanges();

    expect(el('raison-constat')?.textContent).toContain('checklist');
    expect(el('demarrer-depuis-constats')).toBeNull();
    expect((el('demarrer') as HTMLButtonElement).disabled).toBeTrue();
    flush();
  }));

  it('audit en cours : « Ajouter un constat » actif, aucune raison affichée', fakeAsync(() => {
    render(makePlan('IN_PROGRESS'));
    tick(); fixture.detectChanges();

    expect((el('ajouter-constat') as HTMLButtonElement).disabled).toBeFalse();
    expect(el('raison-constat')).toBeNull();
    flush();
  }));

  it('la règle suit le serveur pour chaque statut', fakeAsync(() => {
    render(makePlan('IN_PROGRESS'));
    expect(component.findingBlocker(makePlan('IN_PROGRESS'))).toBeNull();
    expect(component.findingBlocker(makePlan('PLANNED'))).toBeTruthy();
    expect(component.findingBlocker(makePlan('COMPLETED'))).toBeTruthy();
    expect(component.findingBlocker(makePlan('CANCELLED'))).toBeTruthy();
    expect(component.startBlocker(makePlan('PLANNED', { checklist: [] }))).toBeTruthy();
    expect(component.startBlocker(makePlan('PLANNED'))).toBeNull();
    flush();
  }));

  it('« Démarrer l\'audit » depuis la section des constats lance la transition', fakeAsync(() => {
    svc.startPlan.and.returnValue(of(makePlan('IN_PROGRESS')));
    render(makePlan('PLANNED'));
    tick(); fixture.detectChanges();

    (el('demarrer-depuis-constats') as HTMLButtonElement).click();

    expect(svc.startPlan).toHaveBeenCalledWith(ID);
    flush();
  }));

  // ---- auditeur responsable ------------------------------------------------------

  it('affiche le nom enregistré de l\'auditeur responsable, jamais son identifiant', fakeAsync(() => {
    render(makePlan('PLANNED', { leadAuditorId: 'u2', leadAuditorName: 'Bruno Petit' }));
    tick(); fixture.detectChanges();

    const text = el('auditeur-responsable')?.textContent ?? '';
    expect(text).toContain('Bruno Petit');
    expect(text).not.toContain('u2');
    flush();
  }));

  it('audit ancien sans nom : soi-même par son nom de session, un autre par un libellé neutre', fakeAsync(() => {
    render(makePlan('PLANNED'));
    expect(component.leadAuditorLabel(makePlan('PLANNED'))).toBe('Alice Martin');
    const autre = component.leadAuditorLabel(makePlan('PLANNED', { leadAuditorId: 'aaaa-bbbb' }));
    expect(autre).not.toContain('aaaa-bbbb');
    expect(autre).toContain('nom non enregistré');
    flush();
  }));

  it('audit ancien sans nom : le nom d\'un collègue se lit dans l\'annuaire, sans /api/v1/users', fakeAsync(() => {
    render(makePlan('PLANNED', { leadAuditorId: 'kc-claire' }));
    tick(); fixture.detectChanges();

    expect(directory.getByAccount).toHaveBeenCalledWith('kc-claire');
    expect(el('auditeur-responsable')?.textContent).toContain('Claire Roux');
    flush();
  }));

  it('ne consulte pas l\'annuaire quand le nom est recopié ou que l\'auditeur est soi-même', fakeAsync(() => {
    render(makePlan('PLANNED', { leadAuditorId: 'u2', leadAuditorName: 'Bruno Petit' }));
    tick();
    render(makePlan('PLANNED'));
    tick();

    expect(directory.getByAccount).not.toHaveBeenCalled();
    flush();
  }));

  // ---- documents ------------------------------------------------------------------

  it('verrouille les documents d\'un audit annulé ou au rapport émis', fakeAsync(() => {
    render(makePlan('IN_PROGRESS'));
    expect(component.documentsLockReason(makePlan('IN_PROGRESS'))).toBeNull();
    expect(component.documentsLockReason(makePlan('CANCELLED'))).toBeTruthy();

    component.onReportIssued(true);
    tick();
    expect(component.documentsLockReason(makePlan('COMPLETED'))).toBeTruthy();
    flush();
  }));
});
