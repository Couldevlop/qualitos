import { ComponentFixture, TestBed } from '@angular/core/testing';
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
    svc = jasmine.createSpyObj<SuppliersService>('SuppliersService', ['addNc']);
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
      detectedOn: '2026-10-20', severity: 'CRITICAL', partNumber: 'PN-7781-A', partQuantity: 40,
      eightDRequested: true, eightDReceived: false, lotReference: 'L-12', description: undefined
    });
    expect(ref.close).toHaveBeenCalledWith(created);
    expect(component.submitting).toBeFalse();
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
