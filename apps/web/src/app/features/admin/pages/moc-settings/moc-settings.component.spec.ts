import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { MocService } from '../../../changes/moc.service';
import { MocSettings } from '../../../changes/moc.types';
import { TenantUser } from '../../admin.types';
import { TenantTeamService } from '../../tenant-team.service';
import { MocSettingsComponent } from './moc-settings.component';

/**
 * « Paramètres MOC » : lecture, règles locales (seuils, poids, délai, rôles),
 * titulaires choisis dans l'annuaire, utilisateurs désactivés signalés, enregistrement.
 */
describe('MocSettingsComponent', () => {
  let fixture: ComponentFixture<MocSettingsComponent>;
  let component: MocSettingsComponent;
  let service: jasmine.SpyObj<MocService>;
  let team: jasmine.SpyObj<TenantTeamService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  const membre = (id: string, email: string, active = true): TenantUser => ({
    id: 'app-' + id, tenantId: 't', keycloakId: id, email, roles: ['user'], active, createdAt: '', updatedAt: ''
  });
  const MEMBRES = [membre('u1', 'jean@acme.test'), membre('u2', 'marie@acme.test'), membre('u3', 'ancien@acme.test', false)];

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
    team = jasmine.createSpyObj<TenantTeamService>('TenantTeamService', ['list']);
    team.list.and.returnValue(annuaire
      ? of({ content: MEMBRES, totalElements: 3, totalPages: 1, number: 0, size: 500 })
      : throwError(() => ({ status: 403 })));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [MocSettingsComponent],
      imports: [SharedModule, UiModule, FormsModule, MatAutocompleteModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: MocService, useValue: service },
        { provide: TenantTeamService, useValue: team },
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
    expect(component.holderState({ userId: 'u1', label: 'x' })).toBe('ok');
    expect(component.holderState({ userId: 'u3', label: 'x' })).toBe('inactive');
    expect(component.holderState({ userId: 'u9', label: 'x' })).toBe('unknown');
    expect(hote().textContent).toContain('(désactivé)');
    expect(hote().textContent).toContain("(absent de l'annuaire)");
  });

  it('les titulaires viennent de l’annuaire, actifs et pas deux fois', async () => {
    await setup();
    const hse = component.roles[1];
    expect(component.candidates(hse).map(m => m.keycloakId)).toEqual(['u1', 'u2']);
    hse.search = 'mar';
    expect(component.candidates(hse).map(m => m.email)).toEqual(['marie@acme.test']);
    component.addHolder(hse, MEMBRES[1]);
    component.addHolder(hse, MEMBRES[1]);
    expect(hse.holders.map(h => h.userId)).toEqual(['u3', 'u2']);
    expect(hse.search).toBe('');
    component.removeHolder(hse, hse.holders[0]);
    expect(hse.holders.map(h => h.userId)).toEqual(['u2']);
    expect(component.dirty).toBeTrue();
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
      requiredFor: ['MINOR', 'MAJOR', 'CRITICAL'], holders: [{ userId: 'u1', label: 'jean@acme.test' }] });
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

  it('sans annuaire, les titulaires ne se modifient pas et le dit', async () => {
    await setup(false);
    expect(component.directoryFailed).toBeTrue();
    expect(component.holderState({ userId: 'u9', label: 'x' })).toBe('ok');
    expect(hote().textContent).toContain("L'annuaire n'a pas répondu");
    expect(hote().querySelector('[data-test="recherche-0"]')).toBeNull();
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
