import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of } from 'rxjs';

import { AuthService } from '../../core/auth/auth.service';
import { AddStandardDialogComponent } from './pages/add-standard-dialog/add-standard-dialog.component';
import { SmiStandardsOpener, TENANT_ADMIN_ROLES } from './smi-standards-opener.service';
import { StandardRef } from './smi.types';

describe('SmiStandardsOpener', () => {

  let auth: jasmine.SpyObj<AuthService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let opener: SmiStandardsOpener;

  beforeEach(() => {
    auth = jasmine.createSpyObj<AuthService>('AuthService', ['hasAnyRole']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: auth },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    });
    opener = TestBed.inject(SmiStandardsOpener);
  });

  function fermeture(result: StandardRef | undefined): void {
    dialog.open.and.returnValue({ afterClosed: () => of(result) } as MatDialogRef<AddStandardDialogComponent, StandardRef>);
  }

  it('seul l’administrateur du client, alias compris, peut ajouter une norme', () => {
    auth.hasAnyRole.and.returnValue(true);
    expect(opener.canAdd()).toBeTrue();
    expect(auth.hasAnyRole).toHaveBeenCalledWith(TENANT_ADMIN_ROLES);
    expect(TENANT_ADMIN_ROLES).toEqual(['admin_tenant', 'admin', 'tenant_admin']);
    // Ni manager qualité ni super administrateur : le paramétrage revient au client.
    expect(TENANT_ADMIN_ROLES).not.toContain('quality_manager');
    expect(TENANT_ADMIN_ROLES).not.toContain('super_admin');

    auth.hasAnyRole.and.returnValue(false);
    expect(opener.canAdd()).toBeFalse();
  });

  it('ouvre la fenêtre et annonce la norme ajoutée sous son nom court', () => {
    fermeture({ adoptionId: 'a', code: 'iso-14001', name: 'ISO 14001:2015 — Systèmes de management environnemental' });
    let rendu: StandardRef | undefined;

    opener.open().subscribe(r => (rendu = r));

    expect(dialog.open).toHaveBeenCalledWith(AddStandardDialogComponent,
      jasmine.objectContaining({ panelClass: 'qos-dialog-panel' }));
    expect(rendu!.code).toBe('iso-14001');
    expect(snack.open.calls.mostRecent().args[0]).toContain('ISO 14001:2015 ajoutée au SMI');
  });

  it('fermée sans rien ajouter, elle ne dit rien', () => {
    fermeture(undefined);
    let rendu: StandardRef | undefined = { adoptionId: 'x', code: 'x', name: 'x' };

    opener.open().subscribe(r => (rendu = r));

    expect(rendu).toBeUndefined();
    expect(snack.open).not.toHaveBeenCalled();
  });
});
