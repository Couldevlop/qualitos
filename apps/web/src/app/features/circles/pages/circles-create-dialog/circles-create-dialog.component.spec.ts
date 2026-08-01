import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CirclesService } from '../../circles.service';
import { CircleResponse } from '../../circles.types';
import { CirclesCreateDialogComponent } from './circles-create-dialog.component';

/**
 * Création d'un cercle de qualité (§3.3).
 */
describe('CirclesCreateDialogComponent', () => {
  let fixture: ComponentFixture<CirclesCreateDialogComponent>;
  let component: CirclesCreateDialogComponent;
  let svc: jasmine.SpyObj<CirclesService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CirclesCreateDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const created = { id: 'c-9', name: 'Cercle' } as CircleResponse;

  beforeEach(async () => {
    svc = jasmine.createSpyObj<CirclesService>('CirclesService', ['createCircle']);
    svc.createCircle.and.returnValue(of(created));
    dialogRef = jasmine.createSpyObj<MatDialogRef<CirclesCreateDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [CirclesCreateDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: CirclesService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CirclesCreateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('s\'ouvre sur un formulaire vide et invalide', () => {
    expect(component.form.getRawValue()).toEqual({ name: '', topic: '', description: '' });
    expect(component.form.valid).toBeFalse();
    expect(component.submitting).toBeFalse();
  });

  it('refuse un cercle sans nom', () => {
    component.submit();

    expect(svc.createCircle).not.toHaveBeenCalled();
    expect(component.form.controls.name.touched).toBeTrue();
  });

  it('refuse un nom au-delà de la limite du serveur', () => {
    component.form.patchValue({ name: 'x'.repeat(256) });

    component.submit();

    expect(svc.createCircle).not.toHaveBeenCalled();
  });

  it('n\'envoie pas deux fois pendant un envoi en cours', () => {
    component.form.patchValue({ name: 'Cercle production' });
    component.submitting = true;

    component.submit();

    expect(svc.createCircle).not.toHaveBeenCalled();
  });

  it('crée le cercle en nettoyant les champs et en omettant les vides', () => {
    component.form.patchValue({
      name: '  Cercle production ligne 3  ',
      topic: '  production-soudure  ',
      description: '   '
    });

    component.submit();

    expect(svc.createCircle).toHaveBeenCalledWith({
      name: 'Cercle production ligne 3',
      topic: 'production-soudure',
      description: undefined
    });
    expect(dialogRef.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('laisse le dialogue ouvert et explique quand le serveur refuse', () => {
    svc.createCircle.and.returnValue(throwError(() => ({ status: 500 })));
    component.form.patchValue({ name: 'Cercle' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    // La saisie doit rester récupérable : refermer ferait perdre le travail.
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien créer à l\'annulation', () => {
    component.cancel();

    expect(svc.createCircle).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
