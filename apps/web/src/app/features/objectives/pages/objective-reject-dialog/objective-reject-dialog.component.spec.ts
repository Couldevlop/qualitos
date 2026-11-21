import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ObjectiveProposal } from '../../objectives.types';
import { ObjectiveRejectDialogComponent } from './objective-reject-dialog.component';

describe('ObjectiveRejectDialogComponent — rejeter avec justification', () => {

  const PROPOSITION: ObjectiveProposal = {
    id: 'pr-1', objectiveId: 'o-1', objectiveTitle: 'Taux de NC client',
    level: 'CRITICAL', periodStart: '2026-10-01T00:00:00Z',
    periodEnd: '2026-11-01T00:00:00Z', observedValue: 900, breachPeriods: 1,
    title: 'Objectif critique : Taux de NC client', description: null,
    effectivenessCriterion: 'Le KPI « NC client » revient à ou sous 500 ppm.',
    status: 'PENDING', capaCaseId: null, rejectionReason: null,
    decidedBy: null, decidedAt: null, createdAt: '2026-11-01T00:00:00Z'
  };

  let fixture: ComponentFixture<ObjectiveRejectDialogComponent>;
  let component: ObjectiveRejectDialogComponent;
  let dialogRef: jasmine.SpyObj<MatDialogRef<ObjectiveRejectDialogComponent, string>>;

  beforeEach(async () => {
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ObjectiveRejectDialogComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { proposal: PROPOSITION } }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(ObjectiveRejectDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  /**
   * Le motif est OBLIGATOIRE : sans lui, un objectif critique sans dossier ne se
   * distingue pas d'un objectif critique que personne n'a regardé.
   */
  it('refuse de valider sans motif', () => {
    expect(component.form.invalid).toBeTrue();
    component.submit();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('refuse un motif qui n\'est que des espaces', () => {
    component.form.controls.reason.setValue('   ');
    component.submit();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('rend le motif élagué de ses espaces de bord', () => {
    component.form.controls.reason.setValue('  arrêt planifié ligne 2  ');
    component.submit();
    expect(dialogRef.close).toHaveBeenCalledWith('arrêt planifié ligne 2');
  });

  it('ferme sans motif à l\'annulation', () => {
    component.cancel();
    expect(dialogRef.close).toHaveBeenCalledWith();
  });

  /** Ce qu'on écarte est rappelé : on ne juge pas un écart qu'on ne relit plus. */
  it('rappelle la proposition et son critère verrouillé', () => {
    const rappel = (fixture.nativeElement as HTMLElement)
      .querySelector('[data-test="rejet-rappel"]');
    expect(rappel?.textContent).toContain('Taux de NC client');
    expect(rappel?.textContent).toContain('500 ppm');
  });
});
