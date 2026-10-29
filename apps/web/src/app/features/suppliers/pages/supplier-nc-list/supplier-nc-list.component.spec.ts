import { HttpHeaders, HttpResponse } from '@angular/common/http';
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { SuppliersService } from '../../suppliers.service';
import { NonConformityResponse, SupplierResponse } from '../../suppliers.types';
import { SupplierDocumentsDialogComponent } from '../supplier-documents-dialog/supplier-documents-dialog.component';
import { SuppliersNcDialogComponent } from '../suppliers-nc-dialog/suppliers-nc-dialog.component';
import { NC_PAGE_SIZE, SupplierNcListComponent } from './supplier-nc-list.component';

describe('SupplierNcListComponent', () => {
  let fixture: ComponentFixture<SupplierNcListComponent>;
  let component: SupplierNcListComponent;
  let svc: jasmine.SpyObj<SuppliersService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const supplier = { id: 's-1', code: 'ACME', name: 'Acme' } as SupplierResponse;
  const nc = (id: string, over: Partial<NonConformityResponse> = {}): NonConformityResponse => ({
    id, tenantId: 't', supplierId: 's-1', supplierCode: 'ACME', supplierName: 'Acme',
    description: 'Cotes hors tolérance', severity: 'MAJOR', status: 'OPEN', detectedOn: '2026-10-20',
    partNumber: 'PN-1', partQuantity: 12, eightDRequested: false, eightDReceived: false, documentCount: 0,
    createdAt: '', updatedAt: '', ...over
  });
  const page = (rows: NonConformityResponse[], total = rows.length) =>
    ({ content: rows, totalElements: total, totalPages: 1, number: 0, size: rows.length });
  const host = (): HTMLElement => fixture.nativeElement as HTMLElement;

  async function setup(scopedId: string | null): Promise<void> {
    svc = jasmine.createSpyObj<SuppliersService>('SuppliersService', ['get', 'list', 'listAllNcs', 'updateNc', 'ncReport']);
    svc.get.and.returnValue(of(supplier));
    svc.list.and.returnValue(of({ content: [supplier], totalElements: 1, totalPages: 1, number: 0, size: 1 }));
    svc.listAllNcs.and.returnValue(of(page([nc('a'), nc('b', { status: 'RESOLVED' })])));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [SupplierNcListComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap(scopedId ? { id: scopedId } : {}) } } },
        { provide: SuppliersService, useValue: svc },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(SupplierNcListComponent);
    component = fixture.componentInstance;
  }

  function init(): void {
    fixture.detectChanges();
    tick(150);
    fixture.detectChanges();
  }

  describe('vue transverse', () => {
    beforeEach(async () => setup(null));

    it('liste toutes les NC du tenant, avec la colonne fournisseur et son filtre', fakeAsync(() => {
      init();
      expect(svc.listAllNcs).toHaveBeenCalledWith(
        { supplierId: undefined, status: undefined, severity: undefined }, 0, NC_PAGE_SIZE);
      expect(component.columns).toContain('supplier');
      expect(component.suppliers).toEqual([supplier]);
      expect(host().querySelectorAll('tr.mat-mdc-row').length).toBe(2);
      expect(host().querySelector('[data-test="filtre-fournisseur"]')).not.toBeNull();
      expect(host().querySelector('[data-test="nouvelle-nc"]')).toBeNull();
      expect(component.heading).toContain('fournisseurs');
      expect(host().textContent).toContain('2 non-conformités');
    }));

    it('filtre, revient en page 0, et efface les filtres', fakeAsync(() => {
      init();
      component.onPage({ pageIndex: 2, pageSize: NC_PAGE_SIZE, length: 100 });
      tick(150);
      expect(svc.listAllNcs.calls.mostRecent().args[1]).toBe(2);

      component.filters.patchValue({ status: 'OPEN', severity: 'CRITICAL', supplierId: 's-1' });
      tick(150);
      expect(component.hasFilters).toBeTrue();
      expect(svc.listAllNcs.calls.mostRecent().args).toEqual(
        [{ supplierId: 's-1', status: 'OPEN', severity: 'CRITICAL' }, 0, NC_PAGE_SIZE]);

      component.clearFilters();
      tick(150);
      expect(component.hasFilters).toBeFalse();
    }));

    it('dit quand aucun résultat ne correspond aux filtres', fakeAsync(() => {
      svc.listAllNcs.and.returnValue(of(page([])));
      init();
      expect(host().querySelector('[data-test="vide"]')!.textContent).toContain('Aucune non-conformité fournisseur');
      component.filters.patchValue({ status: 'OPEN' });
      tick(150);
      fixture.detectChanges();
      expect(host().querySelector('[data-test="vide"]')!.textContent).toContain('ces filtres');
    }));

    it('signale un échec de chargement et vide la table', fakeAsync(() => {
      svc.listAllNcs.and.returnValue(throwError(() => ({ status: 500 })));
      init();
      expect(component.error).toBeTruthy();
      expect(component.rows).toEqual([]);
      expect(component.loading).toBeFalse();
      expect(host().querySelector('[role="alert"]')).not.toBeNull();
    }));

    it('garde une liste de fournisseurs vide si elle ne se charge pas', fakeAsync(() => {
      svc.list.and.returnValue(throwError(() => ({ status: 500 })));
      init();
      expect(component.suppliers).toEqual([]);
    }));

    it('change le statut dans la ligne, en gardant le fournisseur affiché', fakeAsync(() => {
      init();
      const row = component.rows[0];
      svc.updateNc.and.returnValue(of(nc('a', { status: 'IN_REVIEW', supplierCode: undefined, supplierName: undefined })));

      component.setStatus(row, 'IN_REVIEW');

      expect(svc.updateNc).toHaveBeenCalledWith('s-1', 'a', { status: 'IN_REVIEW' });
      expect(component.rows[0].status).toBe('IN_REVIEW');
      expect(component.rows[0].supplierName).toBe('Acme');
      expect(component.saving.has('a')).toBeFalse();
    }));

    it('n\'envoie rien quand le statut ne change pas', fakeAsync(() => {
      init();
      component.setStatus(component.rows[0], 'OPEN');
      expect(svc.updateNc).not.toHaveBeenCalled();
    }));

    it('coche le 8D demandé, et le décoche si le serveur refuse', fakeAsync(() => {
      init();
      const row = component.rows[0];
      svc.updateNc.and.returnValue(throwError(() => ({ status: 409 })));

      component.setEightD(row, 'eightDRequested', true);

      expect(svc.updateNc).toHaveBeenCalledWith('s-1', 'a', { eightDRequested: true });
      expect(component.rows[0].eightDRequested).toBeFalse();
      expect(snack.open).toHaveBeenCalled();
    }));

    it('ignore une réponse pour une ligne qui a disparu', fakeAsync(() => {
      init();
      const ghost = nc('zz');
      svc.updateNc.and.returnValue(of(nc('zz', { eightDReceived: true })));
      component.setEightD(ghost, 'eightDReceived', true);
      expect(component.rows.map(r => r.id)).toEqual(['a', 'b']);
    }));

    it('ouvre les pièces jointes, verrouillées pour une NC close, et reprend le nouveau compte', fakeAsync(() => {
      init();
      dialog.open.and.returnValue({ afterClosed: () => of(4) } as never);
      const closed = component.rows[1];

      component.openDocuments(closed);

      const [cmp, cfg] = dialog.open.calls.mostRecent().args as [unknown, { data: { lockReason: string | null; subject: string } }];
      expect(cmp).toBe(SupplierDocumentsDialogComponent);
      expect(cfg.data.lockReason).toBeTruthy();
      expect(cfg.data.subject).toContain('ACME');
      expect(closed.documentCount).toBe(4);
    }));

    it('garde le compte si la fenêtre se ferme sans changement', fakeAsync(() => {
      init();
      dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);
      component.openDocuments(component.rows[0]);
      expect(component.rows[0].documentCount).toBe(0);
    }));

    it('télécharge la fiche PDF sous le nom donné par le serveur', fakeAsync(() => {
      init();
      const save = spyOn(HTMLAnchorElement.prototype, 'click');
      svc.ncReport.and.returnValue(of(new HttpResponse({
        body: new Blob(['%PDF'], { type: 'application/pdf' }),
        headers: new HttpHeaders({ 'Content-Disposition': 'attachment; filename="fiche.pdf"' })
      })));

      (host().querySelector('[data-test="rapport-nc"]') as HTMLButtonElement).click();

      expect(svc.ncReport).toHaveBeenCalledWith('s-1', 'a');
      expect(save).toHaveBeenCalled();
    }));

    it('signale une fiche PDF qui n\'a pas pu être générée', fakeAsync(() => {
      init();
      svc.ncReport.and.returnValue(throwError(() => ({ status: 500 })));
      component.downloadReport(component.rows[0]);
      expect(snack.open).toHaveBeenCalled();
    }));

    it('ouvre une NC pour la modifier, et recharge une fois enregistrée', fakeAsync(() => {
      init();
      const before = svc.listAllNcs.calls.count();
      dialog.open.and.returnValue({ afterClosed: () => of(component.rows[0]) } as never);
      component.edit(component.rows[0]);
      const [cmp, cfg] = dialog.open.calls.mostRecent().args as [unknown, { data: { supplierId: string; nc: unknown } }];
      expect(cmp).toBe(SuppliersNcDialogComponent);
      expect(cfg.data.nc).toBe(component.rows[0]);
      expect(cfg.data.supplierId).toBe(component.rows[0].supplierId);
      expect(svc.listAllNcs.calls.count()).toBe(before + 1);
    }));

    it('ne propose pas de créer une NC hors d\'un fournisseur', fakeAsync(() => {
      init();
      component.create();
      expect(dialog.open).not.toHaveBeenCalled();
    }));

    it('donne les classes des pastilles et un libellé accessible aux pièces jointes', fakeAsync(() => {
      init();
      expect(component.severityBadge('CRITICAL')).toBe('sev sev-critical');
      expect(component.ncStatusBadge('IN_REVIEW')).toBe('ncs ncs-in_review');
      expect(component.trackById(0, nc('q'))).toBe('q');
      expect(component.documentsAria(nc('q', { documentCount: 3 }))).toContain('3');
    }));
  });

  describe('vue d\'un fournisseur', () => {
    beforeEach(async () => setup('s-1'));

    it('ne liste que ses NC, sans colonne ni filtre fournisseur', fakeAsync(() => {
      init();
      expect(svc.get).toHaveBeenCalledWith('s-1');
      expect(svc.list).not.toHaveBeenCalled();
      expect(component.supplier).toEqual(supplier);
      expect(svc.listAllNcs.calls.mostRecent().args[0]?.supplierId).toBe('s-1');
      expect(component.columns).not.toContain('supplier');
      expect(host().querySelector('[data-test="filtre-fournisseur"]')).toBeNull();
      expect(component.heading).toContain('du fournisseur');
      expect(component.subject(component.rows[0])).not.toContain('ACME');
    }));

    it('crée une NC chez ce fournisseur et recharge', fakeAsync(() => {
      init();
      const before = svc.listAllNcs.calls.count();
      dialog.open.and.returnValue({ afterClosed: () => of(nc('new')) } as never);

      (host().querySelector('[data-test="nouvelle-nc"]') as HTMLButtonElement).click();

      const [cmp, cfg] = dialog.open.calls.mostRecent().args as [unknown, { data: { supplierId: string } }];
      expect(cmp).toBe(SuppliersNcDialogComponent);
      expect(cfg.data.supplierId).toBe('s-1');
      expect(svc.listAllNcs.calls.count()).toBe(before + 1);
    }));

    it('ne recharge pas quand la création est abandonnée', fakeAsync(() => {
      init();
      const before = svc.listAllNcs.calls.count();
      dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);
      component.create();
      expect(svc.listAllNcs.calls.count()).toBe(before);
    }));

    it('reste utilisable si la fiche du fournisseur ne se charge pas', fakeAsync(() => {
      svc.get.and.returnValue(throwError(() => ({ status: 404 })));
      init();
      expect(component.supplier).toBeNull();
      expect(component.rows.length).toBe(2);
    }));
  });
});
