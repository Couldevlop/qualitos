import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ProductsService } from '../../../products/products.service';
import { RevisionRequestView } from '../../../products/products.types';
import { CapaRevisionImpactComponent } from './capa-revision-impact.component';

/**
 * L'encart « Impact PFMEA / Control Plan » de la fiche CAPA.
 *
 * <p>Il n'existe que s'il a quelque chose à dire, et son lien mène à la fiche du
 * produit concerné : quelqu'un qui vient de lire une CAPA cherche ce qu'elle a
 * fait bouger, pas la liste des produits.
 */
describe('CapaRevisionImpactComponent', () => {

  const request = (over: Partial<RevisionRequestView> = {}): RevisionRequestView => ({
    id: 'r-1', productId: 'p-9', targetType: 'PFMEA_ITEM', targetId: 'i-1',
    triggerType: 'CAPA_CLOSED', triggerRefId: 'capa-7', triggerRefLabel: 'NC-2026-0143',
    rationale: 'la cause traitée fait baisser l’occurrence 6 → 5',
    field: 'occurrence', from: '6', to: '5',
    status: 'PENDING', createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z',
    ...over
  });

  let fixture: ComponentFixture<CapaRevisionImpactComponent>;
  let component: CapaRevisionImpactComponent;
  let service: jasmine.SpyObj<ProductsService>;
  let router: { navigate: jasmine.Spy };

  beforeEach(async () => {
    service = jasmine.createSpyObj<ProductsService>('ProductsService',
      ['revisionRequestsForTrigger']);
    router = { navigate: jasmine.createSpy('navigate') };

    await TestBed.configureTestingModule({
      declarations: [CapaRevisionImpactComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ProductsService, useValue: service },
        { provide: Router, useValue: router }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaRevisionImpactComponent);
    component = fixture.componentInstance;
    component.capaId = 'capa-7';
  });

  it('n’apparaît pas quand cette CAPA n’a rien proposé', fakeAsync(() => {
    service.revisionRequestsForTrigger.and.returnValue(of([]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.visible).toBeFalse();
    expect(fixture.nativeElement.querySelector('.impact-card')).toBeNull();
  }));

  it('apparaît dès qu’une proposition est issue de cette CAPA', fakeAsync(() => {
    service.revisionRequestsForTrigger.and.returnValue(of([request()]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.visible).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('occurrence : 6 → 5');
    expect(fixture.nativeElement.textContent).toContain('la cause traitée');
  }));

  it('mène à la fiche du produit concerné, pas à la liste des produits', fakeAsync(() => {
    service.revisionRequestsForTrigger.and.returnValue(of([request()]));
    fixture.detectChanges();
    tick();

    component.open(component.requests[0]);

    expect(router.navigate).toHaveBeenCalledWith(['/products', 'p-9']);
  }));

  it('interroge le déclencheur par l’identifiant de la CAPA', fakeAsync(() => {
    service.revisionRequestsForTrigger.and.returnValue(of([]));

    fixture.detectChanges();
    tick();

    expect(service.revisionRequestsForTrigger).toHaveBeenCalledWith('capa-7');
  }));

  it('reste muet plutôt que bruyant quand l’appel échoue', fakeAsync(() => {
    service.revisionRequestsForTrigger.and.returnValue(throwError(() => ({ status: 500 })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.visible).toBeFalse();
  }));

  it('nomme une création de ligne quand la proposition n’a pas de champ', () => {
    expect(component.summary(request({ field: undefined, targetType: 'PFMEA_ITEM_CREATE' })))
      .toContain('PFMEA');
    expect(component.summary(request({ field: undefined, targetType: 'CONTROL_PLAN_LINE_CREATE' })))
      .toContain('control plan');
  });
});
