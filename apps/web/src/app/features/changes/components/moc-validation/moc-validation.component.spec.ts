import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { MocService } from '../../moc.service';
import { MocDetail } from '../../moc.types';
import { carte, demande } from '../../testing/moc.fixtures';
import { MocValidationComponent } from './moc-validation.component';

/** La validation : phrase de synthèse, cartes, décision du titulaire, refus motivé. */
describe('MocValidationComponent', () => {
  let fixture: ComponentFixture<MocValidationComponent>;
  let component: MocValidationComponent;
  let service: jasmine.SpyObj<MocService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let emis: MocDetail[];
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const el = (t: string) => hote().querySelector(`[data-test="${t}"]`) as HTMLElement | null;

  const QUATRE = [
    carte({ id: 'v0', roleLabel: 'Responsable de service', decision: 'APPROVED', decidedByName: 'R. Martin',
      decidedAt: '2026-09-28T07:12:00Z', comment: 'OK', holders: ['r@acme.test'] }),
    carte({ id: 'v1', roleLabel: 'HSE', canDecide: true, heldByMe: true }),
    carte({ id: 'v2', roleLabel: 'Directeur technique', holders: ['t@acme.test'] }),
    carte({ id: 'v3', roleLabel: 'Directeur de site', holders: [] })
  ];

  async function setup(d: MocDetail): Promise<void> {
    service = jasmine.createSpyObj<MocService>('MocService', ['decide']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [MocValidationComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [{ provide: MocService, useValue: service }, { provide: MatSnackBar, useValue: snack }]
    }).compileComponents();
    fixture = TestBed.createComponent(MocValidationComponent);
    component = fixture.componentInstance;
    component.detail = d;
    emis = [];
    component.changed.subscribe(x => emis.push(x));
    component.ngOnChanges();
    fixture.detectChanges();
  }

  it('résume la classe et le nombre de validations, et montre chaque carte', async () => {
    await setup(demande({ status: 'IN_VALIDATION', validations: QUATRE }));
    expect(el('resume')?.textContent).toContain('Changement critique : 4 validation(s) requise(s). Un seul refus rejette la demande.');
    expect(el('etat-0')?.textContent).toContain('Approuvé');
    expect(el('etat-0')?.textContent).toContain('28/09/2026');
    expect(el('carte-0')?.textContent).toContain('Approuvé par R. Martin');
    expect(el('carte-0')?.textContent).toContain('OK');
    expect(el('carte-1')?.textContent).toContain('Titulaires : j.dupont@acme.test, m.martin@acme.test');
    expect(el('carte-2')?.textContent).toContain('En attente de : t@acme.test');
    expect(el('carte-3')?.textContent).toContain('Aucun titulaire');
    // Seul le titulaire voit les boutons.
    expect(el('approuver-1')).not.toBeNull();
    expect(el('approuver-2')).toBeNull();
  });

  it('un refus exige un commentaire ; l’approbation n’en exige pas', async () => {
    await setup(demande({ status: 'IN_VALIDATION', validations: QUATRE }));
    // Les deux boutons sont actifs d'emblée ; un refus sans motif n'est pas envoyé.
    expect((el('approuver-1') as HTMLButtonElement).disabled).toBeFalse();
    expect((el('rejeter-1') as HTMLButtonElement).disabled).toBeFalse();
    expect(el('motif-requis-1')).toBeNull();
    (el('rejeter-1') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect(service.decide).not.toHaveBeenCalled();
    expect(el('motif-requis-1')?.textContent).toContain('motivé');
    expect(document.activeElement).toBe(el('commentaire-1'));

    component.setComment(QUATRE[1], '  Coûts non chiffrés ');
    fixture.detectChanges();
    expect(el('motif-requis-1')).toBeNull();
    service.decide.and.returnValue(of(demande({ status: 'REJECTED' })));
    (el('rejeter-1') as HTMLButtonElement).click();
    expect(service.decide).toHaveBeenCalledWith('c1', 'v1', { decision: 'REJECTED', comment: 'Coûts non chiffrés' });
    expect(emis[0].status).toBe('REJECTED');

    TestBed.resetTestingModule();
    await setup(demande({ status: 'IN_VALIDATION', validations: QUATRE }));
    service.decide.and.returnValue(of(demande()));
    (el('approuver-1') as HTMLButtonElement).click();
    expect(service.decide).toHaveBeenCalledWith('c1', 'v1', { decision: 'APPROVED', comment: null });
  });

  it('un refus du serveur est expliqué (titulaire, auto-validation)', async () => {
    await setup(demande({ status: 'IN_VALIDATION', validations: QUATRE }));
    service.decide.and.returnValue(throwError(() => ({ status: 403, error: { code: 'moc.not-holder', role: 'HSE' } })));
    component.approve(QUATRE[1]);
    expect(snack.open.calls.mostRecent().args[0]).toContain('« HSE »');
    expect(component.busy).toBeNull();
  });

  it('le demandeur titulaire est prévenu qu’il ne peut pas valider', async () => {
    await setup(demande({ status: 'IN_VALIDATION', requestedByMe: true,
      validations: [carte({ id: 'v1', heldByMe: true, canDecide: false })] }));
    expect(hote().textContent).toContain('vous ne pouvez pas valider votre propre demande');
    expect(el('approuver-0')).toBeNull();
  });

  it('avant soumission : aperçu, ou validateurs encore inconnus', async () => {
    await setup(demande({ validations: [carte({ id: null, preview: true })] }));
    expect(el('apercu')).not.toBeNull();
    expect(el('etat-0')).toBeNull();
    TestBed.resetTestingModule();
    await setup(demande({ classification: 'NOT_ASSESSED', score: null, validations: [] }));
    expect(hote().textContent).toContain('une fois les huit domaines');
  });

  it('une demande close d’avant le MOC renvoie à l’historique', async () => {
    await setup(demande({ status: 'APPROVED', classification: 'NOT_ASSESSED', validations: [] }));
    expect(component.legacy).toBeTrue();
    expect(hote().textContent).toContain('antérieure au MOC');
  });

  it('une carte décidée oublie son brouillon de commentaire', async () => {
    await setup(demande({ status: 'IN_VALIDATION', validations: QUATRE }));
    component.setComment(QUATRE[1], 'brouillon');
    component.detail = demande({ status: 'IN_VALIDATION', validations: [carte({ id: 'v1', decision: 'APPROVED' })] });
    component.ngOnChanges();
    expect(component.comment(component.detail.validations[0])).toBe('');
    expect(component.comment(carte({ id: null }))).toBe('');
  });
});
