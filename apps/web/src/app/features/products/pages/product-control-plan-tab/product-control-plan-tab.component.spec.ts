import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatMenuModule } from '@angular/material/menu';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, Subject, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ProductsService } from '../../products.service';
import { ControlPlanLineView, ControlPlanView } from '../../products.types';
import { ProductControlPlanTabComponent } from './product-control-plan-tab.component';

/**
 * L'onglet Control Plan.
 *
 * <p>Deux points de vigilance : une ligne sans lien vers le PFMEA doit le dire —
 * un contrôle sans raison d'être coûte du temps au poste sans réduire aucun
 * risque — et le bouton « Approuver » ne s'affiche pas pour qui n'y a pas droit.
 * L'autorité reste le serveur ; montrer un bouton qui répondra 403 est seulement
 * une mauvaise expérience.
 */
describe('ProductControlPlanTabComponent', () => {

  const plan = (over: Partial<ControlPlanView> = {}): ControlPlanView => ({
    id: 'cp-1', productId: 'p-1', phase: 'PRODUCTION', code: 'CP-4471',
    revision: 1, status: 'DRAFT',
    createdAt: '2026-08-19T08:00:00Z', updatedAt: '2026-08-19T08:00:00Z', ...over
  });

  const line = (over: Partial<ControlPlanLineView> = {}): ControlPlanLineView => ({
    id: 'l-1', sequenceNo: 10, characteristicLabel: 'Diamètre alésage',
    characteristicType: 'PRODUCT', ...over
  });

  let fixture: ComponentFixture<ProductControlPlanTabComponent>;
  let component: ProductControlPlanTabComponent;
  let service: jasmine.SpyObj<ProductsService>;
  let auth: jasmine.SpyObj<AuthService>;
  let snack: { open: jasmine.Spy };
  let action: Subject<void>;

  async function setup(roles: string[]): Promise<void> {
    service = jasmine.createSpyObj<ProductsService>('ProductsService',
      ['controlPlans', 'controlPlan', 'createControlPlan', 'openRevision',
        'approveControlPlan', 'deleteLine']);
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole', 'stepUp']);
    action = new Subject<void>();
    snack = { open: jasmine.createSpy('open').and.returnValue({ onAction: () => action }) };
    auth.hasAnyRole.and.callFake((wanted: string[]) =>
      wanted.some(role => roles.includes(role)));

    await TestBed.configureTestingModule({
      declarations: [ProductControlPlanTabComponent],
      imports: [SharedModule, UiModule, MatMenuModule, NoopAnimationsModule],
      providers: [
        { provide: ProductsService, useValue: service },
        { provide: AuthService, useValue: auth },
        { provide: MatDialog, useValue: { open: () => ({ afterClosed: () => of(null) }) } },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ProductControlPlanTabComponent);
    component = fixture.componentInstance;
    component.productId = 'p-1';
  }

  it('marque « sans justification » une ligne qui ne cite aucune ligne de PFMEA', fakeAsync(async () => {
    await setup(['QUALITY_MANAGER']);
    service.controlPlans.and.returnValue(of([plan()]));
    service.controlPlan.and.returnValue(of({
      plan: plan(), lines: [line(), line({ id: 'l-2', fmeaItemId: 'i-9' })]
    }));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const badges = fixture.nativeElement.querySelectorAll('.qos-badge[data-tone="warning"]');
    expect(badges.length).toBe(1);
    expect(badges[0].textContent).toContain('sans justification');
  }));

  it('cache le bouton d’approbation à un rôle qui n’y a pas droit', fakeAsync(async () => {
    await setup(['QUALITY_MANAGER']);
    service.controlPlans.and.returnValue(of([plan()]));
    service.controlPlan.and.returnValue(of({ plan: plan(), lines: [] }));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.canApprove).toBeFalse();
    expect(fixture.nativeElement.textContent).not.toContain('Approuver');
  }));

  it('montre le bouton d’approbation au directeur qualité', fakeAsync(async () => {
    await setup(['DIRECTOR_QUALITY']);
    service.controlPlans.and.returnValue(of([plan()]));
    service.controlPlan.and.returnValue(of({ plan: plan(), lines: [] }));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.canApprove).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Approuver');
  }));

  it('ouvre d’abord le brouillon quand il en existe un', fakeAsync(async () => {
    await setup(['QUALITY_MANAGER']);
    const active = plan({ id: 'cp-active', status: 'ACTIVE' });
    const draft = plan({ id: 'cp-draft', status: 'DRAFT', revision: 2 });
    service.controlPlans.and.returnValue(of([active, draft]));
    service.controlPlan.and.returnValue(of({ plan: draft, lines: [] }));

    fixture.detectChanges();
    tick();

    expect(service.controlPlan).toHaveBeenCalledWith('p-1', 'cp-draft');
  }));

  it('n’autorise l’édition que sur un brouillon', fakeAsync(async () => {
    await setup(['QUALITY_MANAGER']);
    service.controlPlans.and.returnValue(of([plan({ status: 'ACTIVE' })]));
    service.controlPlan.and.returnValue(of({ plan: plan({ status: 'ACTIVE' }), lines: [line()] }));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.editable).toBeFalse();
    expect(fixture.nativeElement.textContent).toContain('Ouvrir une révision');
  }));

  it('propose une réauthentification quand la signature exige un second facteur', fakeAsync(async () => {
    // Le serveur refuse la signature, pas la session : on redemande le palier
    // plutôt que d'afficher « approbation impossible ».
    await setup(['DIRECTOR_QUALITY']);
    service.controlPlans.and.returnValue(of([plan()]));
    service.controlPlan.and.returnValue(of({ plan: plan(), lines: [] }));
    service.approveControlPlan.and.returnValue(throwError(() => ({
      status: 403, error: { type: 'https://qualitos.io/errors/step-up-required' }
    })));
    auth.stepUp.and.returnValue(true);

    fixture.detectChanges();
    tick();
    component.approve();
    tick();
    action.next();

    expect(auth.stepUp).toHaveBeenCalledWith('/products/p-1');
  }));

  it('affiche l’empreinte et la transaction d’un plan scellé', fakeAsync(async () => {
    // Un auditeur qui reçoit le PDF veut confronter son empreinte à celle-ci.
    // L'afficher en clair est ce qui lui évite de nous croire sur parole.
    await setup(['QUALITY_MANAGER']);
    const sealed = plan({
      status: 'ACTIVE',
      sealSha256: '0f5a1b2c3d4e5f60718293a4b5c6d7e8f90a1b2c3d4e5f60718293a4b5c6d7e8',
      anchorTxRef: 'tx-0001'
    });
    service.controlPlans.and.returnValue(of([sealed]));
    service.controlPlan.and.returnValue(of({ plan: sealed, lines: [line()] }));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const seal = fixture.nativeElement.querySelector('.cp-seal');
    expect(seal).toBeTruthy();
    expect(seal.textContent).toContain('0f5a1b2c');
    expect(seal.textContent).toContain('tx-0001');
  }));

  it('ne montre aucune preuve sur un brouillon, qui n’a rien d’opposable', fakeAsync(async () => {
    await setup(['QUALITY_MANAGER']);
    service.controlPlans.and.returnValue(of([plan({ status: 'DRAFT' })]));
    service.controlPlan.and.returnValue(of({ plan: plan({ status: 'DRAFT' }), lines: [line()] }));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.cp-seal')).toBeNull();
  }));

  it('affiche un état vide explicite quand le produit n’a aucun plan', fakeAsync(async () => {
    await setup(['QUALITY_MANAGER']);
    service.controlPlans.and.returnValue(of([]));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.selected).toBeUndefined();
    expect(fixture.nativeElement.querySelector('.empty')).toBeTruthy();
    expect(service.controlPlan).not.toHaveBeenCalled();
  }));
});
