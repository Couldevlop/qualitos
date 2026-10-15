import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatSnackBar } from '@angular/material/snack-bar';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { DirectoryService } from '../../../../core/directory/directory.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerComponent } from '../../../../shared/user-picker/user-picker.component';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { MocService } from '../../../changes/moc.service';
import { MocSettings } from '../../../changes/moc.types';
import { MocSettingsComponent } from './moc-settings.component';

/**
 * « Paramètres MOC » : lecture, règles locales (seuils, poids, délai, rôles),
 * titulaires choisis dans l'annuaire en lecture restreinte (ADR 0091), nom et
 * adresse recopiés, utilisateurs désactivés signalés, enregistrement.
 */
describe('MocSettingsComponent', () => {
  let fixture: ComponentFixture<MocSettingsComponent>;
  let component: MocSettingsComponent;
  let service: jasmine.SpyObj<MocService>;
  let directory: jasmine.SpyObj<DirectoryService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const membre = (id: string, displayName: string, email: string, active = true): DirectoryUser => ({
    id: 'app-' + id, accountId: id, displayName, email, active
  });
  const MEMBRES = [
    membre('u1', 'Jean Dupont', 'jean@acme.test'),
    membre('u2', 'Marie Curie', 'marie@acme.test'),
    membre('u3', 'Ancien Membre', 'ancien@acme.test', false)
  ];

  function reglages(): MocSettings {
    return {
      majorThreshold: 25, criticalThreshold: 50, reminderDays: 3,
      weights: { SAFETY: 1, HEALTH_ENVIRONMENT: 1, QUALITY: 1, PRODUCTION: 1, MAINTENANCE: 1, COSTS: 1, REGULATORY: 1, TRAINING: 1 },
      roles: [
        { id: 'r1', label: 'Responsable de service', requiredFor: ['MINOR', 'MAJOR', 'CRITICAL'], holders: [{ userId: 'u1', label: 'jean@acme.test' }] },
        { id: 'r2', label: 'HSE', requiredFor: ['MINOR', 'MAJOR', 'CRITICAL'], holders: [{ userId: 'u3', label: 'ancien@acme.test' }] },
        { id: 'r3', label: 'Directeur de site', requiredFor: ['CRITICAL'], holders: [{ userId: 'u9', label: 'parti@acme.test' }] }
      ]
    };
  }

  async function setup(annuaire = true): Promise<void> {
    service = jasmine.createSpyObj<MocService>('MocService', ['settings', 'saveSettings']);
    service.settings.and.returnValue(of(reglages()));
    directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.search.and.returnValue(annuaire
      ? of({ content: MEMBRES.filter(m => m.active), page: 0, size: 20, totalElements: 2, totalPages: 1 })
      : throwError(() => ({ status: 0 })));
    directory.getByAccount.and.callFake(id => {
      if (!annuaire) return throwError(() => ({ status: 0 }));
      const m = MEMBRES.find(x => x.accountId === id);
      return m ? of(m) : throwError(() => ({ status: 404 }));
    });
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [MocSettingsComponent],
      imports: [SharedModule, UiModule, FormsModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: MocService, useValue: service },
        { provide: DirectoryService, useValue: directory },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(MocSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('charge les réglages, sans rien à enregistrer', async () => {
    await setup();
    expect(component.roles.map(r => r.label)).toEqual(['Responsable de service', 'HSE', 'Directeur de site']);
    expect(component.dirty).toBeFalse();
    expect(component.problems).toEqual([]);
    expect(hote().querySelector('[data-test="barre"]')).toBeNull();
    expect(hote().querySelector('[data-test="resume-seuils"]')?.textContent).toContain('Mineur < 25 ≤ Majeur < 50 ≤ Critique');
  });

  it('signale un titulaire désactivé ou absent de l’annuaire', async () => {
    await setup();
    expect(directory.getByAccount.calls.allArgs()).toEqual([['u1'], ['u3'], ['u9']]);
    expect(component.holderState({ userId: 'u1', label: 'x' })).toBe('ok');
    expect(component.holderState({ userId: 'u3', label: 'x' })).toBe('inactive');
    expect(component.holderState({ userId: 'u9', label: 'x' })).toBe('unknown');
    expect(hote().textContent).toContain('(désactivé)');
    expect(hote().textContent).toContain("(absent de l'annuaire)");
  });

  it('les titulaires viennent de l’annuaire, nom et adresse recopiés, et pas deux fois', async () => {
    await setup();
    const hse = component.roles[1];
    // Le sélecteur de chaque rôle écarte ses titulaires actuels.
    expect(component.holderIds(hse)).toEqual(['u3']);
    const pickers = fixture.debugElement.queryAll(By.directive(UserPickerComponent));
    expect(pickers.length).toBe(3);
    const pickerHse = pickers[1].componentInstance as UserPickerComponent;
    expect(pickerHse.excludeIds).toEqual(['u3']);

    // Un choix dans le sélecteur ajoute le titulaire, puis le sélecteur se vide.
    pickerHse.onSelected({ option: { value: MEMBRES[1] } } as never);
    expect(pickerHse.selected).toBeNull();
    component.addHolder(hse, MEMBRES[1]);
    component.addHolder(hse, null);
    expect(hse.holders).toEqual([
      { userId: 'u3', label: 'ancien@acme.test' },
      { userId: 'u2', label: 'Marie Curie', email: 'marie@acme.test' }
    ]);
    component.removeHolder(hse, hse.holders[0]);
    expect(hse.holders.map(h => h.userId)).toEqual(['u2']);
    expect(component.dirty).toBeTrue();

    service.saveSettings.and.returnValue(of(reglages()));
    component.save();
    expect(service.saveSettings.calls.mostRecent().args[0].roles[1].holders)
      .toEqual([{ userId: 'u2', label: 'Marie Curie', email: 'marie@acme.test' }]);
  });

  it('recopie l’adresse comme nom seulement quand l’annuaire n’a pas de nom', async () => {
    await setup();
    const role = component.roles[0];
    component.addHolder(role, membre('u7', '', 'sans.nom@acme.test'));
    expect(role.holders[1]).toEqual({ userId: 'u7', label: 'sans.nom@acme.test', email: 'sans.nom@acme.test' });
  });

  it('refuse localement des seuils, un délai ou des poids incohérents', async () => {
    await setup();
    component.majorThreshold = 50;
    expect(component.problems.join()).toContain('seuil majeur < seuil critique');
    component.majorThreshold = 25;
    component.reminderDays = 31;
    expect(component.problems.join()).toContain('1 à 30');
    component.reminderDays = 3;
    component.weights.SAFETY = 11;
    expect(component.problems.join()).toContain('entier de 0 à 10');
    for (const d of component.domains) component.weights[d] = 0;
    expect(component.problems.join()).toContain('supérieur à 0');
  });

  it('refuse localement des rôles incohérents', async () => {
    await setup();
    component.roles[1].label = 'responsable de service';
    expect(component.problems.join()).toContain('même libellé');
    component.roles[1].label = ' ';
    expect(component.problems.join()).toContain('1 à 100 caractères');
    component.roles[1].label = 'HSE';
    component.toggleRequired(component.roles[2], 'CRITICAL', false);
    component.toggleRequired(component.roles[0], 'CRITICAL', false);
    component.toggleRequired(component.roles[1], 'CRITICAL', false);
    expect(component.problems.join()).toContain('« Critique »');
    component.toggleRequired(component.roles[1], 'CRITICAL', true);
    component.toggleRequired(component.roles[1], 'CRITICAL', true);
    expect(component.roles[1].requiredFor.filter(c => c === 'CRITICAL').length).toBe(1);
    component.roles.splice(0, 3);
    expect(component.problems.join()).toContain('au moins un rôle');
  });

  it('ajoute, ordonne et retire des rôles ; avertit d’un rôle requis sans titulaire', async () => {
    await setup();
    component.addRole();
    const nouveau = component.roles[3];
    expect(nouveau.id).toBeNull();
    expect(component.warnNoHolder(nouveau)).toBeTrue();
    component.move(3, -1);
    expect(component.roles[2]).toBe(nouveau);
    component.move(0, -1);
    expect(component.roles[0].label).toBe('Responsable de service');
    component.removeRole(2);
    expect(component.roles.length).toBe(3);
  });

  it('enregistre ce qui a changé et reprend la réponse du serveur', async () => {
    await setup();
    component.reminderDays = 5;
    component.weights.PRODUCTION = 0;
    fixture.detectChanges();
    expect(hote().querySelector('[data-test="barre"]')).not.toBeNull();
    const reponse = { ...reglages(), reminderDays: 5 };
    service.saveSettings.and.returnValue(of(reponse));
    component.save();
    const envoye = service.saveSettings.calls.mostRecent().args[0];
    expect(envoye.reminderDays).toBe(5);
    expect(envoye.weights.PRODUCTION).toBe(0);
    expect(envoye.roles[0]).toEqual({ id: 'r1', label: 'Responsable de service',
      requiredFor: ['MINOR', 'MAJOR', 'CRITICAL'], holders: [{ userId: 'u1', label: 'jean@acme.test', email: null }] });
    expect(component.dirty).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
  });

  it('un refus du serveur nomme le rôle ; annuler revient à l’enregistré', async () => {
    await setup();
    component.roles.splice(2, 1);
    service.saveSettings.and.returnValue(throwError(() => ({
      status: 409, error: { code: 'moc.settings.role-in-use', role: 'Directeur de site' }
    })));
    component.save();
    expect(snack.open.calls.mostRecent().args[0]).toContain('« Directeur de site »');
    expect(component.saving).toBeFalse();
    component.discard();
    expect(component.roles.length).toBe(3);
    expect(component.dirty).toBeFalse();
  });

  it('annuaire indisponible : aucun titulaire signalé à tort, le sélecteur reste en place', async () => {
    await setup(false);
    expect(component.holderState({ userId: 'u3', label: 'x' })).toBe('ok');
    expect(component.holderState({ userId: 'u9', label: 'x' })).toBe('ok');
    expect(hote().textContent).not.toContain('(désactivé)');
    // Plus de « l'annuaire n'a pas répondu, les titulaires ne se modifient pas » :
    // le sélecteur dit lui-même que l'annuaire est indisponible, et l'on réessaie.
    expect(hote().querySelector('[data-test="recherche-0"]')).not.toBeNull();
  });

  it('dit quand les réglages ne se chargent pas', async () => {
    await setup();
    service.settings.and.returnValue(throwError(() => ({ status: 500 })));
    component.charger();
    fixture.detectChanges();
    expect(component.failed).toBeTrue();
    expect(hote().querySelector('.echec')).not.toBeNull();
  });
});
