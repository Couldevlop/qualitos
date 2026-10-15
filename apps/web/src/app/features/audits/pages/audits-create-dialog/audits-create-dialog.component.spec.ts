import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { DirectoryService } from '../../../../core/directory/directory.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerComponent } from '../../../../shared/user-picker/user-picker.component';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { AuditsService } from '../../audits.service';
import { AuditPlanResponse } from '../../audits.types';
import { AuditsCreateDialogComponent } from './audits-create-dialog.component';

/**
 * Création d'un plan d'audit (§4.4).
 *
 * L'auditeur responsable se CHOISIT dans l'annuaire en lecture restreinte
 * (ADR 0091) — ouvert à tout membre —, l'utilisateur connecté par défaut. Le
 * nom recopié est celui de l'annuaire, jamais l'adresse quand un nom existe.
 */
describe('AuditsCreateDialogComponent', () => {
  let fixture: ComponentFixture<AuditsCreateDialogComponent>;
  let component: AuditsCreateDialogComponent;
  let svc: jasmine.SpyObj<AuditsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<AuditsCreateDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let session: AuthUser | null;
  let directory: jasmine.SpyObj<DirectoryService>;

  const MOI: DirectoryUser = { id: 'i1', accountId: 'u1', displayName: 'Demo', email: 'demo@exemple.test', active: true };
  const BRUNO: DirectoryUser = { id: 'i2', accountId: 'u2', displayName: 'Bruno Petit', email: 'bruno@exemple.test', active: true };

  const created = { id: 'a-9', title: 'Audit' } as AuditPlanResponse;

  beforeEach(async () => {
    session = { userId: 'u1', tenantId: 't1', displayName: 'Demo', roles: ['quality_manager'] };
    svc = jasmine.createSpyObj<AuditsService>('AuditsService', ['createPlan']);
    svc.createPlan.and.returnValue(of(created));
    dialogRef = jasmine.createSpyObj<MatDialogRef<AuditsCreateDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.getByAccount.and.callFake(id =>
      [MOI, BRUNO].some(u => u.accountId === id)
        ? of([MOI, BRUNO].find(u => u.accountId === id)!)
        : throwError(() => ({ status: 404 })));
    directory.search.and.returnValue(of({ content: [MOI, BRUNO], page: 0, size: 20, totalElements: 2, totalPages: 1 }));

    await TestBed.configureTestingModule({
      declarations: [AuditsCreateDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        { provide: AuditsService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => session } },
        { provide: DirectoryService, useValue: directory }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(AuditsCreateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  const picker = (): UserPickerComponent =>
    fixture.debugElement.query(By.directive(UserPickerComponent)).componentInstance as UserPickerComponent;

  /** Choisir une personne dans le sélecteur, comme le ferait un clic dans sa liste. */
  function choose(user: DirectoryUser): void {
    picker().onSelected({ option: { value: user } } as never);
    fixture.detectChanges();
  }

  // ---- Ouverture ---------------------------------------------------------------

  it('propose les six natures d\'audit du référentiel, interne par défaut', () => {
    expect(component.types.map(t => t.value))
      .toEqual(['INTERNAL', 'EXTERNAL', 'SUPPLIER', 'LPA', 'CERTIFICATION', 'SURVEILLANCE']);
    expect(component.types.every(t => !!t.label)).toBeTrue();
    expect(component.form.getRawValue().type).toBe('INTERNAL');
  });

  it('s\'ouvre sur un formulaire vide et invalide', () => {
    expect(component.form.getRawValue().title).toBe('');
    expect(component.form.valid).toBeFalse();
    expect(component.submitting).toBeFalse();
  });

  it('l\'auditeur est choisi dans l\'annuaire, soi-même par défaut, affiché par son nom', () => {
    expect(component.form.getRawValue().leadAuditorId).toBe('u1');
    expect(directory.getByAccount).toHaveBeenCalledWith('u1');
    expect(picker().selected?.displayName).toBe('Demo');
    // Aucun appel à l'annuaire d'administration, ni message « pas accessible avec vos droits ».
    expect(fixture.nativeElement.querySelector('[data-test="annuaire-indisponible"]')).toBeNull();
  });

  // ---- Validation ---------------------------------------------------------------

  it('refuse un plan sans intitulé', () => {
    component.submit();

    expect(svc.createPlan).not.toHaveBeenCalled();
    expect(component.form.controls.title.touched).toBeTrue();
  });

  it('refuse un intitulé ou une norme au-delà des limites du serveur', () => {
    component.form.patchValue({ title: 'x'.repeat(256) });
    component.submit();
    expect(svc.createPlan).not.toHaveBeenCalled();

    component.form.patchValue({ title: 'Audit', standard: 'y'.repeat(101) });
    component.submit();
    expect(svc.createPlan).not.toHaveBeenCalled();
  });

  it('n\'envoie pas deux fois pendant un envoi en cours', () => {
    component.form.patchValue({ title: 'Audit interne' });
    component.submitting = true;

    component.submit();

    expect(svc.createPlan).not.toHaveBeenCalled();
  });

  // ---- Création -------------------------------------------------------------------

  it('crée le plan avec l\'auditeur du JWT, champs nettoyés', () => {
    component.form.patchValue({
      title: '  Audit interne ISO 9001  ',
      scope: '  Siège + 3 usines  ',
      type: 'CERTIFICATION',
      standard: '  ISO_9001  ',
      scheduledDate: '2026-11-15'
    });

    component.submit();

    expect(svc.createPlan).toHaveBeenCalledWith({
      title: 'Audit interne ISO 9001',
      scope: 'Siège + 3 usines',
      type: 'CERTIFICATION',
      standard: 'ISO_9001',
      scheduledDate: '2026-11-15',
      // Laissé vide : la clé part à `undefined` comme les autres facultatifs
      // (cf. « omet les champs facultatifs laissés vides ») et disparaît à la
      // sérialisation JSON. Le serveur ne voit donc aucun destinataire.
      reminderEmail: undefined,
      leadAuditorId: 'u1',
      leadAuditorName: 'Demo'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('confie l\'audit à un collègue choisi, avec son NOM d\'annuaire et non son adresse', () => {
    component.form.patchValue({ title: 'Audit' });
    choose(BRUNO);

    component.submit();

    expect(svc.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
      leadAuditorId: 'u2', leadAuditorName: 'Bruno Petit'
    }));
  });

  it('recopie l\'adresse seulement quand l\'annuaire n\'a pas de nom', () => {
    component.form.patchValue({ title: 'Audit' });
    choose({ ...BRUNO, displayName: '' });

    component.submit();

    expect(svc.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
      leadAuditorId: 'u2', leadAuditorName: 'bruno@exemple.test'
    }));
  });

  it('nettoie le destinataire du rappel comme les autres champs', () => {
    component.form.patchValue({
      title: 'Audit', reminderEmail: '  qualite@exemple.test  '
    });

    component.submit();

    expect(svc.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
      reminderEmail: 'qualite@exemple.test'
    }));
  });

  it('refuse d\'envoyer une adresse de rappel mal formée', () => {
    // Le formulaire barre la route AVANT l'appel : une adresse fautive rejetée
    // par le serveur ne se découvrirait qu'au 400, sans dire quel champ est en cause.
    component.form.patchValue({ title: 'Audit', reminderEmail: 'pas-une-adresse' });

    component.submit();

    expect(svc.createPlan).not.toHaveBeenCalled();
  });

  it('omet les champs facultatifs laissés vides', () => {
    component.form.patchValue({ title: 'Audit', scope: '   ', standard: '  ' });

    component.submit();

    expect(svc.createPlan).toHaveBeenCalledWith(jasmine.objectContaining({
      scope: undefined, standard: undefined, scheduledDate: undefined
    }));
  });

  // ---- Cas dégradés -----------------------------------------------------------------

  it('refuse de créer sans auditeur responsable, plutôt que d\'en inventer un', () => {
    component.form.patchValue({ title: 'Audit' });
    picker().clear();
    fixture.detectChanges();

    expect(component.form.getRawValue().leadAuditorId).toBeFalsy();
    component.submit();

    expect(svc.createPlan).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('laisse le dialogue ouvert et explique quand le serveur refuse', () => {
    svc.createPlan.and.returnValue(throwError(() => ({ status: 500 })));
    component.form.patchValue({ title: 'Audit' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    // La saisie doit rester récupérable : refermer ferait perdre le travail.
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien créer à l\'annulation', () => {
    component.cancel();

    expect(svc.createPlan).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
