import { SimpleChange } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { MatDialog } from '@angular/material/dialog';
import { MatSelect } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, Subject, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse, AuditReport, AuditStatus } from '../../audits.types';
import { AuditReportPanelComponent, ISSUE_ROLES } from './audit-report-panel.component';

/**
 * Section « Rapport » : brouillon, puis émission qui fige.
 *
 * Ce qui se vérifie : la raison d'une émission barrée est écrite, le brouillon
 * part tel qu'affiché, l'émission enregistre d'abord ce qui est à l'écran,
 * un rapport émis n'est plus modifiable, et l'absence de second facteur mène à
 * la réauthentification plutôt qu'à un refus muet.
 */
describe('AuditReportPanelComponent', () => {
  let fixture: ComponentFixture<AuditReportPanelComponent>;
  let component: AuditReportPanelComponent;
  let svc: jasmine.SpyObj<AuditsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let auth: jasmine.SpyObj<AuthService>;
  let snackAction: Subject<void>;

  const figures = { majorNc: 1, minorNc: 2, observation: 0, opportunity: 0, conformity: 1, total: 4, conformityScore: 80 };

  function report(over: Partial<AuditReport> = {}): AuditReport {
    return { id: 'r1', planId: 'p1', status: 'DRAFT', summary: null, strengths: null, improvements: null,
      conclusion: null, keyFigures: figures, ...over };
  }

  function plan(status: AuditStatus, over: Partial<AuditPlanResponse> = {}): AuditPlanResponse {
    return { id: 'p1', reference: 'AUD-1', tenantId: 't', title: 'A', type: 'INTERNAL', status,
      leadAuditorId: 'u1', createdAt: '', updatedAt: 'v1', findings: [], ...over };
  }

  function show(p: AuditPlanResponse, r: AuditReport): void {
    svc.getReport.and.returnValue(of(r));
    component.plan = p;
    component.ngOnChanges({ plan: new SimpleChange(undefined, p, true) });
    fixture.detectChanges();
  }

  function el(test: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(`[data-test="${test}"]`);
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<AuditsService>('AuditsService',
      ['getReport', 'saveReportDraft', 'issueReport', 'generateReport']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snackAction = new Subject<void>();
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    snack.open.and.returnValue({ onAction: () => snackAction.asObservable() } as never);
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['stepUp', 'snapshot', 'hasAnyRole']);
    auth.hasAnyRole.and.returnValue(true);

    await TestBed.configureTestingModule({
      declarations: [AuditReportPanelComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: AuditsService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: auth }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditReportPanelComponent);
    component = fixture.componentInstance;
  });

  function confirmDialog(answer: boolean): void {
    dialog.open.and.returnValue({ afterClosed: () => of(answer) } as never);
  }

  it('montre les chiffres clés et l\'état brouillon', () => {
    show(plan('IN_PROGRESS'), report());
    expect(el('etat-rapport')?.textContent).toContain('Brouillon');
    expect(el('chiffres-cles')?.textContent).toContain('4');
  });

  it('audit en cours : émission barrée, la raison dit qu\'il faut clôturer', () => {
    show(plan('IN_PROGRESS'), report({ summary: 'S', conclusion: 'FAVORABLE' }));
    expect((el('emettre') as HTMLButtonElement).disabled).toBeTrue();
    expect(el('raison-emission')?.textContent).toContain('clôturé');
  });

  it('audit clôturé sans conclusion : la raison le dit', () => {
    show(plan('COMPLETED'), report({ summary: 'S' }));
    expect(component.issueBlocker).toContain('conclusion');
  });

  it('rôle de terrain : émission barrée, la raison renvoie au pilotage qualité', () => {
    auth.hasAnyRole.and.returnValue(false);
    show(plan('COMPLETED'), report({ summary: 'S', conclusion: 'FAVORABLE' }));
    expect(component.issueBlocker).toContain('pilotage qualité');
    expect((el('emettre') as HTMLButtonElement).disabled).toBeTrue();
    expect(auth.hasAnyRole).toHaveBeenCalledWith(ISSUE_ROLES);
  });

  it('audit clôturé et rapport complet : émission possible', () => {
    show(plan('COMPLETED'), report({ summary: 'S', conclusion: 'FAVORABLE' }));
    expect(component.issueBlocker).toBeNull();
    expect((el('emettre') as HTMLButtonElement).disabled).toBeFalse();
  });

  it('enregistre le brouillon tel qu\'affiché, champs vides envoyés à null', () => {
    show(plan('IN_PROGRESS'), report());
    svc.saveReportDraft.and.returnValue(of(report({ summary: 'Synthèse' })));
    component.form.patchValue({ summary: '  Synthèse  ', strengths: '   ', conclusion: 'UNFAVORABLE' });
    component.form.markAsDirty();

    component.saveDraft();

    expect(svc.saveReportDraft).toHaveBeenCalledWith('p1', {
      summary: 'Synthèse', strengths: null, improvements: null, conclusion: 'UNFAVORABLE'
    });
  });

  it('émission : confirme, enregistre d\'abord ce qui est à l\'écran, puis émet', () => {
    show(plan('COMPLETED'), report({ summary: 'S', conclusion: 'FAVORABLE' }));
    component.form.patchValue({ strengths: 'Forces' });
    component.form.markAsDirty();
    confirmDialog(true);
    svc.saveReportDraft.and.returnValue(of(report({ summary: 'S', conclusion: 'FAVORABLE', strengths: 'Forces' })));
    svc.issueReport.and.returnValue(of(report({
      status: 'ISSUED', summary: 'S', conclusion: 'FAVORABLE', issuedAt: '2026-10-14T09:00:00Z',
      issuedByName: 'Alice Martin', contentSha256: 'a'.repeat(64)
    })));
    const issued: boolean[] = [];
    component.issuedChange.subscribe(v => issued.push(v));

    component.issue();
    fixture.detectChanges();

    expect(svc.saveReportDraft).toHaveBeenCalled();
    expect(svc.issueReport).toHaveBeenCalledWith('p1');
    expect(component.issued).toBeTrue();
    expect(component.form.disabled).toBeTrue();
    expect(el('emission')?.textContent).toContain('Alice Martin');
    expect(el('emettre')).toBeNull();
    expect(issued).toContain(true);
  });

  it('émission annulée à la confirmation : rien n\'est envoyé', () => {
    show(plan('COMPLETED'), report({ summary: 'S', conclusion: 'FAVORABLE' }));
    confirmDialog(false);

    component.issue();

    expect(svc.issueReport).not.toHaveBeenCalled();
  });

  it('sans second facteur : propose de se réauthentifier', () => {
    show(plan('COMPLETED'), report({ summary: 'S', conclusion: 'FAVORABLE' }));
    confirmDialog(true);
    svc.issueReport.and.returnValue(throwError(() => ({
      status: 403, error: { type: 'https://qualitos.io/errors/step-up-required' }
    })));
    auth.stepUp.and.returnValue(true);

    component.issue();
    snackAction.next();

    expect(auth.stepUp).toHaveBeenCalledWith('/audits/p1');
  });

  it('rapport émis : lecture seule, aucun bouton IA ni brouillon', () => {
    show(plan('COMPLETED'), report({ status: 'ISSUED', summary: 'S', conclusion: 'FAVORABLE' }));
    expect(component.editable).toBeFalse();
    expect(component.form.disabled).toBeTrue();
    expect(el('enregistrer-brouillon')).toBeNull();
  });

  it('la synthèse IA remplit le brouillon, à relire', () => {
    show(plan('IN_PROGRESS'), report());
    svc.generateReport.and.returnValue(of(plan('IN_PROGRESS', { reportSummary: 'Synthèse IA' })));

    component.generateSummary();

    expect(component.form.getRawValue().summary).toBe('Synthèse IA');
    expect(component.form.dirty).toBeTrue();
  });

  it('un rechargement du plan n\'écrase pas une saisie en cours', () => {
    show(plan('IN_PROGRESS'), report());
    component.form.patchValue({ strengths: 'En cours de saisie' });
    component.form.markAsDirty();

    show(plan('IN_PROGRESS', { updatedAt: 'v2' }), report({ keyFigures: { ...figures, total: 5 } }));

    expect(component.form.getRawValue().strengths).toBe('En cours de saisie');
    expect(component.report?.keyFigures.total).toBe(5);
  });

  it('conclusions formulées selon le type d\'audit', () => {
    show(plan('IN_PROGRESS', { type: 'CERTIFICATION' }), report());
    expect(component.conclusions[0].label).toContain('Certification');
  });

  it('le menu de conclusion garde les mêmes options d\'une détection à l\'autre', () => {
    // Des options recréées à chaque passage relancent MatSelect par microtâche,
    // donc une nouvelle détection, sans fin : l'onglet se figeait.
    show(plan('COMPLETED'), report());
    const select = fixture.debugElement.query(By.directive(MatSelect)).componentInstance as MatSelect;
    const avant = select.options.toArray();
    const liste = component.conclusions;

    fixture.detectChanges();
    fixture.detectChanges();

    expect(component.conclusions).toBe(liste);
    expect(select.options.length).toBe(3);
    select.options.forEach((o, i) => expect(o).toBe(avant[i]));
  });
});
