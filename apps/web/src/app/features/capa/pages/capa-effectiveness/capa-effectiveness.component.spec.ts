import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { of, throwError } from 'rxjs';

import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { CapaService } from '../../capa.service';
import { CapaEffectivenessRow, CapaEffectivenessSummary } from '../../capa.types';
import {
  CapaEvaluationBoardComponent
} from '../../components/capa-evaluation-board/capa-evaluation-board.component';
import { CapaEffectivenessComponent } from './capa-effectiveness.component';

/**
 * L'écran d'efficacité mesurée.
 *
 * <p>Ce qu'il doit garantir : ne jamais afficher un taux là où le serveur a
 * refusé d'en calculer un — une fenêtre en cours ou l'absence d'occurrence
 * antérieure se disent en toutes lettres — et mettre en avant l'écart entre ce
 * qui a été déclaré à la clôture et ce que le terrain a montré.
 */
describe('CapaEffectivenessComponent', () => {

  const row = (over: Partial<CapaEffectivenessRow> = {}): CapaEffectivenessRow => ({
    capaId: 'c-1', title: 'Dérive dimensionnelle', criticity: 'MAJOR',
    closedAt: '2026-03-12T09:00:00Z', status: 'MEASURED',
    occurrencesBefore: 12, occurrencesAfter: 1, ratePercent: 92,
    aggravated: false, daysObserved: 180, daysInWindow: 180,
    declaredEffective: true, preciseMatch: true, ...over
  });

  const summary = (over: Partial<CapaEffectivenessSummary> = {}): CapaEffectivenessSummary => ({
    windowMonths: 6, measured: 1, inObservation: 0, notMeasurable: 0,
    averageRatePercent: 92, aggravated: 0, declaredButFailed: 0,
    truncated: false, rows: [row()], ...over
  });

  let fixture: ComponentFixture<CapaEffectivenessComponent>;
  let component: CapaEffectivenessComponent;
  let service: jasmine.SpyObj<CapaService>;
  let snack: { open: jasmine.Spy };
  /** Les parametres de l'adresse, que chaque banc peut poser avant le montage. */
  let parametres: Record<string, string>;

  beforeEach(async () => {
    // `evaluationBoard` est dans la liste parce que le premier onglet de l'écran
    // héberge le plan de travail des évaluations (ADR 0108) : sans elle, son
    // ngOnInit appellerait une méthode absente du mannequin et le banc tomberait
    // sur une erreur qui n'a rien à voir avec ce qu'il vérifie.
    parametres = {};
    service = jasmine.createSpyObj<CapaService>('CapaService',
      ['effectiveness', 'evaluationBoard']);
    // `effectiveness` a elle aussi une reponse par defaut : les bancs qui ne
    // s'interessent pas a la mesure par recidive montent quand meme l'ecran, et
    // son ngOnInit l'appelle. Sans ce defaut, ils tombaient sur un mannequin
    // muet -- une panne qui ne dit rien de ce qu'ils verifient.
    service.effectiveness.and.returnValue(of(summary()));
    service.evaluationBoard.and.returnValue(of({
      effectiveRatePercent: undefined, evaluated: 0, awaitingEvaluation: 0, stillOpen: 0,
      reopened: 0, partial: 0, missingCriterion: 0, total: 0, truncated: false, rows: []
    }));
    snack = { open: jasmine.createSpy('open') };
    await TestBed.configureTestingModule({
      declarations: [CapaEffectivenessComponent, CapaEvaluationBoardComponent],
      imports: [SharedModule, UiModule, MatButtonToggleModule, RouterTestingModule,
        NoopAnimationsModule],
      providers: [
        { provide: CapaService, useValue: service },
        { provide: MatSnackBar, useValue: snack },
        // L'adresse peut nommer le dossier a juger (`?capa=`) : le banc part
        // d'une adresse nue, et le cas nomme a son propre fournisseur.
        { provide: ActivatedRoute,
          useValue: {
            // Lu a la demande : un banc pose `parametres` puis monte le
            // composant lui-meme, apres que ce fournisseur a ete enregistre.
            snapshot: { get queryParamMap() { return convertToParamMap(parametres); } }
          } }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CapaEffectivenessComponent);
    component = fixture.componentInstance;
  });

  /**
   * Ouvre l'onglet « Mesure par récidive » et rend son élément racine.
   *
   * <p>Deux raisons de borner les requêtes DOM à cet élément plutôt que
   * d'interroger la page entière. D'abord la mesure vit maintenant dans le
   * second onglet : sans ce basculement, son gabarit n'est jamais rendu et
   * l'assertion échoue sur une absence qui n'est pas celle qu'elle vise.
   * Ensuite les deux onglets portent les MÊMES classes d'état — `.empty`,
   * `.notice`, `.card.alarm` — et Material garde attaché l'onglet déjà visité :
   * une requête non bornée trouverait celle du voisin et le banc passerait au
   * vert sans rien avoir vérifié.
   */
  function mesure(): HTMLElement {
    component.tab = 1;
    fixture.detectChanges();
    const body = fixture.nativeElement.querySelector('.recurrence') as HTMLElement;
    expect(body).withContext("l'onglet de mesure par récidive n'est pas rendu").toBeTruthy();
    return body;
  }

  it('demande six mois par défaut et affiche la synthèse', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary()));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(service.effectiveness).toHaveBeenCalledWith(6);
    expect(mesure().textContent).toContain('92 %');
    expect(component.loading).toBeFalse();
  }));

  it('change de fenêtre sans rejouer la même', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary()));
    fixture.detectChanges();
    tick();

    component.changeWindow(12);
    tick();
    component.changeWindow(12);
    tick();

    expect(service.effectiveness).toHaveBeenCalledWith(12);
    expect(service.effectiveness).toHaveBeenCalledTimes(2);
  }));

  it('dit qu’il est trop tôt plutôt que d’afficher un taux partiel', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary({
      measured: 0, inObservation: 1, averageRatePercent: undefined,
      rows: [row({ status: 'IN_OBSERVATION', ratePercent: undefined,
        occurrencesAfter: 0, daysObserved: 60 })]
    })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const texte = mesure().textContent ?? '';
    expect(texte).toContain('Trop tôt pour conclure');
    expect(texte).not.toContain('%');
  }));

  it('nomme l’absence d’occurrence antérieure au lieu de conclure', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary({
      measured: 0, notMeasurable: 1, averageRatePercent: undefined,
      rows: [row({ status: 'NOT_MEASURABLE', ratePercent: undefined, occurrencesBefore: 0 })]
    })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(mesure().textContent).toContain('Aucune occurrence antérieure');
  }));

  it('signale un dossier déclaré efficace que la mesure dément', fakeAsync(() => {
    const dementie = row({ ratePercent: 0, declaredEffective: true, occurrencesAfter: 12 });
    service.effectiveness.and.returnValue(of(summary({
      declaredButFailed: 1, averageRatePercent: 0, rows: [dementie]
    })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    const body = mesure();
    expect(component.contradicted(dementie)).toBeTrue();
    expect(body.querySelector('.contradicted')).toBeTruthy();
    expect(body.querySelector('.card.alarm')).toBeTruthy();
  }));

  it('avertit quand le périmètre a été tronqué', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary({ truncated: true })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(mesure().querySelector('.notice')).toBeTruthy();
  }));

  it('dit qu’un rapprochement par catégorie ne vaut qu’indication', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary({ rows: [row({ preciseMatch: false })] })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(mesure().textContent).toContain('Rapprochement par catégorie');
  }));

  it('affiche un état vide explicite quand rien n’est mesurable', fakeAsync(() => {
    service.effectiveness.and.returnValue(of(summary({
      measured: 0, averageRatePercent: undefined, rows: []
    })));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(mesure().querySelector('.empty')).toBeTruthy();
  }));

  it('cesse de faire patienter quand le chargement échoue', fakeAsync(() => {
    service.effectiveness.and.returnValue(throwError(() => ({ status: 500 })));

    fixture.detectChanges();
    tick();

    expect(component.loading).toBeFalse();
    expect(snack.open.calls.mostRecent().args[0]).toContain('Impossible de charger');
  }));

  it('arrondit les mois observés plutôt que d’afficher une fausse précision', () => {
    expect(component.observedMonths(row({ daysObserved: 59 }))).toBe(2);
    expect(component.windowMonths(row({ daysInWindow: 180 }))).toBe(6);
  });

  it('ouvre sur le plan de travail, pas sur la mesure', fakeAsync(() => {
    // L'onglet où l'on AGIT vient en premier : la mesure par récidive se
    // consulte, le jugement se rend. Ouvrir sur la seconde ferait chercher
    // l'action là où il n'y a que de la lecture.
    service.effectiveness.and.returnValue(of(summary()));

    fixture.detectChanges();
    tick();
    fixture.detectChanges();

    expect(component.tab).toBe(0);
    expect(fixture.nativeElement.querySelector('qos-capa-evaluation-board')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.recurrence')).toBeFalsy();
  }));

  it('classe le taux par palier et non par nuance continue', () => {
    expect(component.rateClass(row({ ratePercent: 92 }))).toBe('rate-good');
    expect(component.rateClass(row({ ratePercent: 50 }))).toBe('rate-fair');
    expect(component.rateClass(row({ ratePercent: 10 }))).toBe('rate-poor');
    expect(component.rateClass(row({ ratePercent: undefined }))).toBe('rate-unknown');
  });

  // ---- arrivee depuis la fiche CAPA -------------------------------------------

  it("ouvre l'onglet des evaluations sur le dossier nomme par l'adresse", () => {
    // Le renvoi « Evaluer contre le critere » de la fiche CAPA passe
    // `?capa=<id>` : l'ecran doit s'ouvrir sur CE dossier, pret a juger.
    parametres = { capa: 'c-42' };
    const f = TestBed.createComponent(CapaEffectivenessComponent);
    f.detectChanges();

    expect(f.componentInstance.focusCapaId).toBe('c-42');
    expect(f.componentInstance.tab).toBe(0);
  });

  it("ne nomme aucun dossier quand l'adresse est nue", () => {
    expect(component.focusCapaId).toBeUndefined();
    expect(component.tab).toBe(0);
  });

});
