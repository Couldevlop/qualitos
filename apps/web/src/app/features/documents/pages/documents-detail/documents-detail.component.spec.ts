import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { AuthzService } from '../../../../core/authz/authz.service';
import { CircuitService } from '../../../../core/circuits/circuit.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DocumentCircuitComponent } from '../../components/document-circuit/document-circuit.component';
import { DocumentsService } from '../../documents.service';
import { DocumentResponse, DocumentVersionResponse } from '../../documents.types';
import { DocumentsRejectDialogComponent } from '../documents-reject-dialog/documents-reject-dialog.component';
import { DocumentsDetailComponent } from './documents-detail.component';

describe('DocumentsDetailComponent — revue et circuit', () => {

  let fixture: ComponentFixture<DocumentsDetailComponent>;
  let component: DocumentsDetailComponent;
  let svc: jasmine.SpyObj<DocumentsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const version = (o: Partial<DocumentVersionResponse>): DocumentVersionResponse => ({
    id: 'v1', documentId: 'doc-1', versionNumber: 1, status: 'PUBLISHED', authorId: 'autrice',
    createdAt: '', updatedAt: '', ...o
  });

  const DOC: DocumentResponse = {
    id: 'doc-1', tenantId: 't', code: 'PRO-001', title: 'Procédure', type: 'PROCEDURE', status: 'ACTIVE',
    ownerId: 'o', mandatoryRead: false, createdAt: '', updatedAt: '',
    versions: [
      version({ id: 'v1', versionNumber: 1, status: 'PUBLISHED' }),
      version({ id: 'v2', versionNumber: 2, status: 'IN_REVIEW' })
    ]
  } as DocumentResponse;

  beforeEach(async () => {
    svc = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['get', 'approve']);
    svc.get.and.returnValue(of(DOC));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    const circuits = jasmine.createSpyObj<CircuitService>('CircuitService', ['run']);
    circuits.run.and.returnValue(of(null));
    const authz = jasmine.createSpyObj<AuthzService>('AuthzService', ['me', 'can']);
    authz.me.and.returnValue(of({ userId: 'chloe', roles: ['QUALITY_MANAGER'], permissions: ['*'] }));
    authz.can.and.returnValue(of(true));

    await TestBed.configureTestingModule({
      declarations: [DocumentsDetailComponent, DocumentCircuitComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 'doc-1' })) } },
        { provide: DocumentsService, useValue: svc },
        { provide: CircuitService, useValue: circuits },
        { provide: AuthzService, useValue: authz },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'chloe' }) } },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DocumentsDetailComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('met en tête la version en revue, avec son circuit', () => {
    expect(component.attention(DOC).map(v => v.id)).toEqual(['v2']);
    expect(hote().querySelectorAll('qos-document-circuit').length).toBe(1);
  });

  it('un brouillon revenu d’un refus demande aussi l’attention ; un brouillon neuf, non', () => {
    const d = { ...DOC, versions: [
      version({ id: 'a', status: 'DRAFT', rejectionReason: 'Incomplet' }),
      version({ id: 'b', status: 'DRAFT' })
    ] } as DocumentResponse;
    expect(component.attention(d).map(v => v.id)).toEqual(['a']);
    expect(component.trackById(0, d.versions[0])).toBe('a');
  });

  it('refuser ouvre le dialogue motivé et recharge après un refus', () => {
    dialog.open.and.returnValue({ afterClosed: () => of(version({ status: 'DRAFT' })) } as never);
    svc.get.calls.reset();

    (hote().querySelector('[data-test="refuser"]') as HTMLButtonElement).click();

    expect(dialog.open).toHaveBeenCalledWith(DocumentsRejectDialogComponent, jasmine.objectContaining({
      data: { version: DOC.versions[1] }
    }));
    expect(svc.get).toHaveBeenCalled();
  });

  it('une approbation qui ne franchit qu’une étape le dit', () => {
    svc.approve.and.returnValue(of(version({ status: 'IN_REVIEW' })));
    component.approve(DOC.versions[1]);
    expect(snack.open.calls.mostRecent().args[0]).toContain('la validation continue');

    svc.approve.and.returnValue(of(version({ status: 'APPROVED' })));
    component.approve(DOC.versions[1]);
    expect(snack.open.calls.mostRecent().args[0]).toContain('Version approuvée');
  });
});
