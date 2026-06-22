import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StandardsService } from '../../../standards/standards.service';
import { StandardsDocGenService } from '../../standards-doc-gen.service';
import { DossierView } from '../../standards-doc-gen.types';
import { DocGenComponent } from './doc-gen.component';

describe('DocGenComponent', () => {
  let component: DocGenComponent;
  let fixture: ComponentFixture<DocGenComponent>;
  let svc: jasmine.SpyObj<StandardsDocGenService>;
  let standards: jasmine.SpyObj<StandardsService>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const dossier: DossierView = {
    id: 'd1', tenantId: 't', standardId: 's1', standardCode: 'iso-9001',
    standardName: 'ISO 9001:2015', organizationName: 'ACME', language: 'fr',
    status: 'GENERE', aiProvider: 'ollama',
    documents: [
      { key: 'manuel-qualite', kind: 'MANUAL', label: 'Manuel', status: 'GENERE',
        normDocId: 'n1', reuseSuggestedNormDocId: null, failureReason: null, sectionCount: 7 },
      { key: 'pol', kind: 'POLICY', label: 'Politique', status: 'ECHEC',
        normDocId: null, reuseSuggestedNormDocId: null, failureReason: 'boom', sectionCount: 1 }
    ],
    totalCount: 2, generatedCount: 1, failedCount: 1, progressPercent: 100,
    integritySha256: null, integritySignature: null, anchorTxRef: null,
    finalizedAt: null, finalizedByUserId: null, createdByUserId: 'u', createdAt: 'now',
    updatedAt: 'now'
  };

  beforeEach(async () => {
    svc = jasmine.createSpyObj<StandardsDocGenService>('StandardsDocGenService',
      ['catalog', 'list', 'get', 'start', 'retry', 'finalize']);
    standards = jasmine.createSpyObj<StandardsService>('StandardsService', ['listCatalog']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    svc.catalog.and.returnValue(of([
      { key: 'manuel-qualite', kind: 'MANUAL', label: 'Manuel', status: 'EN_ATTENTE',
        normDocId: null, reuseSuggestedNormDocId: null, failureReason: null, sectionCount: 7 },
      { key: 'politique-qualite', kind: 'POLICY', label: 'Politique', status: 'EN_ATTENTE',
        normDocId: null, reuseSuggestedNormDocId: null, failureReason: null, sectionCount: 1 }
    ]));
    svc.list.and.returnValue(of([dossier]));
    standards.listCatalog.and.returnValue(of({
      content: [{ id: 's1', code: 'iso-9001', fullName: 'ISO 9001:2015',
        currentVersion: '2015', status: 'PUBLISHED' }],
      totalElements: 1, totalPages: 1, number: 0, size: 100
    }));

    await TestBed.configureTestingModule({
      declarations: [DocGenComponent],
      imports: [SharedModule, UiModule, ReactiveFormsModule, NoopAnimationsModule],
      providers: [
        { provide: StandardsDocGenService, useValue: svc },
        { provide: StandardsService, useValue: standards },
        { provide: MatSnackBar, useValue: snack },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(DocGenComponent);
    component = fixture.componentInstance;
  });

  it('loads standards, catalog and dossiers on init', () => {
    fixture.detectChanges();
    expect(component.standards.length).toBe(1);
    expect(component.catalog.length).toBe(2);
    expect(component.dossiers.length).toBe(1);
  });

  it('toggles document selection', () => {
    fixture.detectChanges();
    expect(component.isSelected('manuel-qualite')).toBeFalse();
    component.toggleKey('manuel-qualite');
    expect(component.isSelected('manuel-qualite')).toBeTrue();
    component.toggleKey('manuel-qualite');
    expect(component.isSelected('manuel-qualite')).toBeFalse();
  });

  it('does not start when the form is invalid', () => {
    fixture.detectChanges();
    component.start();
    expect(svc.start).not.toHaveBeenCalled();
  });

  it('starts generation with the tenant profile and selected keys', () => {
    svc.start.and.returnValue(of(dossier));
    fixture.detectChanges();
    component.form.patchValue({ standardId: 's1', organizationName: 'ACME', language: 'fr' });
    component.toggleKey('manuel-qualite');
    component.start();

    expect(svc.start).toHaveBeenCalled();
    const arg = svc.start.calls.mostRecent().args[0];
    expect(arg.standardId).toBe('s1');
    expect(arg.tenantProfile.organizationName).toBe('ACME');
    expect(arg.documentKeys).toEqual(['manuel-qualite']);
    expect(component.active?.id).toBe('d1');
    expect(snack.open).toHaveBeenCalled();
  });

  it('omits documentKeys when no piece is selected (full plan)', () => {
    svc.start.and.returnValue(of(dossier));
    fixture.detectChanges();
    component.form.patchValue({ standardId: 's1', organizationName: 'ACME' });
    component.start();
    const arg = svc.start.calls.mostRecent().args[0];
    expect(arg.documentKeys).toBeUndefined();
  });

  it('shows an error message when generation fails', () => {
    svc.start.and.returnValue(throwError(() => ({ status: 503 })));
    fixture.detectChanges();
    component.form.patchValue({ standardId: 's1', organizationName: 'ACME' });
    component.start();
    expect(component.error).toContain('indisponible');
  });

  it('retries failed pieces of the active dossier', () => {
    svc.retry.and.returnValue(of(dossier));
    fixture.detectChanges();
    component.active = dossier;
    component.retry();
    expect(svc.retry).toHaveBeenCalledWith('d1');
  });

  it('finalizes only with a signature', () => {
    svc.finalize.and.returnValue(of({ ...dossier, status: 'FINALISE' }));
    fixture.detectChanges();
    component.active = dossier;
    component.finalize();
    expect(svc.finalize).not.toHaveBeenCalled(); // signature manquante

    component.finalizeForm.patchValue({ signature: 'ma-signature' });
    component.finalize();
    expect(svc.finalize).toHaveBeenCalledWith('d1', { signature: 'ma-signature', notes: undefined });
    expect(component.active?.status).toBe('FINALISE');
  });

  it('exposes status helpers', () => {
    expect(component.canFinalize(dossier)).toBeTrue();
    expect(component.hasFailures(dossier)).toBeTrue();
    expect(component.dossierStatusTone('FINALISE')).toBe('success');
    expect(component.docStatusTone('GENERE')).toBe('success');
    expect(component.docStatusTone('ECHEC')).toBe('danger');
    expect(component.kindLabel('MANUAL')).toBeTruthy();
  });

  it('renders one row per dossier and per document in the active panel', () => {
    svc.start.and.returnValue(of(dossier));
    fixture.detectChanges();
    component.form.patchValue({ standardId: 's1', organizationName: 'ACME' });
    component.start();
    fixture.detectChanges();
    const docs = (fixture.nativeElement as HTMLElement).querySelectorAll('.docgen-doc');
    expect(docs.length).toBe(2);
  });
});
