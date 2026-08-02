import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { IshikawaService } from '../../ishikawa.service';
import { IshikawaDiagramResponse } from '../../ishikawa.types';
import { IshikawaCreateDialogComponent } from './ishikawa-create-dialog.component';

/**
 * Création d'un diagramme d'Ishikawa (§3.5).
 *
 * Le dialogue porte deux responsabilités qui se voient mal à l'usage : il choisit
 * le découpage des branches (6M, 7M, 8M) — qui conditionne toute l'analyse
 * ultérieure — et il rattache le diagramme à son auteur depuis le JWT, jamais
 * depuis un champ de formulaire (§18.2 #2).
 */
describe('IshikawaCreateDialogComponent', () => {
  let fixture: ComponentFixture<IshikawaCreateDialogComponent>;
  let component: IshikawaCreateDialogComponent;
  let svc: jasmine.SpyObj<IshikawaService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<IshikawaCreateDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let session: AuthUser | null;

  const created = { id: 'ish-9', problemStatement: 'P' } as IshikawaDiagramResponse;

  beforeEach(async () => {
    session = { userId: 'u1', tenantId: 't1', displayName: 'Demo', roles: ['quality_manager'] };
    svc = jasmine.createSpyObj<IshikawaService>('IshikawaService', ['createDiagram']);
    svc.createDiagram.and.returnValue(of(created));
    dialogRef = jasmine.createSpyObj<MatDialogRef<IshikawaCreateDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [IshikawaCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: IshikawaService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => session } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(IshikawaCreateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ---- Ouverture ---------------------------------------------------------------

  it('propose les trois découpages de branches, 6M par défaut', () => {
    expect(component.modes.map(m => m.value)).toEqual(['SIX_M', 'SEVEN_M', 'EIGHT_M']);
    // Le 6M est le socle : les deux autres l'étendent, ce que les indications
    // doivent dire pour que le choix soit éclairé.
    expect(component.modes[1].hint).toContain('6M');
    expect(component.modes[2].hint).toContain('7M');
    expect(component.form.getRawValue().mode).toBe('SIX_M');
  });

  it('s\'ouvre sur un formulaire vide et invalide', () => {
    expect(component.form.getRawValue().problemStatement).toBe('');
    expect(component.form.valid).toBeFalse();
    expect(component.submitting).toBeFalse();
  });

  // ---- Validation ----------------------------------------------------------------

  it('refuse un énoncé de problème absent', () => {
    component.submit();

    expect(svc.createDiagram).not.toHaveBeenCalled();
    expect(component.form.controls.problemStatement.touched).toBeTrue();
  });

  it('refuse un énoncé au-delà de la limite du serveur', () => {
    component.form.patchValue({ problemStatement: 'x'.repeat(501) });

    component.submit();

    expect(svc.createDiagram).not.toHaveBeenCalled();
  });

  it('n\'envoie pas deux fois pendant un envoi en cours', () => {
    component.form.patchValue({ problemStatement: 'Défauts de soudure' });
    component.submitting = true;

    component.submit();

    expect(svc.createDiagram).not.toHaveBeenCalled();
  });

  // ---- Création --------------------------------------------------------------------

  it('crée le diagramme au nom de l\'utilisateur du JWT, champs nettoyés', () => {
    component.form.patchValue({
      problemStatement: '  Hausse des rebuts ligne 3  ',
      description: '  Analyse menée avec la production.  ',
      mode: 'SEVEN_M'
    });

    component.submit();

    expect(svc.createDiagram).toHaveBeenCalledWith({
      problemStatement: 'Hausse des rebuts ligne 3',
      description: 'Analyse menée avec la production.',
      mode: 'SEVEN_M',
      ownerId: 'u1'
    });
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('omet une description vide plutôt que d\'envoyer une chaîne d\'espaces', () => {
    component.form.patchValue({ problemStatement: 'Problème', description: '   ' });

    component.submit();

    expect(svc.createDiagram).toHaveBeenCalledWith(
      jasmine.objectContaining({ description: undefined })
    );
  });

  // ---- Cas dégradés -------------------------------------------------------------------

  it('refuse de créer sans session et le dit, plutôt que d\'inventer un auteur', () => {
    session = null;
    component.form.patchValue({ problemStatement: 'Problème' });

    component.submit();

    expect(svc.createDiagram).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    // L'indicateur de chargement ne doit pas rester actif sur un abandon.
    expect(component.submitting).toBeFalse();
  });

  it('laisse le dialogue ouvert et explique quand le serveur refuse', () => {
    svc.createDiagram.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ problemStatement: 'Problème' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    // La saisie doit rester récupérable : refermer ferait perdre le travail.
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien créer à l\'annulation', () => {
    component.cancel();

    expect(svc.createDiagram).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
