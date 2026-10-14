import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { MocService } from '../../moc.service';
import { demande } from '../../testing/moc.fixtures';
import { MocRequestDialogComponent, MocRequestDialogData } from './moc-request-dialog.component';

/** La saisie d'une demande : création, modification pré-remplie, titre obligatoire, champs vides envoyés nuls. */
describe('MocRequestDialogComponent', () => {
  let fixture: ComponentFixture<MocRequestDialogComponent>;
  let component: MocRequestDialogComponent;
  let service: jasmine.SpyObj<MocService>;
  let ref: jasmine.SpyObj<MatDialogRef<MocRequestDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function setup(data: MocRequestDialogData): Promise<void> {
    service = jasmine.createSpyObj<MocService>('MocService', ['create', 'update']);
    ref = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [MocRequestDialogComponent],
      imports: [SharedModule, UiModule, MatButtonToggleModule, NoopAnimationsModule],
      providers: [
        { provide: MocService, useValue: service },
        { provide: MatDialogRef, useValue: ref },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(MocRequestDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('crée une demande : pas de titre, pas d’envoi ; les champs vides partent nuls', async () => {
    await setup({});
    expect(component.editing).toBeFalse();
    expect(component.dialogTitle).toBe('Nouvelle demande de changement');
    expect(component.blockedReason).toContain('titre');
    component.submit();
    expect(service.create).not.toHaveBeenCalled();

    component.form.patchValue({ title: '  Remplacement du four  ', type: 'TEMPORARY', site: '  ' });
    expect(component.blockedReason).toBeUndefined();
    const creee = demande({ code: 'MOC-2026-015' });
    service.create.and.returnValue(of(creee));
    component.submit();
    expect(service.create).toHaveBeenCalledWith({
      title: 'Remplacement du four', type: 'TEMPORARY', site: null, description: null, justification: null, plannedFor: null
    });
    expect(ref.close).toHaveBeenCalledWith(creee);
    expect(snack.open.calls.mostRecent().args[0]).toContain('MOC-2026-015');
  });

  it('modifie une demande, pré-remplie', async () => {
    const d = demande({ plannedFor: '2026-12-01' });
    await setup({ detail: d });
    expect(component.editing).toBeTrue();
    expect(component.dialogTitle).toContain('MOC-2026-014');
    expect(component.form.getRawValue()).toEqual(jasmine.objectContaining({
      title: 'Remplacement du four 3', site: 'Atelier 2', plannedFor: '2026-12-01'
    }));
    service.update.and.returnValue(of(d));
    component.submit();
    expect(service.update).toHaveBeenCalledWith('c1', jasmine.objectContaining({ plannedFor: '2026-12-01' }));
    expect(ref.close).toHaveBeenCalledWith(d);
  });

  it('un échec garde le dialogue ouvert et l’explique ; annuler le ferme', async () => {
    await setup({});
    component.form.patchValue({ title: 'Four' });
    service.create.and.returnValue(throwError(() => ({ status: 403 })));
    component.submit();
    expect(ref.close).not.toHaveBeenCalled();
    expect(component.saving).toBeFalse();
    expect(snack.open).toHaveBeenCalled();
    component.cancel();
    expect(ref.close).toHaveBeenCalledWith();
  });
});
