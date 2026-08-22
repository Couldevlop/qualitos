import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { NcService } from '../../../nc/nc.service';
import { NcResponse } from '../../../nc/nc.types';
import { ProductNcTabComponent } from './product-nc-tab.component';

/**
 * L'onglet « NC liées ».
 *
 * <p>La séparation entre défauts expliqués et défauts inexpliqués n'est pas
 * cosmétique : les seconds sont l'information utile, et noyés dans une liste
 * unique ils ne se verraient jamais.
 */
describe('ProductNcTabComponent', () => {

  const nc = (over: Partial<NcResponse>): NcResponse => ({
    id: 'nc-1', reference: 'NC-2026-0143', title: 'Bavure sur alésage',
    category: 'PRODUCT', severity: 'MAJOR', status: 'OPEN', origin: 'INTERNAL',
    detectedAt: '2026-08-19T08:00:00Z',
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z', ...over
  });

  let fixture: ComponentFixture<ProductNcTabComponent>;
  let component: ProductNcTabComponent;
  let service: jasmine.SpyObj<NcService>;

  beforeEach(async () => {
    service = jasmine.createSpyObj<NcService>('NcService', ['listNcs']);

    await TestBed.configureTestingModule({
      declarations: [ProductNcTabComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: NcService, useValue: service },
        { provide: Router, useValue: { navigate: jasmine.createSpy('navigate') } },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductNcTabComponent);
    component = fixture.componentInstance;
    component.productId = 'p-1';
  });

  function page(content: NcResponse[]) {
    return of({ content, totalElements: content.length, totalPages: 1, number: 0, size: 100 });
  }

  it('sépare les défauts que l’analyse n’explique pas', fakeAsync(() => {
    service.listNcs.and.returnValue(page([
      nc({ id: 'explained', fmeaItemId: 'i-1' }),
      nc({ id: 'orphan-1' }),
      nc({ id: 'orphan-2' })
    ]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.explained.map(item => item.id)).toEqual(['explained']);
    expect(component.unexplained.map(item => item.id)).toEqual(['orphan-1', 'orphan-2']);
    expect(fixture.nativeElement.querySelector('.block-warning')).toBeTruthy();
  }));

  it('n’affiche pas le bloc d’alerte quand tout est rattaché', fakeAsync(() => {
    service.listNcs.and.returnValue(page([nc({ id: 'explained', fmeaItemId: 'i-1' })]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.block-warning')).toBeNull();
  }));

  it('demande la liste filtrée sur le produit, pas la liste entière', fakeAsync(() => {
    service.listNcs.and.returnValue(page([]));

    fixture.detectChanges();
    tick();

    expect(service.listNcs).toHaveBeenCalledWith(0, 100, { productId: 'p-1' });
  }));

  it('affiche un état vide explicite quand aucune NC ne cite le produit', fakeAsync(() => {
    service.listNcs.and.returnValue(page([]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.total).toBe(0);
    expect(fixture.nativeElement.querySelector('.empty')).toBeTruthy();
  }));

  it('ouvre la fiche de la non-conformité au clic', fakeAsync(() => {
    service.listNcs.and.returnValue(page([nc({ id: 'nc-9' })]));
    fixture.detectChanges();
    tick();

    component.open(component.unexplained[0]);

    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/nc', 'nc-9']);
  }));
});
