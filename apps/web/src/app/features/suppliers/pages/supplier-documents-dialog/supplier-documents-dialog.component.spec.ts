import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { Subject } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { SupplierDocumentsDialogComponent, SupplierDocumentsDialogData } from './supplier-documents-dialog.component';

describe('SupplierDocumentsDialogComponent', () => {
  let fixture: ComponentFixture<SupplierDocumentsDialogComponent>;
  let component: SupplierDocumentsDialogComponent;
  let ref: { close: jasmine.Spy; disableClose: boolean; backdropClick: () => Subject<MouseEvent>;
             keydownEvents: () => Subject<KeyboardEvent> };
  let backdrop: Subject<MouseEvent>;
  let keys: Subject<KeyboardEvent>;

  async function setup(data: SupplierDocumentsDialogData): Promise<void> {
    backdrop = new Subject<MouseEvent>();
    keys = new Subject<KeyboardEvent>();
    ref = { close: jasmine.createSpy('close'), disableClose: false,
            backdropClick: () => backdrop, keydownEvents: () => keys };
    await TestBed.configureTestingModule({
      declarations: [SupplierDocumentsDialogComponent],
      imports: [SharedModule],
      providers: [
        { provide: MatDialogRef, useValue: ref },
        { provide: MAT_DIALOG_DATA, useValue: data }
      ],
      // Le panneau a son propre banc.
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
    fixture = TestBed.createComponent(SupplierDocumentsDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  const NC: SupplierDocumentsDialogData = {
    supplierId: 's-1', ownerType: 'NON_CONFORMITY', ownerId: 'n-1', subject: 'NC PN-1', lockReason: null
  };

  it('titre selon le porteur et rappelle son sujet', async () => {
    await setup(NC);
    expect(component.title).toContain('non-conformité');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('NC PN-1');
  });

  it('titre « certificat » pour un certificat', async () => {
    await setup({ ...NC, ownerType: 'CERTIFICATE', subject: 'iso-9001' });
    expect(component.title).toContain('certificat');
  });

  it('se ferme sans résultat quand rien n\'a changé', async () => {
    await setup(NC);
    component.close();
    expect(ref.close).toHaveBeenCalledWith(undefined);
  });

  it('se ferme sur le nouveau nombre de documents', async () => {
    await setup(NC);
    component.changed(3);
    (fixture.nativeElement.querySelector('[data-test="fermer"]') as HTMLButtonElement).click();
    expect(ref.close).toHaveBeenCalledWith(3);
  });

  it('rend aussi le nombre quand on ferme par Échap ou par le voile', async () => {
    await setup(NC);
    expect(ref.disableClose).toBeTrue();
    component.changed(1);

    keys.next(new KeyboardEvent('keydown', { key: 'Enter' }));
    expect(ref.close).not.toHaveBeenCalled();

    keys.next(new KeyboardEvent('keydown', { key: 'Escape' }));
    expect(ref.close).toHaveBeenCalledWith(1);

    backdrop.next(new MouseEvent('click'));
    expect(ref.close).toHaveBeenCalledTimes(2);
  });
});
