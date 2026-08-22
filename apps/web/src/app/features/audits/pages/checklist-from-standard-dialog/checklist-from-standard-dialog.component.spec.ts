import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { StandardsService } from '../../../standards/standards.service';
import { StandardsPage, StandardSummary } from '../../../standards/standards.types';
import { AuditsService } from '../../audits.service';
import { ChecklistItemResponse } from '../../audits.types';
import { ChecklistFromStandardDialogComponent } from './checklist-from-standard-dialog.component';

/**
 * Génération de la checklist d'un audit depuis un référentiel (§8).
 *
 * Le catalogue propose les procédures internes COMME les normes livrées :
 * auditer sa propre procédure et auditer une ISO se pilotent de la même façon.
 */
describe('ChecklistFromStandardDialogComponent', () => {
  let fixture: ComponentFixture<ChecklistFromStandardDialogComponent>;
  let component: ChecklistFromStandardDialogComponent;
  let standards: jasmine.SpyObj<StandardsService>;
  let audits: jasmine.SpyObj<AuditsService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ChecklistFromStandardDialogComponent>>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let el: HTMLElement;

  function summary(over: Partial<StandardSummary> = {}): StandardSummary {
    return {
      id: 's1', code: 'PRO-002', fullName: 'Procédure d\'audit interne',
      currentVersion: 'v3', status: 'PUBLISHED', owned: true, ...over
    } as StandardSummary;
  }

  function page(content: StandardSummary[]): StandardsPage {
    return { content, totalElements: content.length, totalPages: 1, number: 0, size: content.length };
  }

  beforeEach(async () => {
    standards = jasmine.createSpyObj<StandardsService>('StandardsService', ['listCatalog']);
    standards.listCatalog.and.returnValue(of(page([summary(), summary({ id: 's2', code: 'iso-9001', owned: false })])));
    audits = jasmine.createSpyObj<AuditsService>('AuditsService', ['generateChecklistFromStandard']);
    audits.generateChecklistFromStandard.and.returnValue(
      of([{ id: 'i1' } as ChecklistItemResponse, { id: 'i2' } as ChecklistItemResponse]));
    dialogRef = jasmine.createSpyObj<MatDialogRef<ChecklistFromStandardDialogComponent>>(
      'MatDialogRef', ['close']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    await TestBed.configureTestingModule({
      declarations: [ChecklistFromStandardDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: StandardsService, useValue: standards },
        { provide: AuditsService, useValue: audits },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MatSnackBar, useValue: snack },
        { provide: MAT_DIALOG_DATA, useValue: { planId: 'p1' } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ChecklistFromStandardDialogComponent);
    component = fixture.componentInstance;
    el = fixture.nativeElement as HTMLElement;
    fixture.detectChanges();
  });

  it('propose les procédures internes comme les normes livrées', done => {
    component.standards$.subscribe(list => {
      expect(list.map(s => s.id)).toEqual(['s1', 's2']);
      done();
    });
  });

  it('n\'envoie rien tant qu\'aucun référentiel n\'est choisi', () => {
    component.submit();

    expect(audits.generateChecklistFromStandard).not.toHaveBeenCalled();
    expect(component.form.controls.standardId.touched).toBeTrue();
  });

  it('génère la checklist et rend le nombre de questions produites', () => {
    component.form.setValue({ standardId: 's1' });

    component.submit();

    expect(audits.generateChecklistFromStandard).toHaveBeenCalledWith('p1', 's1');
    expect(dialogRef.close).toHaveBeenCalledWith(2);
    expect(component.submitting).toBeFalse();
  });

  it('annonce un référentiel encore vide sans le présenter comme une panne', () => {
    audits.generateChecklistFromStandard.and.returnValue(of([]));
    component.form.setValue({ standardId: 's1' });

    component.submit();

    expect(snack.open.calls.mostRecent().args[0]).toContain('aucune exigence');
    // Refermé quand même : l'audit vise désormais ce référentiel, la fiche a changé.
    expect(dialogRef.close).toHaveBeenCalledWith(0);
  });

  it('dit quoi faire quand le catalogue est vide', () => {
    standards.listCatalog.and.returnValue(of(page([])));
    component.ngOnInit();
    fixture.detectChanges();

    expect(el.querySelector('.empty-state')!.textContent).toContain('Adoptez une norme');
  });

  [
    { status: 409, expected: 'n\'est pas vide' },
    { status: 404, expected: 'introuvable' },
    { status: 500, expected: 'impossible' }
  ].forEach(({ status, expected }) => {
    it(`explique le refus ${status}`, () => {
      audits.generateChecklistFromStandard.and.returnValue(throwError(() => ({ status })));
      component.form.setValue({ standardId: 's1' });

      component.submit();

      expect(snack.open.calls.mostRecent().args[0]).toContain(expected);
      expect(dialogRef.close).not.toHaveBeenCalled();
      expect(component.submitting).toBeFalse();
    });
  });

  it('referme sans rien générer à l\'annulation', () => {
    component.cancel();

    expect(dialogRef.close).toHaveBeenCalledWith();
    expect(audits.generateChecklistFromStandard).not.toHaveBeenCalled();
  });
});
