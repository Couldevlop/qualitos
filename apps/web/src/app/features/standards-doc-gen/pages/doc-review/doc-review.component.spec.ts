import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StandardsDocGenService } from '../../standards-doc-gen.service';
import { NormDocView } from '../../standards-doc-gen.types';
import { DocReviewComponent } from './doc-review.component';

describe('DocReviewComponent', () => {
  let component: DocReviewComponent;
  let fixture: ComponentFixture<DocReviewComponent>;
  let svc: jasmine.SpyObj<StandardsDocGenService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const draft: NormDocView = {
    id: 'n1', tenantId: 't', standardId: 's1', standardCode: 'iso-9001',
    kind: 'MANUAL', title: 'Manuel Qualité',
    sections: [{ key: 'ctx', title: 'Contexte', clauses: ['4.1'], bodyMarkdown: 'Corps' }],
    status: 'BROUILLON_IA', aiProvider: 'ollama', markdown: '# Manuel',
    submittedAt: null, submittedByUserId: null, approvedAt: null, approvedByUserId: null,
    approvalNotes: null, humanSignature: null, rejectionReason: null,
    createdByUserId: 'u', createdAt: 'now', updatedAt: 'now'
  };

  function setup(id: string | null): void {
    svc = jasmine.createSpyObj<StandardsDocGenService>('StandardsDocGenService',
      ['getNormDoc', 'submitNormDoc', 'approveNormDoc', 'rejectNormDoc']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    svc.getNormDoc.and.returnValue(of(draft));

    TestBed.configureTestingModule({
      declarations: [DocReviewComponent],
      imports: [SharedModule, UiModule, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        { provide: StandardsDocGenService, useValue: svc },
        { provide: MatSnackBar, useValue: snack },
        { provide: ActivatedRoute, useValue: {
          snapshot: { paramMap: convertToParamMap(id ? { id } : {}) } } },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    fixture = TestBed.createComponent(DocReviewComponent);
    component = fixture.componentInstance;
  }

  it('loads the document by route id', () => {
    setup('n1');
    fixture.detectChanges();
    expect(svc.getNormDoc).toHaveBeenCalledWith('n1');
    expect(component.doc?.title).toBe('Manuel Qualité');
  });

  it('does nothing without a route id', () => {
    setup(null);
    fixture.detectChanges();
    expect(svc.getNormDoc).not.toHaveBeenCalled();
  });

  it('submits a draft for review', () => {
    setup('n1');
    svc.submitNormDoc.and.returnValue(of({ ...draft, status: 'EN_VALIDATION' }));
    fixture.detectChanges();
    component.submit();
    expect(svc.submitNormDoc).toHaveBeenCalledWith('n1');
    expect(component.doc?.status).toBe('EN_VALIDATION');
    expect(snack.open).toHaveBeenCalled();
  });

  it('requires a signature to approve', () => {
    setup('n1');
    svc.approveNormDoc.and.returnValue(of({ ...draft, status: 'APPROUVE' }));
    fixture.detectChanges();
    component.doc = { ...draft, status: 'EN_VALIDATION' };
    component.approve();
    expect(svc.approveNormDoc).not.toHaveBeenCalled(); // signature manquante

    component.approveForm.patchValue({ signature: 'sig' });
    component.approve();
    expect(svc.approveNormDoc).toHaveBeenCalledWith('n1', { signature: 'sig', notes: undefined });
  });

  it('requires a reason to reject', () => {
    setup('n1');
    svc.rejectNormDoc.and.returnValue(of({ ...draft, status: 'BROUILLON_IA' }));
    fixture.detectChanges();
    component.doc = { ...draft, status: 'EN_VALIDATION' };
    component.reject();
    expect(svc.rejectNormDoc).not.toHaveBeenCalled();

    component.rejectForm.patchValue({ reason: 'sections vides' });
    component.reject();
    expect(svc.rejectNormDoc).toHaveBeenCalledWith('n1', { reason: 'sections vides' });
  });

  it('maps a 403 to a forbidden message', () => {
    setup('n1');
    fixture.detectChanges();
    svc.submitNormDoc.and.returnValue(throwError(() => ({ status: 403 })));
    component.submit();
    expect(component.error).toContain('insuffisants');
  });

  it('exposes status/kind label helpers', () => {
    setup('n1');
    expect(component.statusLabel('APPROUVE')).toBeTruthy();
    expect(component.statusTone('APPROUVE')).toBe('success');
    expect(component.statusTone('REJETE')).toBe('danger');
    expect(component.kindLabel('PROCEDURE')).toBeTruthy();
  });
});
