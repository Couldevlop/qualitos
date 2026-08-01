import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService, AuthUser } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { TrainingService } from '../../training.service';
import { EnrollmentResponse, PathResponse } from '../../training.types';
import { TrainingEnrollDialogComponent } from './training-enroll-dialog.component';

describe('TrainingEnrollDialogComponent', () => {
  let fixture: ComponentFixture<TrainingEnrollDialogComponent>;
  let component: TrainingEnrollDialogComponent;
  let svc: jasmine.SpyObj<TrainingService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<TrainingEnrollDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let session: AuthUser | null;

  const path = (over: Partial<PathResponse> = {}): PathResponse => ({
    id: 'path-1', tenantId: 't1', code: 'yellow-belt', name: 'Yellow Belt',
    durationHours: 14, passingScore: 70, status: 'ACTIVE', createdBy: 'u1',
    createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-01-01T00:00:00Z', ...over
  });

  const enrollment: EnrollmentResponse = {
    id: 'enr-1', tenantId: 't1', userId: 'u1', pathId: 'path-1',
    status: 'ENROLLED', progressPct: 0, enrolledOn: '2026-06-01',
    createdAt: '2026-06-01T00:00:00Z', updatedAt: '2026-06-01T00:00:00Z'
  };

  const page = <T>(content: T[]) => ({
    content, totalElements: content.length, totalPages: 1, number: 0, size: content.length
  });

  function build(): void {
    fixture = TestBed.createComponent(TrainingEnrollDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  beforeEach(async () => {
    svc = jasmine.createSpyObj<TrainingService>('TrainingService', ['listPaths', 'enroll']);
    svc.listPaths.and.returnValue(of(page([path()])));
    svc.enroll.and.returnValue(of(enrollment));
    dialogRef = jasmine.createSpyObj<MatDialogRef<TrainingEnrollDialogComponent>>('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    session = { userId: 'u1', tenantId: 't1', displayName: 'Demo', roles: ['user'] };

    await TestBed.configureTestingModule({
      declarations: [TrainingEnrollDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: TrainingService, useValue: svc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: AuthService, useValue: { snapshot: () => session } }
      ]
    }).compileComponents();
  });

  it('ne propose que les parcours actifs au premier affichage', () => {
    build();
    expect(svc.listPaths).toHaveBeenCalledWith(0, 100, 'ACTIVE');
    expect(component.loading).toBeFalse();
    expect(component.paths$.value.length).toBe(1);
  });

  it('recharge la liste quand le filtre de statut change', () => {
    build();
    component.statusFilter.setValue('ARCHIVED');
    expect(svc.listPaths).toHaveBeenCalledWith(0, 100, 'ARCHIVED');
  });

  it('affiche un message quand le catalogue de parcours est indisponible', () => {
    svc.listPaths.and.returnValue(throwError(() => ({ status: 500 })));
    build();
    expect(component.loadError).toBe('Erreur serveur — réessayez dans un instant.');
    expect(component.loading).toBeFalse();
  });

  it('efface le message d’erreur dès qu’un rechargement réussit', () => {
    svc.listPaths.and.returnValue(throwError(() => ({ status: 500 })));
    build();
    svc.listPaths.and.returnValue(of(page([path()])));
    component.loadPaths();
    expect(component.loadError).toBeNull();
  });

  it('refuse l’envoi tant qu’aucun parcours n’est sélectionné', () => {
    build();
    component.submit();
    expect(svc.enroll).not.toHaveBeenCalled();
    expect(component.form.controls.pathId.touched).toBeTrue();
  });

  it('refuse l’inscription sans session et n’appelle pas le serveur', () => {
    session = null;
    build();
    component.form.patchValue({ pathId: 'path-1' });
    component.submit();
    expect(svc.enroll).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('inscrit l’utilisateur du JWT au parcours choisi et rend le résultat au parent', () => {
    build();
    component.form.patchValue({ pathId: 'path-1' });
    component.submit();
    expect(svc.enroll).toHaveBeenCalledWith({ userId: 'u1', pathId: 'path-1' });
    expect(dialogRef.close).toHaveBeenCalledWith(enrollment);
  });

  it('garde le dialogue ouvert et signale un doublon refusé par le serveur', () => {
    build();
    svc.enroll.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ pathId: 'path-1' });
    component.submit();
    expect(snack.open)
      .toHaveBeenCalledWith('État incompatible — rechargez la page.', 'OK', { duration: 4000 });
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('ferme sans rien envoyer à l’annulation', () => {
    build();
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(svc.enroll).not.toHaveBeenCalled();
  });
});
