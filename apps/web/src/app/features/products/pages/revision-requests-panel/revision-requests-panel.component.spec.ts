import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ProductsService } from '../../products.service';
import { RevisionRequestView } from '../../products.types';
import { RevisionRequestsPanelComponent } from './revision-requests-panel.component';

/**
 * Le panneau des révisions proposées.
 *
 * <p>Deux garanties : la justification est lisible en clair — une proposition
 * qu'on ne peut pas contester ne se confirme pas en conscience — et le refus
 * reste impossible tant qu'aucun motif n'est écrit.
 */
describe('RevisionRequestsPanelComponent', () => {

  const request = (over: Partial<RevisionRequestView> = {}): RevisionRequestView => ({
    id: 'r-1', productId: 'p-1', targetType: 'PFMEA_ITEM', targetId: 'i-1',
    triggerType: 'NC_CREATED', triggerRefId: 'nc-1', triggerRefLabel: 'NC-2026-0143',
    rationale: '3 NC en 12 mois sur ce mode de défaillance — occurrence 4 → 6',
    field: 'occurrence', from: '4', to: '6',
    status: 'PENDING', createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z',
    ...over
  });

  let fixture: ComponentFixture<RevisionRequestsPanelComponent>;
  let component: RevisionRequestsPanelComponent;
  let service: jasmine.SpyObj<ProductsService>;

  beforeEach(async () => {
    service = jasmine.createSpyObj<ProductsService>('ProductsService',
      ['revisionRequests', 'acceptRevision', 'rejectRevision']);

    await TestBed.configureTestingModule({
      declarations: [RevisionRequestsPanelComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        { provide: ProductsService, useValue: service },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RevisionRequestsPanelComponent);
    component = fixture.componentInstance;
    component.productId = 'p-1';
  });

  it('affiche la justification en clair', fakeAsync(() => {
    service.revisionRequests.and.returnValue(of([request()]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const text = fixture.nativeElement.querySelector('.rationale').textContent;
    expect(text).toContain('3 NC en 12 mois');
    expect(text).toContain('occurrence 4 → 6');
  }));

  it('laisse le bouton de refus désactivé tant que le motif est vide', fakeAsync(() => {
    service.revisionRequests.and.returnValue(of([request()]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.canReject(component.requests[0])).toBeFalse();

    component.notes['r-1'] = '   ';
    expect(component.canReject(component.requests[0])).toBeFalse();

    component.notes['r-1'] = 'Cotation revue le 12/08';
    expect(component.canReject(component.requests[0])).toBeTrue();
  }));

  it('ne refuse rien quand le motif est vide, même si on force l’appel', fakeAsync(() => {
    service.revisionRequests.and.returnValue(of([request()]));
    fixture.detectChanges();
    tick();

    component.reject(component.requests[0]);

    expect(service.rejectRevision).not.toHaveBeenCalled();
  }));

  it('transmet le motif détouré des espaces au refus', fakeAsync(() => {
    service.revisionRequests.and.returnValue(of([request()]));
    service.rejectRevision.and.returnValue(of(request({ status: 'REJECTED' })));
    fixture.detectChanges();
    tick();

    component.notes['r-1'] = '  Cotation revue le 12/08  ';
    component.reject(component.requests[0]);
    tick();

    expect(service.rejectRevision).toHaveBeenCalledWith('r-1', 'Cotation revue le 12/08');
  }));

  it('prévient le parent après une décision pour qu’il rafraîchisse son compteur', fakeAsync(() => {
    service.revisionRequests.and.returnValue(of([request()]));
    service.acceptRevision.and.returnValue(of(request({ status: 'ACCEPTED' })));
    const decided = jasmine.createSpy('decided');
    component.decided.subscribe(decided);
    fixture.detectChanges();
    tick();

    component.accept(component.requests[0]);
    tick();

    expect(decided).toHaveBeenCalled();
  }));

  it('nomme la création d’une ligne quand la proposition n’a pas de champ', () => {
    expect(component.summary(request({ field: undefined, targetType: 'PFMEA_ITEM_CREATE' })))
      .toContain('PFMEA');
    expect(component.summary(request({ field: undefined, targetType: 'CONTROL_PLAN_LINE_CREATE' })))
      .toContain('control plan');
    expect(component.summary(request())).toBe('occurrence : 4 → 6');
  });

  it('affiche un état vide explicite plutôt qu’un panneau muet', fakeAsync(() => {
    service.revisionRequests.and.returnValue(of([]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.empty')).toBeTruthy();
  }));
});
