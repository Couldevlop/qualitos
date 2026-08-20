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

  beforeEach(async () => {
    service = jasmine.createSpyObj<ProductsService>('ProductsService', ['list', 'revisionRequests']);
    await TestBed.configureTestingModule({
      declarations: [ProductsListComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        { provide: ProductsService, useValue: service },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(null) }) } },
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } }
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
});
