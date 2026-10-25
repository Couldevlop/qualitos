import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { SuppliersService } from '../../../suppliers/suppliers.service';
import { SupplierDocumentCatalogItem, SupplierResponse } from '../../../suppliers/suppliers.types';
import { SUPPLIER_DOCUMENTS_PAGE_SIZE, SupplierDocumentsLibraryComponent } from './supplier-documents-library.component';

describe('SupplierDocumentsLibraryComponent', () => {
  let fixture: ComponentFixture<SupplierDocumentsLibraryComponent>;
  let component: SupplierDocumentsLibraryComponent;
  let svc: jasmine.SpyObj<SuppliersService>;

  const supplier = { id: 's-1', code: 'ACME', name: 'Acme' } as SupplierResponse;
  const item = (id: string, over: Partial<SupplierDocumentCatalogItem> = {}): SupplierDocumentCatalogItem => ({
    id, supplierId: 's-1', ownerType: 'NON_CONFORMITY', ownerId: 'n-1', category: 'EIGHT_D',
    contentType: 'application/pdf', sizeBytes: 3 * 1024 * 1024, originalFilename: `${id}.pdf`,
    createdAt: '2026-10-20T10:00:00Z', url: `https://store/${id}`,
    supplierCode: 'ACME', supplierName: 'Acme', ownerLabel: 'PN-1', ...over
  });
  const page = (rows: SupplierDocumentCatalogItem[]) =>
    ({ content: rows, totalElements: rows.length, totalPages: 1, number: 0, size: rows.length });
  const host = (): HTMLElement => fixture.nativeElement as HTMLElement;

  beforeEach(async () => {
    svc = jasmine.createSpyObj<SuppliersService>('SuppliersService', ['list', 'listAllDocuments']);
    svc.list.and.returnValue(of({ content: [supplier], totalElements: 1, totalPages: 1, number: 0, size: 1 }));
    svc.listAllDocuments.and.returnValue(of(page([
      item('a'),
      item('b', { ownerType: 'CERTIFICATE', ownerId: 'c-1', category: 'CERTIFICATE', url: undefined,
                  originalFilename: undefined, ownerLabel: 'iso-9001 — C-77' })
    ])));
    await TestBed.configureTestingModule({
      declarations: [SupplierDocumentsLibraryComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [provideRouter([]), { provide: SuppliersService, useValue: svc }]
    }).compileComponents();
    fixture = TestBed.createComponent(SupplierDocumentsLibraryComponent);
    component = fixture.componentInstance;
  });

  function init(): void {
    fixture.detectChanges();
    tick(250);
    fixture.detectChanges();
  }

  it('liste toutes les pièces fournisseurs, sans filtre au départ', fakeAsync(() => {
    init();
    expect(svc.listAllDocuments).toHaveBeenCalledWith(
      { q: undefined, supplierId: undefined, ownerType: undefined, category: undefined }, 0, SUPPLIER_DOCUMENTS_PAGE_SIZE);
    expect(component.suppliers).toEqual([supplier]);
    expect(host().querySelectorAll('tr.mat-mdc-row').length).toBe(2);
  }));

  it('ouvre le fichier par son lien présigné, et nomme celui qui n\'en a pas', fakeAsync(() => {
    init();
    const links = host().querySelectorAll('[data-test="ouvrir"]');
    expect(links.length).toBe(1);
    expect(links[0].getAttribute('href')).toBe('https://store/a');
    expect(links[0].getAttribute('rel')).toContain('noopener');
    expect(host().textContent).toContain('Document sans nom');
  }));

  it('mène au porteur : la liste des NC du fournisseur, ou sa fiche pour un certificat', fakeAsync(() => {
    init();
    expect(component.ownerLink(component.rows[0])).toEqual(['/suppliers', 's-1', 'non-conformites']);
    expect(component.ownerLink(component.rows[1])).toEqual(['/suppliers', 's-1']);
    const porteurs = host().querySelectorAll('[data-test="porteur"]');
    expect(porteurs[0].getAttribute('href')).toBe('/suppliers/s-1/non-conformites');
    expect(porteurs[1].getAttribute('href')).toBe('/suppliers/s-1');
    expect(host().textContent).toContain('iso-9001 — C-77');
  }));

  it('donne nature, porteur, taille et icône en clair', fakeAsync(() => {
    init();
    const [nc, cert] = component.rows;
    expect(component.categoryLabel(nc)).toBe('8D fournisseur');
    expect(component.ownerType(cert)).toBe('Certificat');
    expect(component.size(nc)).toBe('3.0 Mo');
    expect(component.icon(nc)).toBe('picture_as_pdf');
    expect(component.trackById(0, nc)).toBe('a');
  }));

  it('filtre, nettoie la recherche et revient en page 0', fakeAsync(() => {
    init();
    component.onPage({ pageIndex: 3, pageSize: SUPPLIER_DOCUMENTS_PAGE_SIZE, length: 200 });
    expect(svc.listAllDocuments.calls.mostRecent().args[1]).toBe(3);

    component.filters.patchValue({ q: '  8d ', supplierId: 's-1', ownerType: 'NON_CONFORMITY', category: 'EIGHT_D' });
    tick(250);

    expect(component.hasFilters).toBeTrue();
    expect(svc.listAllDocuments.calls.mostRecent().args).toEqual(
      [{ q: '8d', supplierId: 's-1', ownerType: 'NON_CONFORMITY', category: 'EIGHT_D' }, 0, SUPPLIER_DOCUMENTS_PAGE_SIZE]);

    component.clearFilters();
    tick(250);
    expect(component.hasFilters).toBeFalse();
  }));

  it('ne relance pas la recherche pour des espaces seuls', fakeAsync(() => {
    init();
    const before = svc.listAllDocuments.calls.count();
    component.filters.patchValue({ q: '   ' });
    tick(250);
    expect(svc.listAllDocuments.calls.count()).toBe(before);
    expect(component.hasFilters).toBeFalse();
  }));

  it('redemande des liens frais sur « Actualiser »', fakeAsync(() => {
    init();
    const before = svc.listAllDocuments.calls.count();
    (host().querySelector('[data-test="actualiser"]') as HTMLButtonElement).click();
    expect(svc.listAllDocuments.calls.count()).toBe(before + 1);
  }));

  it('dit qu\'il n\'y a rien, avec ou sans filtres', fakeAsync(() => {
    svc.listAllDocuments.and.returnValue(of(page([])));
    init();
    expect(host().querySelector('[data-test="vide"]')!.textContent).toContain('encore été joint');
    component.filters.patchValue({ category: 'OTHER' });
    tick(250);
    fixture.detectChanges();
    expect(host().querySelector('[data-test="vide"]')!.textContent).toContain('ces filtres');
  }));

  it('signale un échec de chargement', fakeAsync(() => {
    svc.listAllDocuments.and.returnValue(throwError(() => ({ status: 500 })));
    svc.list.and.returnValue(throwError(() => ({ status: 500 })));
    init();
    expect(component.error).toBeTruthy();
    expect(component.rows).toEqual([]);
    expect(component.suppliers).toEqual([]);
    expect(component.loading).toBeFalse();
    expect(host().querySelector('[role="alert"]')).not.toBeNull();
  }));
});
