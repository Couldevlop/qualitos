import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { SpcCapabilityService } from '../../../spc/capability/spc-capability.service';
import { SpcSettingsComponent } from './spc-settings.component';

/** « Paramètres SPC » : lecture, règles locales sur les deux seuils, enregistrement. */
describe('SpcSettingsComponent', () => {
  let fixture: ComponentFixture<SpcSettingsComponent>;
  let component: SpcSettingsComponent;
  let service: jasmine.SpyObj<SpcCapabilityService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);

  async function setup(lecture: 'ok' | 'erreur' = 'ok'): Promise<void> {
    service = jasmine.createSpyObj<SpcCapabilityService>('SpcCapabilityService', ['settings', 'updateSettings']);
    service.settings.and.returnValue(lecture === 'ok'
      ? of({ capableThreshold: 1.67, watchThreshold: 1.33, defaults: true })
      : throwError(() => ({ status: 500 })));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [SpcSettingsComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: SpcCapabilityService, useValue: service },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(SpcSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('lit les seuils, dit qu\'ils sont ceux de la plateforme, et ne propose rien d\'enregistrer', async () => {
    await setup();
    expect(component.capableThreshold).toBe(1.67);
    expect(hote().textContent).toContain('Ce sont aujourd\'hui les seuils de la plateforme.');
    expect(un('resume')?.textContent).toContain('Non maîtrisé < 1.33 ≤ À surveiller < 1.67 ≤ Maîtrisé');
    expect(component.dirty).toBeFalse();
    expect(un('enregistrer')).toBeNull();
  });

  it('refuse un seuil hors bornes ou à plus de deux décimales', async () => {
    await setup();
    for (const v of [0, -1, 10.01, 1.234, Number.NaN]) {
      component.capableThreshold = v;
      expect(component.problems.length).withContext(String(v)).toBe(1);
    }
    component.capableThreshold = 10;
    expect(component.problems).toEqual([]);
  });

  it('refuse un seuil de surveillance qui ne reste pas sous le seuil de maîtrise', async () => {
    await setup();
    component.watchThreshold = 1.67;
    fixture.detectChanges();
    expect(component.problems).toEqual(['Le seuil de surveillance doit rester sous le seuil de maîtrise.']);
    expect(un('problemes')).not.toBeNull();
    expect(un('enregistrer')?.hasAttribute('disabled')).toBeTrue();
    component.save();
    expect(service.updateSettings).not.toHaveBeenCalled();
  });

  it('enregistre, puis reprend la réponse du serveur', async () => {
    await setup();
    component.capableThreshold = 2;
    component.watchThreshold = 1.5;
    service.updateSettings.and.returnValue(of({ capableThreshold: 2, watchThreshold: 1.5, defaults: false }));
    fixture.detectChanges();
    un('enregistrer')!.click();
    expect(service.updateSettings).toHaveBeenCalledWith(2, 1.5);
    expect(component.defaults).toBeFalse();
    expect(component.dirty).toBeFalse();
    expect(snack.open).toHaveBeenCalledWith('Paramètres SPC enregistrés. Ils s\'appliquent aux prochaines études.',
      undefined, { duration: 4000 });
  });

  it('annule les changements', async () => {
    await setup();
    component.watchThreshold = 1.4;
    fixture.detectChanges();
    un('annuler')!.click();
    expect(component.watchThreshold).toBe(1.33);
    expect(component.dirty).toBeFalse();
  });

  it('dit l\'échec d\'un enregistrement', async () => {
    await setup();
    component.capableThreshold = 2;
    service.updateSettings.and.returnValue(throwError(() => ({ status: 403 })));
    component.save();
    expect(component.saving).toBeFalse();
    expect(snack.open).toHaveBeenCalledWith('Vous n\'avez pas les droits pour cette action.', undefined, { duration: 5000 });
  });

  it('dit l\'échec de la lecture et réessaie', async () => {
    await setup('erreur');
    expect(component.failed).toBeTrue();
    expect(component.dirty).toBeFalse();
    component.discard();
    expect(hote().querySelector('[role="alert"]')).not.toBeNull();
    expect(component.summary).toContain('1.33');
    service.settings.and.returnValue(of({ capableThreshold: 1.5, watchThreshold: 1, defaults: false }));
    component.charger();
    expect(component.capableThreshold).toBe(1.5);
  });
});
