import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { FmeaService } from '../../../fmea/fmea.service';
import { FmeaItemResponse, FmeaProjectResponse } from '../../../fmea/fmea.types';
import { ProductsService } from '../../../products/products.service';
import { ProductResponse } from '../../../products/products.types';
import { NcService } from '../../nc.service';
import { NcResponse } from '../../nc.types';
import { NcCreateDialogComponent } from './nc-create-dialog.component';

/**
 * Le rattachement d'une non-conformité à son produit et à son mode de défaillance.
 *
 * <p>Trois garanties : la question du mode de défaillance ne se pose qu'une fois
 * le produit choisi, la suggestion affiche les termes qui l'ont motivée — sans
 * quoi elle n'est ni contestable ni confirmable en conscience — et « aucun mode
 * ne correspond » est un choix explicite, pas l'absence de choix.
 */
describe('NcCreateDialogComponent — produit et mode de défaillance', () => {

  const product: ProductResponse = {
    id: 'p-1', code: 'REF-4471', designation: 'Support moteur', status: 'ACTIVE',
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z'
  };

  const project: FmeaProjectResponse = {
    id: 'pf-1', tenantId: 't', code: 'PF-4471', name: 'Assemblage',
    type: 'PROCESS_FMEA', status: 'ACTIVE', criticalRpnThreshold: 100, revision: 1,
    createdBy: 'u', createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z'
  };

  const item: FmeaItemResponse = {
    id: 'i-1', tenantId: 't', projectId: 'pf-1', sequenceNo: 10,
    failureMode: 'Bavure sur alésage', failureEffect: 'Montage impossible',
    severity: 7, occurrence: 4, detection: 5, rpn: 140, critical: true,
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z'
  };

  const created: NcResponse = {
    id: 'nc-1', reference: 'NC-2026-0143', title: 'Bavure',
    category: 'PRODUCT', severity: 'MAJOR', status: 'OPEN', origin: 'INTERNAL',
    detectedAt: '2026-08-19T08:00:00Z',
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z'
  };

  let fixture: ComponentFixture<NcCreateDialogComponent>;
  let component: NcCreateDialogComponent;
  let products: jasmine.SpyObj<ProductsService>;
  let fmea: jasmine.SpyObj<FmeaService>;
  let nc: jasmine.SpyObj<NcService>;

  beforeEach(async () => {
    products = jasmine.createSpyObj<ProductsService>('ProductsService',
      ['list', 'failureModeSuggestions']);
    fmea = jasmine.createSpyObj<FmeaService>('FmeaService', ['list', 'listItems']);
    nc = jasmine.createSpyObj<NcService>('NcService', ['createNc']);

    products.list.and.returnValue(of([product]));
    fmea.list.and.returnValue(of({
      content: [project], totalElements: 1, totalPages: 1, number: 0, size: 50
    }));
    fmea.listItems.and.returnValue(of({
      content: [item], totalElements: 1, totalPages: 1, number: 0, size: 100
    }));

    await TestBed.configureTestingModule({
      declarations: [NcCreateDialogComponent],
      imports: [SharedModule, UiModule, MatRadioModule, NoopAnimationsModule],
      providers: [
        { provide: ProductsService, useValue: products },
        { provide: FmeaService, useValue: fmea },
        { provide: NcService, useValue: nc },
        { provide: AuthService, useValue: { snapshot: () => ({ userId: 'u-1' }) } },
        { provide: MatSnackBar, useValue: { open: jasmine.createSpy('open') } },
        { provide: MatDialogRef, useValue: { close: jasmine.createSpy('close') } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NcCreateDialogComponent);
    component = fixture.componentInstance;
  });

  it('ne pose la question du mode de défaillance qu’après le choix d’un produit', fakeAsync(() => {
    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.productChosen).toBeFalse();
    expect(fixture.nativeElement.querySelector('.failure-mode')).toBeNull();

    component.form.patchValue({ productId: 'p-1' });
    fixture.detectChanges();

    expect(component.productChosen).toBeTrue();
    expect(fixture.nativeElement.querySelector('.failure-mode')).toBeTruthy();
  }));

  it('affiche les termes qui ont motivé la suggestion', fakeAsync(() => {
    products.failureModeSuggestions.and.returnValue(of([
      { fmeaItemId: 'i-1', score: 0.5, matchedTerms: 'alesage, bavure' }
    ]));
    fixture.detectChanges();
    tick();

    component.form.patchValue({ productId: 'p-1', title: 'Bavure sur alésage' });
    component.refreshSuggestions();
    tick();
    fixture.detectChanges();

    expect(component.suggestions).toEqual([
      { fmeaItemId: 'i-1', label: 'Bavure sur alésage', matchedTerms: 'alesage, bavure' }
    ]);
    expect(fixture.nativeElement.textContent).toContain('alesage, bavure');
  }));

  it('interroge le serveur avec le titre ET la description', fakeAsync(() => {
    products.failureModeSuggestions.and.returnValue(of([]));
    fixture.detectChanges();
    tick();

    component.form.patchValue({
      productId: 'p-1', title: 'Bavure', description: 'constatée au poste 20'
    });
    component.refreshSuggestions();
    tick();

    expect(products.failureModeSuggestions)
      .toHaveBeenCalledWith('p-1', 'Bavure constatée au poste 20');
  }));

  it('ne demande rien tant que le défaut n’est pas décrit', fakeAsync(() => {
    fixture.detectChanges();
    tick();

    component.form.patchValue({ productId: 'p-1', title: '' });
    component.refreshSuggestions();
    tick();

    expect(products.failureModeSuggestions).not.toHaveBeenCalled();
  }));

  it('transmet « aucun mode » comme un choix, pas comme un champ vide', fakeAsync(() => {
    nc.createNc.and.returnValue(of(created));
    fixture.detectChanges();
    tick();

    component.form.patchValue({
      title: 'Bavure', productId: 'p-1', failureMode: component.noFailureMode
    });
    component.submit();
    tick();

    const payload = nc.createNc.calls.mostRecent().args[0];
    expect(payload.productId).toBe('p-1');
    // Le serveur reçoit un produit sans mode de défaillance : c'est ce qui
    // déclenchera la proposition de créer une ligne de PFMEA.
    expect(payload.fmeaItemId).toBeUndefined();
  }));

  it('transmet le mode de défaillance confirmé', fakeAsync(() => {
    nc.createNc.and.returnValue(of(created));
    fixture.detectChanges();
    tick();

    component.form.patchValue({ title: 'Bavure', productId: 'p-1', failureMode: 'i-1' });
    component.submit();
    tick();

    expect(nc.createNc.calls.mostRecent().args[0].fmeaItemId).toBe('i-1');
  }));

  it('efface la réponse précédente quand on relance une recherche', fakeAsync(() => {
    products.failureModeSuggestions.and.returnValue(of([]));
    fixture.detectChanges();
    tick();

    component.form.patchValue({ productId: 'p-1', title: 'Bavure', failureMode: 'i-1' });
    component.refreshSuggestions();
    tick();

    expect(component.form.getRawValue().failureMode).toBe('');
  }));

  it('reste utilisable quand le référentiel produit est indisponible', fakeAsync(() => {
    products.list.and.returnValue(of([]));

    fixture.detectChanges();
    tick();

    expect(component.products).toEqual([]);
    expect(component.form.valid).toBeFalse();
  }));
});
