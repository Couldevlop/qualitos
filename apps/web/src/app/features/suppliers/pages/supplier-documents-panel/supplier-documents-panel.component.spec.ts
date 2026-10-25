import { SimpleChange } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { SuppliersService } from '../../suppliers.service';
import { SupplierDocument, SupplierDocumentOwnerType } from '../../suppliers.types';
import { SUPPLIER_DOCUMENT_MAX_BYTES, SupplierDocumentsPanelComponent } from './supplier-documents-panel.component';

describe('SupplierDocumentsPanelComponent', () => {
  let fixture: ComponentFixture<SupplierDocumentsPanelComponent>;
  let component: SupplierDocumentsPanelComponent;
  let svc: jasmine.SpyObj<SuppliersService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let manager: boolean;

  const doc = (id: string, over: Partial<SupplierDocument> = {}): SupplierDocument => ({
    id, supplierId: 's-1', ownerType: 'NON_CONFORMITY', ownerId: 'n-1', category: 'EIGHT_D',
    contentType: 'application/pdf', sizeBytes: 2048, originalFilename: `${id}.pdf`,
    createdAt: '2026-10-20T10:00:00Z', url: `https://store/${id}`, ...over
  });
  const host = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const q = (sel: string) => host().querySelector(sel) as HTMLElement | null;

  async function setup(): Promise<void> {
    svc = jasmine.createSpyObj<SuppliersService>('SuppliersService', ['listDocuments', 'uploadDocument', 'deleteDocument']);
    svc.listDocuments.and.returnValue(of([doc('a')]));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    const auth = { hasAnyRole: jasmine.createSpy('hasAnyRole').and.callFake(() => manager) };
    await TestBed.configureTestingModule({
      declarations: [SupplierDocumentsPanelComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: SuppliersService, useValue: svc },
        { provide: AuthService, useValue: auth },
        { provide: MatSnackBar, useValue: snack },
        { provide: MatDialog, useValue: dialog }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(SupplierDocumentsPanelComponent);
    component = fixture.componentInstance;
  }

  function bind(ownerType: SupplierDocumentOwnerType = 'NON_CONFORMITY', lockReason: string | null = null): void {
    component.supplierId = 's-1';
    component.ownerType = ownerType;
    component.ownerId = 'n-1';
    component.lockReason = lockReason;
    component.ngOnChanges({
      supplierId: new SimpleChange(undefined, 's-1', true),
      ownerType: new SimpleChange(undefined, ownerType, true),
      ownerId: new SimpleChange(undefined, 'n-1', true)
    });
    fixture.detectChanges();
  }

  function pick(file: File | null): HTMLInputElement {
    const input = q('[data-test="saisie-fichier"]') as HTMLInputElement;
    Object.defineProperty(input, 'files', { value: file ? [file] : [], configurable: true });
    component.choose(input);
    return input;
  }

  beforeEach(async () => {
    manager = true;
    await setup();
  });

  it('charge les documents du porteur et propose les natures d\'une NC, le 8D en tête', () => {
    bind();
    expect(svc.listDocuments).toHaveBeenCalledWith('s-1', 'NON_CONFORMITY', 'n-1');
    expect(component.categories.map(c => c.value)).toEqual(['EIGHT_D', 'CAPA_EVIDENCE', 'OTHER']);
    expect(component.category).toBe('EIGHT_D');
    expect(host().querySelectorAll('[data-test="document"]').length).toBe(1);
    expect(q('[data-test="telecharger"]')!.getAttribute('href')).toBe('https://store/a');
  });

  it('propose à un certificat le certificat puis « autre »', () => {
    bind('CERTIFICATE');
    expect(component.categories.map(c => c.value)).toEqual(['CERTIFICATE', 'OTHER']);
    expect(component.category).toBe('CERTIFICATE');
  });

  it('dit qu\'il n\'y a rien quand la liste est vide', () => {
    svc.listDocuments.and.returnValue(of([]));
    bind();
    expect(host().textContent).toContain('Aucun document joint');
  });

  it('joint le fichier choisi avec sa nature, puis recharge et remonte le compte', () => {
    bind();
    svc.uploadDocument.and.returnValue(of(doc('b')));
    svc.listDocuments.and.returnValue(of([doc('a'), doc('b')]));
    const emitted: number[] = [];
    component.changed.subscribe(n => emitted.push(n));
    component.category = 'CAPA_EVIDENCE';
    const file = new File(['x'], 'preuve.pdf', { type: 'application/pdf' });

    const input = pick(file);

    expect(svc.uploadDocument).toHaveBeenCalledWith('s-1', 'NON_CONFORMITY', 'n-1', file, 'CAPA_EVIDENCE');
    expect(emitted).toEqual([2]);
    expect(component.uploading).toBeFalse();
    expect(input.value).toBe('');
    expect(snack.open).toHaveBeenCalled();
  });

  it('refuse sans l\'envoyer un fichier au-delà de 10 Mo', () => {
    bind();
    const big = new File(['x'], 'gros.pdf', { type: 'application/pdf' });
    Object.defineProperty(big, 'size', { value: SUPPLIER_DOCUMENT_MAX_BYTES + 1 });
    pick(big);
    expect(svc.uploadDocument).not.toHaveBeenCalled();
    expect(snack.open.calls.mostRecent().args[0]).toContain('10 Mo');
  });

  it('ne fait rien sans fichier choisi', () => {
    bind();
    pick(null);
    expect(svc.uploadDocument).not.toHaveBeenCalled();
  });

  [
    { status: 400, text: 'type non admis' },
    { status: 413, text: 'Limite atteinte' },
    { status: 409, text: 'verrouillés' }
  ].forEach(({ status, text }) => {
    it(`traduit un refus ${status} du serveur`, () => {
      bind();
      svc.uploadDocument.and.returnValue(throwError(() => ({ status })));
      pick(new File(['x'], 'a.pdf', { type: 'application/pdf' }));
      expect(snack.open.calls.mostRecent().args[0]).toContain(text);
      expect(component.uploading).toBeFalse();
    });
  });

  it('verrouille l\'ajout et le retrait d\'une NC close, et dit pourquoi', () => {
    bind('NON_CONFORMITY', 'NC close');
    expect((q('[data-test="ajouter-document"]') as HTMLButtonElement).disabled).toBeTrue();
    expect(q('[data-test="verrou-documents"]')!.textContent).toContain('NC close');
    expect(q('[data-test="retirer"]')).toBeNull();
    pick(new File(['x'], 'a.pdf'));
    expect(svc.uploadDocument).not.toHaveBeenCalled();
  });

  it('ne propose pas le retrait à qui n\'a pas le droit de supprimer', () => {
    manager = false;
    bind();
    expect(component.canRemove).toBeFalse();
    expect(q('[data-test="retirer"]')).toBeNull();
  });

  it('retire un document après confirmation, puis remonte le compte', () => {
    bind();
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    svc.deleteDocument.and.returnValue(of(undefined));
    svc.listDocuments.and.returnValue(of([]));
    const emitted: number[] = [];
    component.changed.subscribe(n => emitted.push(n));

    (q('[data-test="retirer"]') as HTMLButtonElement).click();

    expect(svc.deleteDocument).toHaveBeenCalledWith('s-1', 'NON_CONFORMITY', 'n-1', 'a');
    expect(emitted).toEqual([0]);
  });

  it('ne retire rien si la confirmation est refusée', () => {
    bind();
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);
    component.remove(doc('a', { originalFilename: undefined }));
    expect(svc.deleteDocument).not.toHaveBeenCalled();
  });

  it('signale l\'échec d\'un retrait', () => {
    bind();
    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    svc.deleteDocument.and.returnValue(throwError(() => ({ status: 500 })));
    component.remove(doc('a'));
    expect(snack.open).toHaveBeenCalled();
  });

  it('dit que le stockage est éteint sur un 503, et signale les autres échecs de lecture', () => {
    svc.listDocuments.and.returnValue(throwError(() => ({ status: 503 })));
    bind();
    expect(component.loadError).toContain('stockage');

    svc.listDocuments.and.returnValue(throwError(() => ({ status: 500 })));
    component.ngOnChanges({ ownerId: new SimpleChange('n-1', 'n-1', false) });
    expect(component.loadError).toBeTruthy();
    expect(component.loadError).not.toContain('stockage');
  });

  it('nomme le document dans le libellé accessible du retrait', () => {
    bind();
    expect(component.removeAria(doc('a'))).toContain('a.pdf');
    expect(component.icon(doc('i', { contentType: 'image/png' }))).toBe('image');
    expect(component.size(2048)).toBe('2 Ko');
    expect(component.categoryLabel('EIGHT_D')).toBe('8D fournisseur');
    expect(component.trackById(0, doc('z'))).toBe('z');
  });
});
