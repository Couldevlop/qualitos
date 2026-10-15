import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { MocService } from '../../moc.service';
import { MocDetail } from '../../moc.types';
import { demande, impacts } from '../../testing/moc.fixtures';
import { MocImpactComponent } from './moc-impact.component';

/** L'analyse d'impact : score, groupe radio au clavier, enregistrement du niveau et du texte, poids affiché. */
describe('MocImpactComponent', () => {
  let fixture: ComponentFixture<MocImpactComponent>;
  let component: MocImpactComponent;
  let service: jasmine.SpyObj<MocService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  let emis: MocDetail[];
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const radio = (d: string, l: string) => hote().querySelector(`[data-radio="${d}-${l}"]`) as HTMLButtonElement;

  async function setup(d: MocDetail): Promise<void> {
    service = jasmine.createSpyObj<MocService>('MocService', ['assess']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [MocImpactComponent],
      imports: [SharedModule, NoopAnimationsModule],
      providers: [{ provide: MocService, useValue: service }, { provide: MatSnackBar, useValue: snack }]
    }).compileComponents();
    fixture = TestBed.createComponent(MocImpactComponent);
    component = fixture.componentInstance;
    component.detail = d;
    emis = [];
    component.changed.subscribe(x => emis.push(x));
    component.ngOnChanges();
    fixture.detectChanges();
  }

  it('montre la classification, la barre et « 54 / 100 »', async () => {
    await setup(demande());
    expect(hote().querySelector('[data-test="classification"]')?.textContent).toContain('Critique');
    expect(hote().querySelector('[data-test="score"]')?.textContent?.trim()).toBe('54 / 100');
    expect(hote().querySelector('[role="meter"]')?.getAttribute('aria-valuenow')).toBe('54');
    expect(hote().querySelectorAll('[role="radiogroup"]').length).toBe(8);
    expect(radio('PRODUCTION', 'HIGH').getAttribute('aria-checked')).toBe('true');
    expect(radio('PRODUCTION', 'HIGH').classList).toContain('segment--choisi');
    expect(hote().querySelector('[data-test="incomplet"]')).toBeNull();
  });

  it('une analyse incomplète le dit et affiche « — / 100 »', async () => {
    await setup(demande({ score: null, classification: 'NOT_ASSESSED', impacts: impacts(['LOW', null, null, null, null, null, null, null]) }));
    expect(hote().querySelector('[data-test="score"]')?.textContent?.trim()).toBe('— / 100');
    expect(hote().querySelector('[data-test="incomplet"]')?.textContent).toContain('1 domaine(s)');
    // Le premier bouton d'un groupe sans choix reçoit la tabulation.
    expect(radio('QUALITY', 'NONE').getAttribute('tabindex')).toBe('0');
    expect(radio('QUALITY', 'LOW').getAttribute('tabindex')).toBe('-1');
  });

  it('choisir un niveau l’enregistre avec le texte saisi', async () => {
    await setup(demande());
    service.assess.and.returnValue(of(demande({ score: 58 })));
    component.noteInput('SAFETY', '  Port des EPI  ');
    radio('SAFETY', 'HIGH').click();
    expect(service.assess).toHaveBeenCalledWith('c1', 'SAFETY', { level: 'HIGH', notes: 'Port des EPI' });
    expect(emis[0].score).toBe(58);
  });

  it('le texte s’enregistre en quittant le champ, seulement s’il a changé et qu’un niveau existe', async () => {
    await setup(demande({ impacts: impacts(['LOW', null, 'LOW', 'HIGH', 'MEDIUM', 'MEDIUM', 'NONE', 'MEDIUM']) }));
    service.assess.and.returnValue(of(demande()));
    const safety = component.detail.impacts[0];
    component.saveNotes(safety);
    expect(service.assess).not.toHaveBeenCalled();
    component.noteInput('SAFETY', 'Nouveau texte');
    component.saveNotes(safety);
    expect(service.assess).toHaveBeenCalledWith('c1', 'SAFETY', { level: 'LOW', notes: 'Nouveau texte' });
    service.assess.calls.reset();
    component.noteInput('HEALTH_ENVIRONMENT', 'sans niveau');
    component.saveNotes(component.detail.impacts[1]);
    expect(service.assess).not.toHaveBeenCalled();
    // Le texte en cours n'est pas écrasé par une relecture.
    component.ngOnChanges();
    expect(component.notes.HEALTH_ENVIRONMENT).toBe('sans niveau');
  });

  it('les flèches parcourent le groupe et choisissent', async () => {
    await setup(demande());
    service.assess.and.returnValue(of(demande()));
    const safety = component.detail.impacts[0]; // MEDIUM
    component.onKey(new KeyboardEvent('keydown', { key: 'ArrowRight' }), safety, 2);
    expect(service.assess.calls.mostRecent().args[2].level).toBe('HIGH');
    component.onKey(new KeyboardEvent('keydown', { key: 'ArrowRight' }), safety, 3);
    expect(service.assess.calls.mostRecent().args[2].level).toBe('NONE');
    component.onKey(new KeyboardEvent('keydown', { key: 'ArrowLeft' }), safety, 0);
    expect(service.assess.calls.mostRecent().args[2].level).toBe('HIGH');
    component.onKey(new KeyboardEvent('keydown', { key: 'Home' }), safety, 3);
    expect(service.assess.calls.mostRecent().args[2].level).toBe('NONE');
    component.onKey(new KeyboardEvent('keydown', { key: 'End' }), safety, 0);
    expect(service.assess.calls.mostRecent().args[2].level).toBe('HIGH');
    const n = service.assess.calls.count();
    component.onKey(new KeyboardEvent('keydown', { key: 'a' }), safety, 0);
    expect(service.assess.calls.count()).toBe(n);
  });

  it('hors analyse, ou sans conduire la demande, tout est en lecture', async () => {
    await setup(demande({ status: 'IN_VALIDATION', impacts: impacts().map(i => ({ ...i, notes: 'EPI' })) }));
    expect(radio('SAFETY', 'HIGH').disabled).toBeTrue();
    expect(hote().querySelector('textarea')).toBeNull();
    expect(hote().querySelector('.domaine__lecture')?.textContent).toContain('EPI');
    component.setLevel(component.detail.impacts[0], 'HIGH');
    expect(service.assess).not.toHaveBeenCalled();
    TestBed.resetTestingModule();
    await setup(demande({ canManage: false }));
    expect(component.editable).toBeFalse();
  });

  it('affiche le poids d’un domaine quand il diffère de 1', async () => {
    const d = demande();
    d.impacts[0] = { ...d.impacts[0], weight: 3 };
    d.impacts[1] = { ...d.impacts[1], weight: 0 };
    await setup(d);
    expect(hote().querySelector('[data-test="poids-SAFETY"]')?.textContent).toContain('×3');
    expect(hote().querySelector('[data-test="poids-HEALTH_ENVIRONMENT"]')?.textContent).toContain('×0');
    expect(hote().querySelector('[data-test="poids-QUALITY"]')).toBeNull();
  });

  it('un échec d’enregistrement est expliqué', async () => {
    await setup(demande());
    service.assess.and.returnValue(throwError(() => ({ status: 403, error: { code: 'moc.not-owner' } })));
    component.setLevel(component.detail.impacts[0], 'HIGH');
    expect(snack.open.calls.mostRecent().args[0]).toContain('demandeur ou un gestionnaire');
    expect(component.isSaving('SAFETY')).toBeFalse();
  });
});
