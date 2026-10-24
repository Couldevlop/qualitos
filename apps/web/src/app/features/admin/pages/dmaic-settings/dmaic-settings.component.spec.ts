import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DmaicResolutionService } from '../../../dmaic/resolution/dmaic-resolution.service';
import { ResolutionSettings } from '../../../dmaic/resolution/dmaic-resolution.types';
import { DmaicSettingsComponent } from './dmaic-settings.component';

function reglage(over: Partial<ResolutionSettings> = {}): ResolutionSettings {
  return {
    defaults: true,
    questions: [
      { code: 'DEF-01', section: 'DEFINE', topic: 'WHAT', label: 'Quel est le problème ?', answerType: 'TEXT' },
      { code: 'DEF-02', section: 'DEFINE', topic: 'WHO', label: 'Qui ?', answerType: 'TEXT' },
      { code: 'MES-01', section: 'MEASURE', topic: null, label: 'Mesure vérifiée ?', answerType: 'YES_NO' }
    ],
    failureTypes: [{ code: 'V10', label: 'Dimensional issue' }],
    severities: [{ level: 1, label: 'Sévère' }, { level: 2, label: 'Modérée' }],
    ...over
  };
}

/** « Paramètres DMAIC » : lecture, édition d'une phase à la fois, règles locales, enregistrement d'un bloc, retour aux défauts. */
describe('DmaicSettingsComponent', () => {
  let fixture: ComponentFixture<DmaicSettingsComponent>;
  let component: DmaicSettingsComponent;
  let service: jasmine.SpyObj<DmaicResolutionService>;
  let snack: jasmine.SpyObj<MatSnackBar>;
  const hote = (): HTMLElement => fixture.nativeElement as HTMLElement;
  const un = (sel: string) => hote().querySelector<HTMLElement>(`[data-test="${sel}"]`);
  const tous = (sel: string) => hote().querySelectorAll<HTMLElement>(`[data-test="${sel}"]`);

  async function setup(lecture: ResolutionSettings | 'erreur' = reglage()): Promise<void> {
    service = jasmine.createSpyObj<DmaicResolutionService>('DmaicResolutionService',
      ['settings', 'replaceSettings', 'resetSettings']);
    service.settings.and.returnValue(lecture === 'erreur' ? throwError(() => ({ status: 500 })) : of(lecture));
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      declarations: [DmaicSettingsComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: DmaicResolutionService, useValue: service },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();
    fixture = TestBed.createComponent(DmaicSettingsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function confirmer(reponse: boolean): jasmine.Spy {
    return spyOn(TestBed.inject(MatDialog), 'open').and.returnValue({
      afterClosed: () => of(reponse)
    } as MatDialogRef<unknown>);
  }

  it('lit le paramétrage, montre une phase à la fois, sans rien proposer d\'enregistrer', async () => {
    await setup();
    expect(hote().textContent).toContain('Ce sont aujourd\'hui les valeurs de la plateforme');
    expect(tous('question').length).toBe(2);
    expect(un('phase-MEASURE')?.textContent).toContain('1');
    un('phase-MEASURE')!.click();
    fixture.detectChanges();
    expect(component.section).toBe('MEASURE');
    expect(tous('question').length).toBe(1);
    expect(tous('severite').length).toBe(2);
    expect(tous('defaillance').length).toBe(1);
    expect(component.dirty).toBeFalse();
    expect(un('enregistrer')).toBeNull();
    expect(un('retablir')?.hasAttribute('disabled')).toBeTrue();
    un('phase-IMPROVE')!.click();
    fixture.detectChanges();
    expect(hote().textContent).toContain('Aucune question dans cette phase.');
  });

  it('ajoute une question au code suivant de sa phase, la déplace et la retire', async () => {
    await setup();
    un('question-ajout')!.click();
    const ajoutee = component.questions[3];
    expect(ajoutee).toEqual(jasmine.objectContaining({ code: 'DEF-03', section: 'DEFINE', topic: 'WHAT', answerType: 'TEXT' }));
    expect(component.dirty).toBeTrue();
    component.section = 'MEASURE';
    component.addQuestion();
    expect(component.questions[4].code).toBe('MES-02');
    expect(component.questions[4].topic).toBeNull();

    component.moveQuestion(ajoutee, -1);
    expect(component.sectionQuestions.length).toBe(2);
    component.section = 'DEFINE';
    expect(component.sectionQuestions.map(q => q.code)).toEqual(['DEF-01', 'DEF-03', 'DEF-02']);
    component.moveQuestion(component.sectionQuestions[0], -1);
    component.moveQuestion(component.sectionQuestions[2], 1);
    expect(component.sectionQuestions.map(q => q.code)).toEqual(['DEF-01', 'DEF-03', 'DEF-02']);
    component.removeQuestion(ajoutee);
    expect(component.countOf('DEFINE')).toBe(2);
  });

  it('saute un code déjà pris et s\'arrête à 150 questions', async () => {
    await setup(reglage({ questions: [
      { code: 'CTL-02', section: 'CONTROL', topic: null, label: 'x', answerType: 'TEXT' }
    ] }));
    component.section = 'CONTROL';
    component.addQuestion();
    expect(component.questions[1].code).toBe('CTL-03');
    while (component.questions.length < 160) {
      const avant = component.questions.length;
      component.addQuestion();
      if (component.questions.length === avant) break;
    }
    expect(component.questions.length).toBe(150);
  });

  it('dit en clair ce qui empêche d\'enregistrer', async () => {
    await setup();
    component.questions[0].code = 'déf 01';
    component.questions[1].code = 'mes-01';
    component.addFailureType();
    component.failureTypes[1].code = 'V10';
    component.severities[1].level = 1;
    fixture.detectChanges();
    expect(component.problems).toEqual([
      'Un code fait 1 à 20 caractères : lettres, chiffres, point, tiret ou soulignement.',
      'Deux questions portent le même code.',
      'Deux types de défaillance portent le même code.',
      'Chaque ligne a besoin d\'un libellé.',
      'Chaque sévérité a son propre niveau, de 1 à 10.'
    ]);
    expect(un('problemes')).not.toBeNull();
    expect(un('enregistrer')?.hasAttribute('disabled')).toBeTrue();
    component.save();
    expect(service.replaceSettings).not.toHaveBeenCalled();

    component.questions = [];
    component.severities = [];
    expect(component.problems).toContain('Gardez au moins une question.');
    expect(component.problems).toContain('Gardez au moins un niveau de sévérité.');
  });

  it('ajoute les sévérités au niveau suivant, dix au plus, et les types de défaillance, trois cents au plus', async () => {
    await setup();
    un('severite-ajout')!.click();
    expect(component.severities[2].level).toBe(3);
    for (let i = 0; i < 12; i++) component.addSeverity();
    expect(component.severities.length).toBe(10);
    component.removeSeverity(component.severities[9]);
    expect(component.severities.length).toBe(9);
    component.severities = [];
    component.addSeverity();
    expect(component.severities[0].level).toBe(1);

    for (let i = 0; i < 310; i++) component.addFailureType();
    expect(component.failureTypes.length).toBe(300);
    component.removeFailureType(component.failureTypes[0]);
    expect(component.failureTypes.length).toBe(299);
  });

  it('enregistre d\'un bloc, codes en capitales et rubrique effacée hors « Définir »', async () => {
    await setup();
    component.questions[0].label = '  Quel est le problème, vraiment ?  ';
    component.questions[2].topic = 'WHERE';
    component.failureTypes[0].code = ' v10 ';
    service.replaceSettings.and.returnValue(of(reglage({ defaults: false })));
    fixture.detectChanges();
    un('enregistrer')!.click();
    const [questions, defaillances, severites] = service.replaceSettings.calls.mostRecent().args;
    expect(questions[0].label).toBe('Quel est le problème, vraiment ?');
    expect(questions[2].topic).toBeNull();
    expect(defaillances[0].code).toBe('V10');
    expect(severites.length).toBe(2);
    expect(component.defaults).toBeFalse();
    expect(component.dirty).toBeFalse();
    expect(snack.open).toHaveBeenCalledWith('Paramètres DMAIC enregistrés.', undefined, { duration: 4000 });
  });

  it('dit l\'échec d\'un enregistrement', async () => {
    await setup();
    component.questions[0].label = 'Autre';
    service.replaceSettings.and.returnValue(throwError(() => ({ status: 403 })));
    component.save();
    expect(component.saving).toBeFalse();
    expect(snack.open).toHaveBeenCalledWith('Vous n\'avez pas les droits pour cette action.', undefined, { duration: 5000 });
  });

  it('annule les changements', async () => {
    await setup();
    component.questions[0].label = 'Autre';
    component.removeSeverity(component.severities[0]);
    fixture.detectChanges();
    un('annuler')!.click();
    expect(component.questions[0].label).toBe('Quel est le problème ?');
    expect(component.severities.length).toBe(2);
    expect(component.dirty).toBeFalse();
  });

  it('revient aux valeurs de la plateforme après confirmation', async () => {
    await setup(reglage({ defaults: false }));
    confirmer(false);
    un('retablir')!.click();
    expect(service.resetSettings).not.toHaveBeenCalled();

    (TestBed.inject(MatDialog).open as jasmine.Spy).and.returnValue({ afterClosed: () => of(true) });
    service.resetSettings.and.returnValue(of(reglage()));
    component.reset();
    expect(service.resetSettings).toHaveBeenCalled();
    expect(component.defaults).toBeTrue();
    expect(snack.open).toHaveBeenCalledWith('Valeurs de la plateforme rétablies.', undefined, { duration: 4000 });

    service.resetSettings.and.returnValue(throwError(() => ({ status: 500 })));
    component.reset();
    expect(component.saving).toBeFalse();
  });

  it('dit l\'échec de la lecture et réessaie', async () => {
    await setup('erreur');
    expect(component.failed).toBeTrue();
    expect(hote().querySelector('[role="alert"]')).not.toBeNull();
    component.discard();
    expect(component.dirty).toBeFalse();
    service.settings.and.returnValue(of(reglage()));
    component.charger();
    expect(component.questions.length).toBe(3);
    expect(component.trackByIndex(2)).toBe(2);
    expect(component.sectionTitle('CONTAIN')).toBe('Analyser — contenir le problème');
  });
});
