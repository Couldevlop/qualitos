import { SimpleChange } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { AuditsService } from '../../audits.service';
import { AuditDocument } from '../../audits.types';
import { AUDIT_DOCUMENT_MAX_BYTES, AuditDocumentsPanelComponent } from './audit-documents-panel.component';

/**
 * Section « Documents » : joindre, lister, télécharger, retirer avec
 * confirmation — et un verrou qui se dit au lieu d'échouer en silence.
 */
describe('AuditDocumentsPanelComponent', () => {
  let fixture: ComponentFixture<AuditDocumentsPanelComponent>;
  let component: AuditDocumentsPanelComponent;
  let svc: jasmine.SpyObj<AuditsService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let roles: string[];

  const doc: AuditDocument = {
    id: 'd1', planId: 'p1', contentType: 'application/pdf', sizeBytes: 1_258_291,
    originalFilename: 'convocation.pdf', createdAt: '2026-10-14T09:00:00Z', url: 'https://minio/x?sig=1'
  };

  function show(lockReason: string | null = null): void {
    component.planId = 'p1';
    component.lockReason = lockReason;
    component.ngOnChanges({ planId: new SimpleChange(undefined, 'p1', true) });
    fixture.detectChanges();
  }

  function el(test: string): HTMLElement | null {
    return fixture.nativeElement.querySelector(`[data-test="${test}"]`);
  }

  function fileInput(file: File): HTMLInputElement {
    // Un faux champ : `files` d'un vrai <input type=file> n'est pas assignable librement.
    return { files: [file] as unknown as FileList, value: 'C:\\fakepath\\x' } as HTMLInputElement;
  }

  beforeEach(async () => {
    roles = ['QUALITY_MANAGER'];
    svc = jasmine.createSpyObj<AuditsService>('AuditsService', ['listDocuments', 'uploadDocument', 'deleteDocument']);
    svc.listDocuments.and.returnValue(of([doc]));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [AuditDocumentsPanelComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [
        { provide: AuditsService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { hasAnyRole: (r: string[]) => r.some(x => roles.includes(x)) } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditDocumentsPanelComponent);
    component = fixture.componentInstance;
  });

  it('liste les documents avec un lien de téléchargement et une taille lisible', () => {
    show();
    const link = el('telecharger') as HTMLAnchorElement;
    expect(link.getAttribute('href')).toBe('https://minio/x?sig=1');
    expect(link.getAttribute('rel')).toContain('noopener');
    expect(fixture.nativeElement.textContent).toContain('1.2 Mo');
    expect(el('ajouter-document')).not.toBeNull();
  });

  it('joint un fichier puis recharge la liste', () => {
    show();
    svc.uploadDocument.and.returnValue(of(doc));
    const file = new File(['%PDF-1.7'], 'plan.pdf', { type: 'application/pdf' });
    const input = fileInput(file);

    component.choose(input);

    expect(svc.uploadDocument).toHaveBeenCalledWith('p1', file);
    expect(svc.listDocuments).toHaveBeenCalledTimes(2);
    expect(input.value).toBe('');
  });

  it('refuse avant envoi un fichier de plus de 10 Mo', () => {
    show();
    const big = new File(['x'], 'gros.pdf', { type: 'application/pdf' });
    Object.defineProperty(big, 'size', { value: AUDIT_DOCUMENT_MAX_BYTES + 1 });

    component.choose(fileInput(big));

    expect(svc.uploadDocument).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
  });

  it('traduit le refus du serveur (type non admis) en message explicite', () => {
    show();
    svc.uploadDocument.and.returnValue(throwError(() => ({ status: 400 })));

    component.choose(fileInput(new File(['MZ'], 'virus.pdf', { type: 'application/pdf' })));

    expect(snack.open.calls.mostRecent().args[0]).toContain('type non admis');
  });

  it('retire un document seulement après confirmation', () => {
    show();
    svc.deleteDocument.and.returnValue(of(void 0));
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);
    component.remove(doc);
    expect(svc.deleteDocument).not.toHaveBeenCalled();

    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    component.remove(doc);
    expect(svc.deleteDocument).toHaveBeenCalledWith('p1', 'd1');
  });

  it('verrouillé : bouton d\'ajout barré, raison affichée, pas de retrait', () => {
    show('Rapport émis : les documents de l\'audit sont figés avec lui.');
    expect((el('ajouter-document') as HTMLButtonElement).disabled).toBeTrue();
    expect(el('verrou-documents')?.textContent).toContain('Rapport émis');
    expect(el('retirer')).toBeNull();

    component.choose(fileInput(new File(['%PDF'], 'a.pdf', { type: 'application/pdf' })));
    expect(svc.uploadDocument).not.toHaveBeenCalled();
  });

  it('le retrait n\'est proposé qu\'aux rôles que le serveur autorise', () => {
    roles = ['AUDITOR'];
    show();
    expect(el('retirer')).toBeNull();
  });

  it('stockage coupé : le dit plutôt que d\'afficher une liste vide', () => {
    svc.listDocuments.and.returnValue(throwError(() => ({ status: 503 })));
    show();
    expect(component.loadError).toContain('stockage');
  });
});
