import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormBuilder } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { SuppliersService } from '../../suppliers.service';
import { NonConformityResponse } from '../../suppliers.types';
import { SuppliersNcDialogComponent } from './suppliers-nc-dialog.component';

describe('SuppliersNcDialogComponent', () => {
  let fixture: ComponentFixture<SuppliersNcDialogComponent>;
  let component: SuppliersNcDialogComponent;
  let svc: jasmine.SpyObj<SuppliersService>;
  let ref: jasmine.SpyObj<MatDialogRef<SuppliersNcDialogComponent, NonConformityResponse>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  beforeEach(async () => {
    svc = jasmine.createSpyObj<SuppliersService>('SuppliersService', ['addNc', 'updateNc']);
    ref = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [SuppliersNcDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: SuppliersService, useValue: svc },
        { provide: MatDialogRef, useValue: ref },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { supplierId: 's-1' } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(SuppliersNcDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  const fill = (over: Record<string, unknown> = {}) => component.form.patchValue({
    detectedOn: '2026-10-20', severity: 'CRITICAL', partNumber: '  PN-7781-A ', partQuantity: 40,
    lotReference: ' L-12 ', description: '', ...over
  });

  it('exige la référence et le nombre de pièces', () => {
    fill({ partNumber: '', partQuantity: null });
    component.submit();
    expect(component.form.controls.partNumber.hasError('required')).toBeTrue();
    expect(component.form.controls.partQuantity.hasError('required')).toBeTrue();
    expect(svc.addNc).not.toHaveBeenCalled();
  });

  it('refuse une référence faite d\'espaces et une quantité nulle, décimale ou démesurée', () => {
    fill({ partNumber: '   ' });
    expect(component.form.controls.partNumber.invalid).toBeTrue();
    [0, 1.5, 1_000_000_001].forEach(q => {
      fill({ partQuantity: q });
      expect(component.form.controls.partQuantity.invalid).withContext(String(q)).toBeTrue();
    });
    fill({ partQuantity: 1 });
    expect(component.form.controls.partQuantity.valid).toBeTrue();
  });

  it('envoie la pièce nettoyée, la quantité et le suivi du 8D, puis se ferme sur la NC', () => {
    const created = { id: 'n-1' } as NonConformityResponse;
    svc.addNc.and.returnValue(of(created));
    fill({ eightDRequested: true });

    component.submit();

    expect(svc.addNc).toHaveBeenCalledWith('s-1', {
      status: 'OPEN', detectedOn: '2026-10-20', severity: 'CRITICAL', partNumber: 'PN-7781-A', partQuantity: 40,
      eightDRequested: true, eightDReceived: false, lotReference: 'L-12', description: undefined
    });
    expect(ref.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
  });

  it('se saisit ouverte par défaut, ou déjà clôturée ou annulée', () => {
    expect(component.form.controls.status.value).toBe('OPEN');
    expect(component.statuses.map(st => st.value)).toEqual(['OPEN', 'RESOLVED', 'REJECTED']);
    expect(component.editing).toBeFalse();
    svc.addNc.and.returnValue(of({ id: 'n-2' } as NonConformityResponse));
    fill({ status: 'REJECTED' });
    component.submit();
    expect(svc.addNc.calls.mostRecent().args[1].status).toBe('REJECTED');
  });

  it('ouvre une NC existante pour la modifier, champs vidés compris', () => {
    const nc = {
      id: 'n-7', supplierId: 's-1', status: 'IN_REVIEW', detectedOn: '2026-10-01', severity: 'MINOR',
      partNumber: 'PN-1', partQuantity: 3, lotReference: 'L-1', description: 'Rayure',
      eightDRequested: true, eightDReceived: false
    } as NonConformityResponse;
    const edit = new SuppliersNcDialogComponent(TestBed.inject(FormBuilder), svc, snack, ref, { supplierId: 's-1', nc });
    expect(edit.editing).toBeTrue();
    expect(edit.form.getRawValue()).toEqual(jasmine.objectContaining({
      status: 'IN_REVIEW', partNumber: 'PN-1', partQuantity: 3, lotReference: 'L-1', eightDRequested: true
    }));
    expect(edit.statuses.map(st => st.value)).toEqual(['OPEN', 'IN_REVIEW', 'RESOLVED', 'REJECTED']);
    svc.updateNc.and.returnValue(of(nc));
    edit.form.patchValue({ status: 'RESOLVED', description: '  ' });
    edit.submit();
    expect(svc.updateNc).toHaveBeenCalledWith('s-1', 'n-7', jasmine.objectContaining({
      status: 'RESOLVED', description: '', lotReference: 'L-1', partNumber: 'PN-1'
    }));
    expect(svc.addNc).not.toHaveBeenCalled();
    expect(ref.close).toHaveBeenCalledWith(nc);
  });

  it('reste ouverte et prévient si l\'enregistrement échoue', () => {
    svc.addNc.and.returnValue(throwError(() => ({ status: 400 })));
    fill();
    component.submit();
    expect(ref.close).not.toHaveBeenCalled();
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('n\'envoie pas deux fois pendant un envoi', () => {
    fill();
    component.submitting = true;
    component.submit();
    expect(svc.addNc).not.toHaveBeenCalled();
  });

  it('se ferme sans rien sur « Annuler »', () => {
    component.cancel();
    expect(ref.close).toHaveBeenCalledWith();
  });
});
