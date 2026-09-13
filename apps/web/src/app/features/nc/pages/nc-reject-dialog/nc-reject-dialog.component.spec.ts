import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { NcService } from '../../nc.service';
import { NcResponse } from '../../nc.types';
import { NcRejectDialogComponent } from './nc-reject-dialog.component';

/**
 * Écarter une réclamation.
 *
 * <p>Ce que ce banc tient : le motif est obligatoire — des espaces ne suffisent
 * pas, `Validators.required` les laisserait passer — et un refus du serveur ne
 * fait pas perdre la saisie, qui a coûté à écrire.
 */
describe('NcRejectDialogComponent', () => {

  let fixture: ComponentFixture<NcRejectDialogComponent>;
  let component: NcRejectDialogComponent;
  let nc: jasmine.SpyObj<NcService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<NcRejectDialogComponent>>;

  const rejetee = { id: 'nc-1', status: 'REJECTED' } as unknown as NcResponse;

  beforeEach(async () => {
    nc = jasmine.createSpyObj<NcService>('NcService', ['reject']);
    nc.reject.and.returnValue(of(rejetee));
    dialogRef = jasmine.createSpyObj<MatDialogRef<NcRejectDialogComponent>>(
      'MatDialogRef', ['close']);

    await TestBed.configureTestingModule({
      declarations: [NcRejectDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: NcService, useValue: nc },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { ncId: 'nc-1', reference: 'NC-2026-0042' } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(NcRejectDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('rappelle la réclamation visée', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('NC-2026-0042');
  });

  it('exige un motif avant d\'appeler le serveur', () => {
    component.submit();

    expect(nc.reject).not.toHaveBeenCalled();
    expect(component.blockedReason).toContain('pas retenue');
  });

  it('refuse un motif réduit à des espaces', () => {
    // `Validators.required` laisse passer '   ' : le serveur refuserait alors un
    // motif que l'écran a cru valide.
    component.form.setValue({ reason: '    ' });

    component.submit();

    expect(nc.reject).not.toHaveBeenCalled();
  });

  it('envoie le motif rogné et rend la réclamation rejetée', () => {
    component.form.setValue({ reason: '  Hors périmètre contractuel.  ' });

    component.submit();

    expect(nc.reject).toHaveBeenCalledWith('nc-1', { reason: 'Hors périmètre contractuel.' });
    expect(dialogRef.close).toHaveBeenCalledWith(rejetee);
  });

  it('garde la saisie quand le serveur refuse la transition', () => {
    nc.reject.and.returnValue(throwError(() => ({ status: 409, error: { detail: 'trop tard' } })));
    component.form.setValue({ reason: 'Constat non reproduit.' });

    component.submit();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.form.getRawValue().reason).toBe('Constat non reproduit.');
    expect(component.submitting).toBeFalse();
  });
});
