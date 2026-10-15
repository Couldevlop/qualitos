import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { DirectoryService } from '../../../../core/directory/directory.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { UserPickerModule } from '../../../../shared/user-picker/user-picker.module';
import { CirclesService } from '../../circles.service';
import { CircleMemberResponse } from '../../circles.types';
import { CirclesMemberDialogComponent, CirclesMemberDialogData } from './circles-member-dialog.component';

/**
 * Un membre de cercle est une personne de l'annuaire (ADR 0092) : on la
 * choisit par son nom, jamais en tapant un identifiant ; ceux qui sont déjà
 * membres ne sont pas proposés une seconde fois.
 */
describe('CirclesMemberDialogComponent', () => {
  const AMINA: DirectoryUser = {
    id: 'i-1', accountId: 'aaaaaaaa-1111-4111-8111-111111111111',
    displayName: 'Amina Dridi', email: 'amina@client.test', active: true
  };
  const KARIM: DirectoryUser = {
    id: 'i-2', accountId: 'bbbbbbbb-2222-4222-8222-222222222222',
    displayName: 'Karim Haddad', email: 'karim@client.test', active: true
  };

  let fixture: ComponentFixture<CirclesMemberDialogComponent>;
  let component: CirclesMemberDialogComponent;
  let circles: jasmine.SpyObj<CirclesService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<CirclesMemberDialogComponent, CircleMemberResponse>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function build(data: CirclesMemberDialogData): Promise<void> {
    circles = jasmine.createSpyObj<CirclesService>('CirclesService', ['addMember']);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    const directory = jasmine.createSpyObj<DirectoryService>('DirectoryService', ['search', 'getByAccount']);
    directory.search.and.returnValue(of({ content: [AMINA, KARIM], page: 0, size: 20, totalElements: 2, totalPages: 1 }));
    directory.getByAccount.and.returnValue(throwError(() => ({ status: 404 })));

    await TestBed.configureTestingModule({
      declarations: [CirclesMemberDialogComponent],
      imports: [SharedModule, UiModule, UserPickerModule, NoopAnimationsModule],
      providers: [
        { provide: CirclesService, useValue: circles },
        { provide: DirectoryService, useValue: directory },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(CirclesMemberDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('n\'a plus de champ où taper un identifiant : la personne se choisit dans l\'annuaire', async () => {
    await build({ circleId: 'c-1' });

    const root = fixture.nativeElement as HTMLElement;
    expect(root.querySelector('[data-test="circle-member"]')).not.toBeNull();
    expect(root.textContent).not.toContain('UUID');
    expect(component.form.valid).toBeFalse();
  });

  it('ajoute la personne choisie avec son rôle', async () => {
    await build({ circleId: 'c-1' });
    const member: CircleMemberResponse = { id: 'm-1', userId: KARIM.accountId, role: 'SECRETARY', joinedAt: '2026-10-01' } as CircleMemberResponse;
    circles.addMember.and.returnValue(of(member));

    component.form.patchValue({ userId: KARIM.accountId, role: 'SECRETARY' });
    component.submit();

    expect(circles.addMember).toHaveBeenCalledWith('c-1', { userId: KARIM.accountId, role: 'SECRETARY' });
    expect(dialogRef.close).toHaveBeenCalledWith(member);
    expect(snack.open).toHaveBeenCalled();
  });

  it('ne propose pas une seconde fois ceux qui sont déjà membres', async () => {
    await build({ circleId: 'c-1', memberIds: [AMINA.accountId] });

    const picker = fixture.debugElement.query(sel => sel.name === 'qos-user-picker');
    expect(picker.componentInstance.excludeIds).toEqual([AMINA.accountId]);
  });

  it('n\'envoie rien sans personne choisie, et signale un échec du serveur', async () => {
    await build({ circleId: 'c-1' });
    component.submit();
    expect(circles.addMember).not.toHaveBeenCalled();

    circles.addMember.and.returnValue(throwError(() => ({ status: 409 })));
    component.form.patchValue({ userId: AMINA.accountId });
    component.submit();
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('se ferme sans rien rendre à l\'annulation', async () => {
    await build({ circleId: 'c-1' });
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });
});
