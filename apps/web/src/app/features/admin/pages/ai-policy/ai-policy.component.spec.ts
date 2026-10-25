import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { provideDirectoryStub } from '../../../../core/directory/testing/directory.stub';
import { PersonNameModule } from '../../../../shared/person-name/person-name.module';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { AiPolicyService } from '../../ai-policy.service';
import { AiPolicy } from '../../ai-policy.types';
import { AiPolicyComponent } from './ai-policy.component';

/** « Fournisseur d'IA » : l'état souverain ou non, le choix, son enregistrement. */
describe('AiPolicyComponent', () => {
  let fixture: ComponentFixture<AiPolicyComponent>;
  let component: AiPolicyComponent;
  let service: jasmine.SpyObj<AiPolicyService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);
  const radio = (sel: string) => un(sel)!.querySelector('input') as HTMLInputElement;

  const politique = (over: Partial<AiPolicy> = {}): AiPolicy => ({
    pinned: false, provider: null, effectiveProvider: 'mistral', platformDefault: 'mistral',
    catalogAvailable: true,
    providers: [
      { name: 'ollama', onPremise: true, configured: true },
      { name: 'anthropic', onPremise: false, configured: false },
      { name: 'mistral', onPremise: false, configured: true }
    ],
    updatedAt: null, updatedBy: null, ...over
  });

  async function setup(lecture: AiPolicy | 'erreur' = politique()): Promise<void> {
    service = jasmine.createSpyObj<AiPolicyService>('AiPolicyService', ['get', 'pin', 'release']);
    service.get.and.returnValue(lecture === 'erreur' ? throwError(() => ({ status: 500 })) : of(lecture));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [AiPolicyComponent],
      imports: [SharedModule, UiModule, FormsModule, PersonNameModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        provideDirectoryStub(),
        { provide: AiPolicyService, useValue: service },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(AiPolicyComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('sans politique, coche le défaut et dit que les demandes partent chez le fournisseur externe', async () => {
    await setup();
    expect(radio('choix-default').checked).toBeTrue();
    expect(un('etat')!.classList).toContain('etat--externe');
    expect(un('etat')!.textContent).toContain('Mistral AI');
    expect(un('enregistrer')).toBeNull();
  });

  it('un tenant épinglé sur le modèle local est annoncé souverain', async () => {
    await setup(politique({ pinned: true, provider: 'ollama', effectiveProvider: 'ollama',
      updatedAt: '2026-10-25T10:00:00Z', updatedBy: '22222222-2222-2222-2222-222222222222' }));
    expect(radio('choix-ollama').checked).toBeTrue();
    expect(un('etat')!.classList).toContain('etat--souverain');
    expect(component.sovereign).toBeTrue();
  });

  it('un fournisseur non configuré ne se choisit pas', async () => {
    await setup();
    expect(radio('choix-anthropic').disabled).toBeTrue();
    component.select('anthropic');
    expect(component.choice).toBe('default');
    expect(component.dirty).toBeFalse();
  });

  it('épingle le modèle local après enregistrement', async () => {
    await setup();
    service.pin.and.returnValue(of(politique({ pinned: true, provider: 'ollama', effectiveProvider: 'ollama' })));
    radio('choix-ollama').click();
    fixture.detectChanges();
    expect(component.dirty).toBeTrue();
    expect(un('etat')!.classList).toContain('etat--souverain');

    un('enregistrer')!.click();
    fixture.detectChanges();

    expect(service.pin).toHaveBeenCalledWith('ollama');
    expect(component.dirty).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
  });

  it('revenir au défaut retire la politique', async () => {
    await setup(politique({ pinned: true, provider: 'ollama', effectiveProvider: 'ollama' }));
    service.release.and.returnValue(of(politique()));
    component.select('default');
    component.save();
    expect(service.release).toHaveBeenCalled();
    expect(component.savedChoice).toBe('default');
  });

  it('prévient quand les demandes vont quitter l\'infrastructure', async () => {
    await setup(politique({ pinned: true, provider: 'ollama', effectiveProvider: 'ollama' }));
    component.select('mistral');
    fixture.detectChanges();
    expect(hote().querySelector('.barre__note')!.textContent).toContain('quitter votre infrastructure');
  });

  it('annuler rend le choix enregistré', async () => {
    await setup();
    component.select('ollama');
    component.discard();
    expect(component.choice).toBe('default');
  });

  it('un refus du serveur garde le choix affiché et le dit', async () => {
    await setup();
    service.pin.and.returnValue(throwError(() => ({ status: 422, error: { detail: 'non configuré' } })));
    component.select('mistral');
    component.save();
    expect(component.saving).toBeFalse();
    expect(component.choice).toBe('mistral');
    expect(snack.open).toHaveBeenCalled();
  });

  it('service d\'IA muet : le dit, et ne prétend pas savoir qui répond', async () => {
    await setup(politique({ catalogAvailable: false, platformDefault: null, effectiveProvider: null,
      providers: [
        { name: 'ollama', onPremise: true, configured: null },
        { name: 'anthropic', onPremise: false, configured: null },
        { name: 'mistral', onPremise: false, configured: null }
      ] }));
    expect(un('catalogue-absent')).not.toBeNull();
    expect(component.sovereign).toBeNull();
    expect(radio('choix-anthropic').disabled).toBeFalse();
  });

  it('un épinglage devenu inutilisable est signalé', async () => {
    await setup(politique({ pinned: true, provider: 'anthropic', effectiveProvider: 'anthropic' }));
    expect(un('epingle-casse')).not.toBeNull();
  });

  it('un échec de lecture se dit et se réessaie', async () => {
    await setup('erreur');
    expect(component.failed).toBeTrue();
    expect(snack.open).toHaveBeenCalled();
    service.get.and.returnValue(of(politique()));
    component.charger();
    expect(component.failed).toBeFalse();
  });

  it('nomme un fournisseur inconnu sans planter', async () => {
    await setup();
    expect(component.label(null)).toBe('inconnu');
    expect(component.save()).toBeUndefined();
  });
});
