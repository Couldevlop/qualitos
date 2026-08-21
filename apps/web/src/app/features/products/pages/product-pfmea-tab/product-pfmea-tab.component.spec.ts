import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FmeaService } from '../../../fmea/fmea.service';
import { FmeaItemResponse, FmeaProjectResponse } from '../../../fmea/fmea.types';
import { ProductsService } from '../../products.service';
import { RevisionRequestView } from '../../products.types';
import { ProductPfmeaTabComponent } from './product-pfmea-tab.component';

/**
 * L'onglet PFMEA de la fiche produit.
 *
 * <p>Le tri est le point qui compte : par priorité d'action PUIS par RPN. Trier
 * par le seul RPN reproduirait le défaut que l'AP corrige, et la ligne la plus
 * grave se retrouverait sous une ligne bénigne au même produit de notes.
 */
describe('ProductPfmeaTabComponent', () => {

  const project: FmeaProjectResponse = {
    id: 'pf-1', tenantId: 't', code: 'PF-4471', name: 'Assemblage',
    type: 'PROCESS_FMEA', status: 'ACTIVE', criticalRpnThreshold: 100, revision: 2,
    createdBy: 'u', createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z'
  };

  const item = (over: Partial<FmeaItemResponse>): FmeaItemResponse => ({
    id: 'i-1', tenantId: 't', projectId: 'pf-1', sequenceNo: 10,
    severity: 5, occurrence: 5, detection: 5, rpn: 125, critical: true,
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z', ...over
  });

  let fixture: ComponentFixture<ProductPfmeaTabComponent>;
  let component: ProductPfmeaTabComponent;
  let fmea: jasmine.SpyObj<FmeaService>;
  let products: jasmine.SpyObj<ProductsService>;

  beforeEach(async () => {
    fmea = jasmine.createSpyObj<FmeaService>('FmeaService', ['list', 'listItems']);
    products = jasmine.createSpyObj<ProductsService>('ProductsService', ['revisionRequests']);

    await TestBed.configureTestingModule({
      declarations: [ProductPfmeaTabComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: FmeaService, useValue: fmea },
        { provide: ProductsService, useValue: products },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductPfmeaTabComponent);
    component = fixture.componentInstance;
    component.productId = 'p-1';
  });

  function page(content: FmeaProjectResponse[]) {
    return of({ content, totalElements: content.length, totalPages: 1, number: 0, size: 50 });
  }

  function itemsPage(content: FmeaItemResponse[]) {
    return of({ content, totalElements: content.length, totalPages: 1, number: 0, size: 100 });
  }

  it('trie par priorité d’action puis par RPN, jamais par le seul RPN', fakeAsync(() => {
    fmea.list.and.returnValue(page([project]));
    fmea.listItems.and.returnValue(itemsPage([
      item({ id: 'low', rpn: 300, actionPriority: 'LOW' }),
      item({ id: 'medium-1', rpn: 40, actionPriority: 'MEDIUM' }),
      item({ id: 'high', rpn: 12, actionPriority: 'HIGH' }),
      item({ id: 'medium-2', rpn: 90, actionPriority: 'MEDIUM' })
    ]));
    products.revisionRequests.and.returnValue(of([]));

    fixture.detectChanges();
    tick();

    expect(component.items.map(i => i.id)).toEqual(['high', 'medium-2', 'medium-1', 'low']);
  }));

  it('relègue une ligne non cotée en fin de tableau', fakeAsync(() => {
    fmea.list.and.returnValue(page([project]));
    fmea.listItems.and.returnValue(itemsPage([
      item({ id: 'unrated', rpn: 0 }),
      item({ id: 'low', rpn: 10, actionPriority: 'LOW' })
    ]));
    products.revisionRequests.and.returnValue(of([]));

    fixture.detectChanges();
    tick();

    expect(component.items.map(i => i.id)).toEqual(['low', 'unrated']);
  }));

  it('marque visiblement une ligne visée par une proposition en attente', fakeAsync(() => {
    fmea.list.and.returnValue(page([project]));
    fmea.listItems.and.returnValue(itemsPage([item({ id: 'i-1', actionPriority: 'HIGH' })]));
    products.revisionRequests.and.returnValue(of([{
      id: 'r-1', productId: 'p-1', targetType: 'PFMEA_ITEM', targetId: 'i-1',
      triggerType: 'NC_CREATED', triggerRefId: 'nc-1', triggerRefLabel: 'NC-2026-0143',
      rationale: '3 NC en 12 mois', status: 'PENDING',
      createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z'
    } as RevisionRequestView]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.isFlagged(component.items[0])).toBeTrue();
    expect(fixture.nativeElement.querySelector('tr.flagged')).toBeTruthy();
  }));

  it('affiche un état vide explicite quand aucun PFMEA ne couvre le produit', fakeAsync(() => {
    fmea.list.and.returnValue(page([]));
    products.revisionRequests.and.returnValue(of([]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.project).toBeUndefined();
    expect(fixture.nativeElement.textContent).toContain('Aucun PFMEA');
    expect(fmea.listItems).not.toHaveBeenCalled();
  }));

  it('donne le ton rouge à une priorité haute et neutre à l’absence de cotation', () => {
    expect(component.tone('HIGH')).toBe('danger');
    expect(component.tone('MEDIUM')).toBe('warning');
    expect(component.tone('LOW')).toBe('neutral');
    expect(component.tone(undefined)).toBe('neutral');
  });
});
