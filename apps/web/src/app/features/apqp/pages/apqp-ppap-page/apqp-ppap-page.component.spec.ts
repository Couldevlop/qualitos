import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { ApqpService } from '../../apqp.service';
import { ApqpCycle, ApqpDeliverable, ApqpPhase } from '../../apqp.types';
import {
  ApqpDeliverableDetailDialogData
} from '../apqp-deliverable-detail-dialog/apqp-deliverable-detail-dialog.component';
import {
  ApqpPpapSummaryComponent
} from '../apqp-ppap-summary/apqp-ppap-summary.component';
import { ApqpPpapPageComponent } from './apqp-ppap-page.component';

/**
 * Le dossier PPAP, sur son propre écran.
 *
 * <p>Ce que ce banc tient : l'écran rend le MÊME composant que sous le schéma du
 * cycle — pas une seconde implémentation qui divergerait — et il ouvre le même
 * popup de livrable. Et un cycle sans livrable étoilé dit quoi faire au lieu de
 * laisser un blanc.
 */
describe('ApqpPpapPageComponent', () => {

  let fixture: ComponentFixture<ApqpPpapPageComponent>;
  let component: ApqpPpapPageComponent;
  let service: jasmine.SpyObj<ApqpService>;
  let dialog: jasmine.SpyObj<MatDialog>;

  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;

  function livrable(partiel: Partial<ApqpDeliverable>): ApqpDeliverable {
    return {
      id: 'd1', position: 1, label: 'MSA', ppap: true, kind: 'ATTACHMENT',
      done: false, evidenceCount: 0, ...partiel
    };
  }

  function cycle(livrables: ApqpDeliverable[], done = 0): ApqpCycle {
    const phase: ApqpPhase = {
      id: 'p4', position: 4, level: 2, title: 'Product and Process Validation',
      purpose: null, question: null, deliverables: livrables
    };
    return {
      phases: [phase],
      ppapDone: done,
      ppapTotal: livrables.filter(d => d.ppap).length
    };
  }

  async function monter(rendu: ApqpCycle, roles: string[] = ['QUALITY_MANAGER']): Promise<void> {
    service = jasmine.createSpyObj<ApqpService>('ApqpService', ['cycle']);
    service.cycle.and.returnValue(of(rendu));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    dialog.open.and.returnValue(
      { afterClosed: () => of(undefined) } as unknown as MatDialogRef<unknown>);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ApqpPpapPageComponent, ApqpPpapSummaryComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ApqpService, useValue: service },
        { provide: MatDialog, useValue: dialog },
        { provide: AuthService, useValue: { hasAnyRole: () => roles.length > 0 } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ApqpPpapPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('rend le dossier avec le compte venu du serveur', async () => {
    await monter(cycle([
      livrable({ id: 'd1', label: 'MSA', done: true }),
      livrable({ id: 'd2', label: 'First Article Inspection Report (FAIR)' }),
      livrable({ id: 'd3', label: 'Floor plan layout', ppap: false })
    ], 1));

    expect(hote().querySelectorAll('[data-test=ligne-ppap]').length).toBe(2);
    expect(hote().querySelector('[data-test=compte-ppap]')!.textContent).toContain('1');
    expect(hote().querySelector('[data-test=compte-ppap]')!.textContent).toContain('2');
  });

  it('ouvre le popup du livrable cliqué', async () => {
    await monter(cycle([livrable({ label: 'PPAP file and approval form' })]));

    hote().querySelector<HTMLButtonElement>('[data-test=ligne-ppap] button')!.click();

    expect(dialog.open).toHaveBeenCalled();
    const data = dialog.open.calls.mostRecent().args[1]!.data as ApqpDeliverableDetailDialogData;
    expect(data.deliverable.label).toBe('PPAP file and approval form');
    expect(data.editable).toBeTrue();
  });

  it('dit quoi faire quand aucun livrable n\'est marqué PPAP', async () => {
    await monter(cycle([livrable({ ppap: false, label: 'Floor plan layout' })]));

    // Un écran muet laisserait croire à un chargement qui n'aboutit pas.
    expect(hote().querySelector('[data-test=dossier-vide]')).not.toBeNull();
    expect(hote().querySelector('[data-test=section-ppap]')).toBeNull();
  });

  it('passe le droit d\'écrire au popup, plutôt que de le laisser le redéduire', async () => {
    await monter(cycle([livrable({})]), []);

    hote().querySelector<HTMLButtonElement>('[data-test=ligne-ppap] button')!.click();

    const data = dialog.open.calls.mostRecent().args[1]!.data as ApqpDeliverableDetailDialogData;
    expect(data.editable).toBeFalse();
  });

  it('survit à un refus du serveur sans écran blanc', async () => {
    service = jasmine.createSpyObj<ApqpService>('ApqpService', ['cycle']);
    service.cycle.and.returnValue(throwError(() => ({ status: 500 })));
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ApqpPpapPageComponent, ApqpPpapSummaryComponent],
      imports: [SharedModule, UiModule, NoopAnimationsModule],
      providers: [
        { provide: ApqpService, useValue: service },
        { provide: MatDialog, useValue: dialog },
        { provide: AuthService, useValue: { hasAnyRole: () => true } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ApqpPpapPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.loading).toBeFalse();
    expect(component.phases).toEqual([]);
  });
});
