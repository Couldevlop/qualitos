import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ProductsService } from '../../products.service';
import { ProductResponse, RevisionRequestView } from '../../products.types';
import { ProductsListComponent } from './products-list.component';

/**
 * La liste des produits.
 *
 * <p>Ce qu'elle doit garantir : que la colonne « À réviser » ne crie que quand il
 * y a quelque chose à voir, qu'un produit obsolète se distingue et ne s'édite
 * plus, et que filtrer ne relance pas la moindre requête.
 */
describe('ProductsListComponent', () => {

  const product = (over: Partial<ProductResponse> = {}): ProductResponse => ({
    id: 'p-1', code: 'REF-4471', designation: 'Support moteur',
    status: 'ACTIVE', createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z',
    ...over
  });

  const revision = (id: string): RevisionRequestView => ({
    id, productId: 'p-1', targetType: 'PFMEA_ITEM', targetId: 'i-1',
    triggerType: 'NC_CREATED', triggerRefId: 'nc-1', triggerRefLabel: 'NC-2026-0143',
    rationale: '3 NC en 12 mois', status: 'PENDING',
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z'
  });

  let fixture: ComponentFixture<ProductsListComponent>;
  let component: ProductsListComponent;
  let service: jasmine.SpyObj<ProductsService>;
  let dialog: { open: jasmine.Spy };
  let closed: unknown;
  let router: { navigate: jasmine.Spy };
  let snack: { open: jasmine.Spy };

  beforeEach(async () => {
    service = jasmine.createSpyObj<ProductsService>('ProductsService', ['list', 'revisionRequests']);
    closed = null;
    dialog = { open: jasmine.createSpy('open').and.callFake(() => ({ afterClosed: () => of(closed) })) };
    router = { navigate: jasmine.createSpy('navigate') };
    snack = { open: jasmine.createSpy('open') };
    await TestBed.configureTestingModule({
      declarations: [ProductsListComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        { provide: ProductsService, useValue: service },
        { provide: MatDialog, useValue: dialog },
        { provide: Router, useValue: router },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductsListComponent);
    component = fixture.componentInstance;
  });

  it('n’affiche aucun badge « à réviser » quand le compteur vaut zéro', fakeAsync(() => {
    // Un badge « 0 » attire l'œil pour rien, et l'œil finit par ne plus voir les autres.
    service.list.and.returnValue(of([product()]));
    service.revisionRequests.and.returnValue(of([]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.rows[0].pendingRevisions).toBe(0);
    const badges = fixture.nativeElement.querySelectorAll('.qos-badge[data-tone="warning"]');
    expect(badges.length).toBe(0);
  }));

  it('affiche le compte quand des révisions attendent', fakeAsync(() => {
    service.list.and.returnValue(of([product()]));
    service.revisionRequests.and.returnValue(of([revision('r-1'), revision('r-2')]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.rows[0].pendingRevisions).toBe(2);
    const badge = fixture.nativeElement.querySelector('.qos-badge[data-tone="warning"]');
    expect(badge.textContent.trim()).toBe('2');
  }));

  it('distingue un produit obsolète et lui retire son bouton d’édition', fakeAsync(() => {
    service.list.and.returnValue(of([product({ status: 'OBSOLETE' })]));
    service.revisionRequests.and.returnValue(of([]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.isEditable(component.rows[0])).toBeFalse();
    expect(fixture.nativeElement.querySelector('.product-row.obsolete')).toBeTruthy();
    expect(fixture.nativeElement.querySelectorAll('button[mat-icon-button]').length).toBe(0);
  }));

  it('filtre sans émettre la moindre requête supplémentaire', fakeAsync(() => {
    service.list.and.returnValue(of([product(), product({ id: 'p-2', status: 'DRAFT' })]));
    service.revisionRequests.and.returnValue(of([]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();
    const callsBefore = service.list.calls.count();

    component.statusFilter = 'DRAFT';
    fixture.detectChanges();

    expect(component.visibleRows.length).toBe(1);
    expect(component.visibleRows[0].product.id).toBe('p-2');
    expect(service.list.calls.count()).toBe(callsBefore);
  }));

  it('reste utilisable quand le compteur de révisions tombe', fakeAsync(() => {
    service.list.and.returnValue(of([product()]));
    service.revisionRequests.and.returnValue(throwError(() => ({ status: 500 })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.rows.length).toBe(1);
    expect(component.rows[0].pendingRevisions).toBe(0);
  }));

  it('ne demande aucun compteur quand il n’y a aucun produit', fakeAsync(() => {
    service.list.and.returnValue(of([]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.rows).toEqual([]);
    expect(service.revisionRequests).not.toHaveBeenCalled();
    expect(fixture.nativeElement.querySelector('.empty')).toBeTruthy();
  }));

  it('ouvre la fiche du produit qu’on désigne', fakeAsync(() => {
    service.list.and.returnValue(of([product()]));
    service.revisionRequests.and.returnValue(of([]));
    fixture.detectChanges();
    tick();

    component.open(component.rows[0]);

    expect(router.navigate).toHaveBeenCalledWith(['/products', 'p-1']);
  }));

  it('ne recharge rien quand la création est abandonnée', fakeAsync(() => {
    service.list.and.returnValue(of([product()]));
    service.revisionRequests.and.returnValue(of([]));
    fixture.detectChanges();
    tick();
    service.list.calls.reset();

    component.create();
    tick();

    expect(dialog.open).toHaveBeenCalled();
    expect(service.list).not.toHaveBeenCalled();
  }));

  it('recharge la liste après une création', fakeAsync(() => {
    service.list.and.returnValue(of([product()]));
    service.revisionRequests.and.returnValue(of([]));
    fixture.detectChanges();
    tick();
    service.list.calls.reset();
    closed = product({ id: 'p-2' });

    component.create();
    tick();

    expect(service.list).toHaveBeenCalledTimes(1);
  }));

  it('ouvre l’édition sans ouvrir la fiche : le clic ne traverse pas la ligne', fakeAsync(() => {
    service.list.and.returnValue(of([product()]));
    service.revisionRequests.and.returnValue(of([]));
    fixture.detectChanges();
    tick();
    const event = new MouseEvent('click');
    spyOn(event, 'stopPropagation');

    component.edit(component.rows[0], event);
    tick();

    expect(event.stopPropagation).toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(dialog.open.calls.mostRecent().args[1].data.product.id).toBe('p-1');
  }));

  it('dit que la liste n’a pas pu être chargée, et cesse de faire patienter', fakeAsync(() => {
    service.list.and.returnValue(throwError(() => ({ status: 500 })));

    fixture.detectChanges();
    tick();

    expect(component.loading).toBeFalse();
    expect(snack.open.calls.mostRecent().args[0]).toContain('Impossible de charger');
  }));
});
