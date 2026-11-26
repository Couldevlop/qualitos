import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { NcService } from '../../nc.service';
import { NcResponse } from '../../nc.types';
import { NcSendBackDialogComponent } from './nc-send-back-dialog.component';

/**
 * LA SECONDE REPONSE DU SECOND REGARD.
 *
 * <p>Qui valide un ecart resolu n'avait qu'une reponse : clore. Si l'action
 * corrective ne repondait pas a la cause, si une preuve manquait, aucun geste
 * ne le disait -- on telephonait au pilote, et le registre gardait un ecart
 * « resolu » que personne ne cloturait.
 */
describe('NcSendBackDialogComponent', () => {

  let fixture: ComponentFixture<NcSendBackDialogComponent>;
  let component: NcSendBackDialogComponent;
  let svc: jasmine.SpyObj<NcService>;
  let ref: jasmine.SpyObj<MatDialogRef<NcSendBackDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  const ECART = {
    id: 'nc-1', reference: 'NC-2026-0007', title: 'Fuite au presse-étoupe',
    category: 'PROCESS', severity: 'MAJOR', status: 'ACTION_DEFINED',
    origin: 'INTERNAL', detectedAt: '', createdAt: '', updatedAt: ''
  } as NcResponse;

  beforeEach(async () => {
    svc = jasmine.createSpyObj<NcService>('NcService', ['sendBack']);
    ref = jasmine.createSpyObj('MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [NcSendBackDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: NcService, useValue: svc },
        { provide: MatDialogRef, useValue: ref },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { ncId: 'nc-1', reference: 'NC-2026-0007' } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(NcSendBackDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('nomme l’écart renvoyé', () => {
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('NC-2026-0007');
  });

  /**
   * LE MESSAGE EST FACULTATIF, et c'est un choix : l'exiger decouragerait le
   * renvoi lui-meme, et un renvoi sans phrase vaut mieux qu'une cloture de
   * complaisance.
   */
  it('renvoie sans message, et rend l’écart redevenu modifiable', () => {
    svc.sendBack.and.returnValue(of(ECART));

    component.submit();

    expect(svc.sendBack).toHaveBeenCalledWith('nc-1', { message: '' });
    expect(ref.close).toHaveBeenCalledWith(ECART);
    expect(snack.open).toHaveBeenCalled();
    expect(component.submitting).toBeFalse();
  });

  it('rogne le message avant de l’envoyer', () => {
    svc.sendBack.and.returnValue(of(ECART));
    component.form.controls.message.setValue("  La preuve d'efficacité manque.  ");

    component.submit();

    expect(svc.sendBack).toHaveBeenCalledWith(
      'nc-1', { message: "La preuve d'efficacité manque." });
  });

  /** Deux mille caracteres au plus, comme au serveur : au-dela, rien ne part. */
  it('ne renvoie rien quand le message dépasse la limite', () => {
    component.form.controls.message.setValue('x'.repeat(2001));

    component.submit();

    expect(svc.sendBack).not.toHaveBeenCalled();
    expect(component.form.controls.message.touched).toBeTrue();
  });

  /** Un second clic pendant l'envoi ne renverrait pas deux fois l'ecart. */
  it('ignore un second envoi tant que le premier court', () => {
    svc.sendBack.and.returnValue(of(ECART));
    component.submitting = true;

    component.submit();

    expect(svc.sendBack).not.toHaveBeenCalled();
  });

  /**
   * LA FENETRE RESTE OUVERTE sur un refus : le message ecrit ne se perd pas
   * parce que la transition a ete refusee.
   */
  it('garde la fenêtre ouverte quand le renvoi est refusé', () => {
    svc.sendBack.and.returnValue(throwError(() => new HttpErrorResponse({ status: 409 })));
    component.form.controls.message.setValue('À revoir');

    component.submit();

    expect(snack.open).toHaveBeenCalled();
    expect(ref.close).not.toHaveBeenCalled();
    expect(component.form.controls.message.value).toBe('À revoir');
    expect(component.submitting).toBeFalse();
  });

  it('se ferme sans rien renvoyer quand on abandonne', () => {
    component.cancel();

    expect(ref.close).toHaveBeenCalledWith();
    expect(svc.sendBack).not.toHaveBeenCalled();
  });
});
