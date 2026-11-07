import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaService } from '../../capa.service';
import {
  CapaCaseResponse, EffectivenessReview, EvaluationBoard, EvaluationRow
} from '../../capa.types';
import { CapaEvaluationBoardComponent } from './capa-evaluation-board.component';

/**
 * Le plan de travail des évaluations d'efficacité (ADR 0108).
 *
 * <p>Ce que ce banc protège, dans l'ordre d'importance.
 *
 * <p><b>L'écran n'offre jamais un geste que le serveur refuse.</b> Le serveur
 * ouvre l'évaluation dès que les actions sont faites, mais refuse le verdict si
 * aucun critère n'a été posé : deux règles, et le drapeau `evaluable` ne porte
 * que la première. S'en tenir à lui afficherait un formulaire dont
 * l'enregistrement échouerait — la pire façon d'apprendre la règle.
 *
 * <p><b>Le verdict annonce sa conséquence avant le clic.</b> « Enregistré »
 * laisserait l'utilisateur découvrir plus tard que son dossier s'est rouvert.
 *
 * <p><b>Changer de ligne vide le formulaire.</b> Un relevé saisi pour un dossier
 * et enregistré sur un autre inscrirait au registre une mesure qui n'a jamais
 * été prise.
 */
describe('CapaEvaluationBoardComponent', () => {

  const review = (over: Partial<EffectivenessReview> = {}): EffectivenessReview => ({
    id: 'r-1', verdict: 'PARTIAL', criterion: '≤ 2 ‰ sur 3 mois',
    baseline: '7,4 ‰', target: '≤ 2 ‰', observed: '3,1 ‰',
    comment: 'Réduit sans atteindre la cible.', reviewerName: 'A. Diallo',
    reviewedAt: '2026-11-01T10:00:00Z', ...over
  });

  const row = (over: Partial<EvaluationRow> = {}): EvaluationRow => ({
    capaId: 'c-1', reference: 'NC-2026-0024', title: 'Référence erronée',
    criticity: 'HIGH', ownerName: 'M. Alaoui',
    stage: 'AWAITING_EVALUATION', evaluable: true,
    actionsTotal: 1, actionsDone: 1,
    criterion: 'Zéro erreur de référence sur 90 jours',
    reviewCount: 0, closureBlocked: false, containmentOnly: false, openNcCount: 0,
    updatedAt: '2026-11-05T09:00:00Z', ...over
  });

  const board = (over: Partial<EvaluationBoard> = {}): EvaluationBoard => ({
    effectiveRatePercent: 50, evaluated: 2, awaitingEvaluation: 1, stillOpen: 2,
    reopened: 0, partial: 0, missingCriterion: 0, total: 2, truncated: false,
    rows: [row()], ...over
  });

  /**
   * La réponse du PATCH, dont le composant ne lit rien.
   *
   * <p>Construire un `CapaCaseResponse` complet ici donnerait vingt champs que
   * personne ne vérifie : après un enregistrement, le composant recharge le
   * tableau depuis le serveur plutôt que de croire la réponse — l'étape, le
   * décompte et le dernier verdict sont calculés là-bas. C'est ce rechargement
   * que les bancs vérifient, pas le contenu de cette réponse.
   */
  const SAVED = undefined as unknown as CapaCaseResponse;

  let fixture: ComponentFixture<CapaEvaluationBoardComponent>;
  let component: CapaEvaluationBoardComponent;
  let service: jasmine.SpyObj<CapaService>;
  let snack: { open: jasmine.Spy };

  beforeEach(async () => {
    service = jasmine.createSpyObj<CapaService>('CapaService',
      ['evaluationBoard', 'recordEffectiveness']);
    snack = { open: jasmine.createSpy('open') };
    await TestBed.configureTestingModule({
      declarations: [CapaEvaluationBoardComponent],
      imports: [SharedModule, UiModule, RouterTestingModule, NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: service },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaEvaluationBoardComponent);
    component = fixture.componentInstance;
  });

  /** Charge le tableau et stabilise la vue. */
  function donne(b: EvaluationBoard): void {
    service.evaluationBoard.and.returnValue(of(b));
    fixture.detectChanges();
    tick();
    fixture.detectChanges();
  }

  // ---- lecture ---------------------------------------------------------------

  it('charge le tableau et affiche les quatre chiffres', fakeAsync(() => {
    donne(board());

    expect(service.evaluationBoard).toHaveBeenCalledTimes(1);
    const texte = fixture.nativeElement.textContent;
    expect(texte).toContain('50 %');
    expect(texte).toContain("Évaluations à faire");
    expect(component.loading).toBeFalse();
  }));

  it('laisse le taux vide plutôt que de montrer zéro quand rien n’est jugé', fakeAsync(() => {
    // Zéro pour cent se lit « tout a échoué ». Vide se lit « rien n'a encore
    // été jugé », qui est la vérité d'un client qui démarre.
    donne(board({ effectiveRatePercent: undefined, evaluated: 0 }));

    expect(fixture.nativeElement.textContent).not.toContain('0 %');
    expect(fixture.nativeElement.textContent).toContain('—');
  }));

  it('affiche un état vide explicite plutôt qu’un tableau vide', fakeAsync(() => {
    donne(board({ rows: [], total: 0 }));

    expect(fixture.nativeElement.querySelector('.empty')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.eval-table')).toBeFalsy();
  }));

  it('avertit que le tableau est borné sans laisser croire que les chiffres le sont',
    fakeAsync(() => {
      donne(board({ truncated: true, total: 250 }));

      expect(fixture.nativeElement.textContent).toContain('limite de lecture');
    }));

  it('annonce les dossiers sans critère tant qu’il est encore posable', fakeAsync(() => {
    donne(board({ missingCriterion: 3 }));

    const texte = fixture.nativeElement.textContent;
    expect(texte).toContain('3 dossiers ouverts');
    expect(texte).toContain('avant le jugement');
  }));

  it('accorde le message au singulier quand un seul dossier est concerné', fakeAsync(() => {
    donne(board({ missingCriterion: 1 }));

    expect(fixture.nativeElement.textContent).toContain("Un dossier ouvert n'a pas de critère");
  }));

  it('cesse de faire patienter quand le chargement échoue', fakeAsync(() => {
    service.evaluationBoard.and.returnValue(throwError(() => ({ status: 500 })));

    fixture.detectChanges();
    tick();

    expect(component.loading).toBeFalse();
    expect(snack.open.calls.mostRecent().args[0]).toContain('Impossible de charger');
  }));

  // ---- le choix d'une ligne --------------------------------------------------

  it('ouvre le panneau sur la ligne choisie', fakeAsync(() => {
    donne(board());
    const r = component.board!.rows[0];

    component.select(r);
    fixture.detectChanges();

    expect(component.selected).toBe(r);
    expect(fixture.nativeElement.textContent).toContain('NC-2026-0024');
  }));

  it('vide le relevé en changeant de ligne', fakeAsync(() => {
    // Un relevé saisi pour un dossier et enregistré sur un autre inscrirait au
    // registre une mesure qui n'a jamais été prise.
    donne(board({ rows: [row(), row({ capaId: 'c-2', reference: 'NC-2026-0027' })] }));
    component.select(component.board!.rows[0]);
    component.form.patchValue({ observed: '1,1 ‰' });
    component.choose('EFFECTIVE');

    component.select(component.board!.rows[1]);

    expect(component.form.value.observed).toBeFalsy();
    expect(component.verdict).toBeUndefined();
  }));

  it('reprend les bornes de l’évaluation précédente sans reprendre son relevé', fakeAsync(() => {
    // On rejuge contre les MÊMES bornes — les ressaisir de mémoire est le
    // meilleur moyen de les changer sans le vouloir. Le relevé, lui, est la
    // mesure du jour : le pré-remplir serait recopier l'ancienne.
    donne(board({ rows: [row({ lastReview: review() })] }));

    component.select(component.board!.rows[0]);

    expect(component.form.value.baseline).toBe('7,4 ‰');
    expect(component.form.value.target).toBe('≤ 2 ‰');
    expect(component.form.value.observed).toBeFalsy();
  }));

  it('ne rejoue rien quand on reclique la ligne déjà ouverte', fakeAsync(() => {
    donne(board());
    const r = component.board!.rows[0];
    component.select(r);
    component.form.patchValue({ observed: '1,1 ‰' });

    component.select(r);

    expect(component.form.value.observed).toBe('1,1 ‰');
  }));

  // ---- le verrou -------------------------------------------------------------

  it('ouvre le formulaire quand les actions sont faites ET le critère posé', fakeAsync(() => {
    donne(board());
    component.select(component.board!.rows[0]);
    fixture.detectChanges();

    expect(component.canJudge).toBeTrue();
    expect(component.lockReason).toBeUndefined();
    expect(fixture.nativeElement.querySelector('form.form')).toBeTruthy();
  }));

  it('ferme le formulaire quand le critère manque, même si le serveur l’ouvrait',
    fakeAsync(() => {
      // LE défaut que ce banc existe pour attraper : `evaluable` vaut vrai, et
      // le serveur refuserait quand même le verdict faute de critère.
      donne(board({ rows: [row({ evaluable: true, criterion: undefined })] }));
      component.select(component.board!.rows[0]);
      fixture.detectChanges();

      expect(component.canJudge).toBeFalse();
      expect(component.lockReason).toBe('NO_CRITERION');
      expect(component.criterionMissing).toBeTrue();
      expect(fixture.nativeElement.querySelector('form.form')).toBeFalsy();
      expect(fixture.nativeElement.textContent).toContain('Aucun critère');
    }));

  it('dit combien d’actions restent plutôt que « verrouillé »', fakeAsync(() => {
    donne(board({
      rows: [row({ stage: 'ACTIONS_IN_PROGRESS', evaluable: false,
        actionsTotal: 3, actionsDone: 1 })]
    }));
    component.select(component.board!.rows[0]);
    fixture.detectChanges();

    expect(component.lockReason).toBe('ACTIONS');
    expect(component.remaining).toBe(2);
    expect(fixture.nativeElement.textContent).toContain('2 actions restent à mener');
  }));

  it('distingue un dossier sans aucune action d’un dossier en cours', fakeAsync(() => {
    // Aucune action n'est un dossier vide : il n'y a rien dont juger l'effet.
    // Une action restante est un dossier qui travaille. Les confondre ferait
    // attendre un jugement qui ne viendra jamais.
    donne(board({
      rows: [row({ stage: 'ACTIONS_IN_PROGRESS', evaluable: false,
        actionsTotal: 0, actionsDone: 0 })]
    }));
    component.select(component.board!.rows[0]);
    fixture.detectChanges();

    expect(component.remaining).toBe(0);
    expect(fixture.nativeElement.textContent).toContain("Aucune action n'est encore portée");
  }));

  it('ne rejuge pas un dossier clos et le dit', fakeAsync(() => {
    donne(board({
      rows: [row({ stage: 'CLOSED_EFFECTIVE', evaluable: false,
        lastReview: review({ verdict: 'EFFECTIVE', observed: '0' }) })]
    }));
    component.select(component.board!.rows[0]);
    fixture.detectChanges();

    expect(component.lockReason).toBe('DONE');
    expect(fixture.nativeElement.textContent).toContain('clôturé');
  }));

  it('attend l’action complémentaire d’un dossier jugé partiel', fakeAsync(() => {
    donne(board({
      rows: [row({ stage: 'COMPLEMENTARY_ACTION_EXPECTED', evaluable: false,
        lastReview: review() })]
    }));
    component.select(component.board!.rows[0]);
    fixture.detectChanges();

    expect(component.lockReason).toBe('JUDGED');
    expect(fixture.nativeElement.textContent).toContain('action complémentaire');
  }));

  // ---- les obstacles à la clôture -------------------------------------------

  it('annonce l’endiguement seul avant le clic, pas après le refus', fakeAsync(() => {
    donne(board({ rows: [row({ closureBlocked: true, containmentOnly: true })] }));
    component.select(component.board!.rows[0]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("mesures d'endiguement");
  }));

  it('chiffre les écarts encore ouverts et accorde le message', fakeAsync(() => {
    donne(board({ rows: [row({ closureBlocked: true, openNcCount: 3 })] }));
    component.select(component.board!.rows[0]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('3 écarts liés');
  }));

  it('accorde au singulier un écart unique', fakeAsync(() => {
    donne(board({ rows: [row({ closureBlocked: true, openNcCount: 1 })] }));
    component.select(component.board!.rows[0]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Un écart lié est encore ouvert');
  }));

  // ---- le formulaire et l'envoi ---------------------------------------------

  it('exige un verdict ET un relevé avant d’autoriser l’envoi', fakeAsync(() => {
    donne(board());
    component.select(component.board!.rows[0]);

    expect(component.complete).toBeFalse();
    component.choose('EFFECTIVE');
    expect(component.complete).toBeFalse();
    component.form.patchValue({ observed: '0' });
    expect(component.complete).toBeTrue();
  }));

  it('refuse un relevé fait d’espaces', fakeAsync(() => {
    donne(board());
    component.select(component.board!.rows[0]);
    component.choose('EFFECTIVE');
    component.form.patchValue({ observed: '   ' });

    expect(component.complete).toBeFalse();
  }));

  it('n’envoie rien tant que le formulaire est incomplet', fakeAsync(() => {
    donne(board());
    component.select(component.board!.rows[0]);
    component.choose('PARTIAL');

    component.submit();

    expect(service.recordEffectiveness).not.toHaveBeenCalled();
  }));

  it('n’envoie rien sur un dossier que le serveur refuserait', fakeAsync(() => {
    donne(board({ rows: [row({ criterion: undefined })] }));
    component.select(component.board!.rows[0]);
    component.choose('EFFECTIVE');
    component.form.patchValue({ observed: '0' });

    component.submit();

    expect(service.recordEffectiveness).not.toHaveBeenCalled();
  }));

  it('envoie le verdict, les trois relevés et le commentaire', fakeAsync(() => {
    donne(board());
    service.recordEffectiveness.and.returnValue(of(SAVED));
    component.select(component.board!.rows[0]);
    component.choose('PARTIAL');
    component.form.patchValue({
      baseline: ' 7,4 ‰ ', target: ' ≤ 2 ‰ ', observed: ' 3,1 ‰ ', comment: ' presque '
    });

    component.submit();
    tick();

    expect(service.recordEffectiveness).toHaveBeenCalledWith('c-1', {
      verdict: 'PARTIAL', baseline: '7,4 ‰', target: '≤ 2 ‰',
      observed: '3,1 ‰', comment: 'presque'
    });
  }));

  it('laisse absents les champs vides plutôt que d’envoyer des chaînes vides', fakeAsync(() => {
    donne(board());
    service.recordEffectiveness.and.returnValue(of(SAVED));
    component.select(component.board!.rows[0]);
    component.choose('INEFFECTIVE');
    component.form.patchValue({ baseline: '', target: '  ', observed: '9 ‰', comment: '' });

    component.submit();
    tick();

    expect(service.recordEffectiveness).toHaveBeenCalledWith('c-1', {
      verdict: 'INEFFECTIVE', baseline: undefined, target: undefined,
      observed: '9 ‰', comment: undefined
    });
  }));

  it('nomme la suite et non l’acte, pour les trois verdicts', fakeAsync(() => {
    const attendu: Array<[Parameters<CapaEvaluationBoardComponent['choose']>[0], string]> = [
      ['EFFECTIVE', 'clôturé'],
      ['PARTIAL', 'action complémentaire'],
      ['INEFFECTIVE', 'rouvert']
    ];
    for (const [verdict, mot] of attendu) {
      snack.open.calls.reset();
      service.evaluationBoard.and.returnValue(of(board()));
      service.recordEffectiveness.and.returnValue(of(SAVED));
      fixture.detectChanges();
      tick();
      component.select(component.board!.rows[0]);
      component.choose(verdict);
      component.form.patchValue({ observed: 'x' });

      component.submit();
      tick();

      expect(snack.open.calls.mostRecent().args[0]).toContain(mot);
    }
  }));

  it('recharge le tableau après un enregistrement', fakeAsync(() => {
    // L'étape, le décompte et le dernier verdict sont calculés par le serveur :
    // les deviner ici afficherait une ligne qui ne correspond plus au dossier.
    donne(board());
    service.recordEffectiveness.and.returnValue(of(SAVED));
    component.select(component.board!.rows[0]);
    component.choose('EFFECTIVE');
    component.form.patchValue({ observed: '0' });

    component.submit();
    tick();

    expect(service.evaluationBoard).toHaveBeenCalledTimes(2);
    expect(component.saving).toBeFalse();
  }));

  it('rouvre la MÊME ligne après rechargement, à jour', fakeAsync(() => {
    // Garder l'objet d'avant afficherait l'ancienne étape dans le panneau, juste
    // à côté du tableau qui montre la nouvelle.
    donne(board());
    component.select(component.board!.rows[0]);
    const apres = board({
      rows: [row({ stage: 'CLOSED_EFFECTIVE', evaluable: false,
        lastReview: review({ verdict: 'EFFECTIVE' }) })]
    });
    service.evaluationBoard.and.returnValue(of(apres));
    service.recordEffectiveness.and.returnValue(of(SAVED));
    component.choose('EFFECTIVE');
    component.form.patchValue({ observed: '0' });

    component.submit();
    tick();

    expect(component.selected?.stage).toBe('CLOSED_EFFECTIVE');
    expect(component.lockReason).toBe('DONE');
  }));

  it('oublie la ligne si elle a quitté le périmètre', fakeAsync(() => {
    donne(board());
    component.select(component.board!.rows[0]);
    service.evaluationBoard.and.returnValue(of(board({ rows: [], total: 0 })));

    component.reload();
    tick();

    expect(component.selected).toBeUndefined();
    expect(component.verdict).toBeUndefined();
  }));

  it('cesse de faire patienter quand l’enregistrement échoue', fakeAsync(() => {
    donne(board());
    service.recordEffectiveness.and.returnValue(throwError(() => ({ status: 422 })));
    component.select(component.board!.rows[0]);
    component.choose('EFFECTIVE');
    component.form.patchValue({ observed: '0' });

    component.submit();
    tick();

    expect(component.saving).toBeFalse();
    expect(snack.open.calls.mostRecent().args[0]).toContain("n'a pas pu être enregistrée");
  }));

  it('n’envoie pas deux fois sur un double clic', fakeAsync(() => {
    donne(board());
    service.recordEffectiveness.and.returnValue(of(SAVED));
    component.select(component.board!.rows[0]);
    component.choose('EFFECTIVE');
    component.form.patchValue({ observed: '0' });
    component.saving = true;

    component.submit();

    expect(service.recordEffectiveness).not.toHaveBeenCalled();
  }));

  it('n’envoie rien sans ligne ouverte', () => {
    component.choose('EFFECTIVE');
    component.submit();

    expect(service.recordEffectiveness).not.toHaveBeenCalled();
  });

  // ---- les tons --------------------------------------------------------------

  it('donne un ton par étape, aligné sur le sens du mot', () => {
    expect(component.stageClass('CLOSED_EFFECTIVE')).toBe('tone-ok');
    expect(component.stageClass('AWAITING_EVALUATION')).toBe('tone-info');
    expect(component.stageClass('COMPLEMENTARY_ACTION_EXPECTED')).toBe('tone-warn');
    expect(component.stageClass('REOPENED')).toBe('tone-bad');
    expect(component.stageClass('ACTIONS_IN_PROGRESS')).toBe('tone-idle');
  });

  it('ne colore en vert que le verdict qui clôt', () => {
    expect(component.verdictClass('EFFECTIVE')).toBe('tone-ok');
    expect(component.verdictClass('PARTIAL')).toBe('tone-warn');
    expect(component.verdictClass('INEFFECTIVE')).toBe('tone-bad');
  });

  it('affiche le critère contre lequel le verdict a été rendu, pas celui du dossier',
    fakeAsync(() => {
      // Le dossier peut porter un critère reformulé ; l'évaluation garde le sien.
      donne(board({
        rows: [row({ stage: 'CLOSED_EFFECTIVE', evaluable: false,
          criterion: 'un critère reformulé depuis',
          lastReview: review({ criterion: 'le critère du jour du jugement' }) })]
      }));
      component.select(component.board!.rows[0]);
      fixture.detectChanges();

      expect(fixture.nativeElement.textContent).toContain('le critère du jour du jugement');
    }));

  it('dit qu’un dossier n’a jamais été évalué au lieu de montrer un vide', fakeAsync(() => {
    donne(board());
    component.select(component.board!.rows[0]);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain("n'a jamais été évalué");
  }));
});
