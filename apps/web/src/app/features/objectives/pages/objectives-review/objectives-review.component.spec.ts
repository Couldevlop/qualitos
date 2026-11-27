import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';
import { EMPTY, of, throwError } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { SharedModule } from '../../../../shared/shared.module';
import { UiModule } from '../../../../shared/ui/ui.module';
import { DocumentsService } from '../../../documents/documents.service';
import { ObjectivesService } from '../../objectives.service';
import {
  ObjectiveProposal,
  ObjectiveReview,
  QualityObjective
} from '../../objectives.types';
import { ObjectivesReviewComponent } from './objectives-review.component';

/**
 * La revue « Objectifs vs KPI réels ».
 *
 * <p>Ce qui est vérifié ici, c'est ce que l'écran DIT : la cible écrite dans le
 * sens du KPI, l'écart signé, pourquoi un objectif reste en alerte, et le fait
 * que la colonne « Action » ouvre la fenêtre CAPA sans rien avoir prérédigé
 * (recette du 27/11/2026, ADR 0143).
 */
describe('ObjectivesReviewComponent — la revue des objectifs (ADR 0133)', () => {

  function objectif(over: Partial<QualityObjective> = {}): QualityObjective {
    return {
      id: 'o-1', title: 'Taux de NC client', kpiId: 'k-1', kpiCode: 'NC-CLI',
      kpiName: 'NC client', kpiUnit: 'ppm', kpiDirection: 'LOWER_IS_BETTER',
      processId: 'p-1', processName: 'Production', isoClause: '6.2',
      periodMonths: 1, targetValue: 500, thresholdWarning: 600, thresholdCritical: 800,
      warningPeriods: 2, criticalPeriods: 1, recoveryPeriods: 3,
      effectivenessCriterion: 'Le KPI « NC client » revient à ou sous 500 ppm…',
      lifecycle: 'ACTIVE',
      assessment: {
        status: 'CRITICAL', observedValue: 900, deviationAbsolute: 400,
        deviationPercent: 80, warningBreaches: 3, criticalBreaches: 1,
        onTargetPeriods: 0, triggeredLevel: 'CRITICAL', recoveryAchieved: false,
        heldByOpenCapa: false, periodsConsidered: 6,
        lastPeriodStart: '2026-10-01T00:00:00Z', lastPeriodEnd: '2026-11-01T00:00:00Z'
      },
      createdAt: '2026-01-01T00:00:00Z', updatedAt: '2026-11-01T00:00:00Z',
      ...over
    };
  }

  function proposition(over: Partial<ObjectiveProposal> = {}): ObjectiveProposal {
    return {
      id: 'pr-1', objectiveId: 'o-1', objectiveTitle: 'Taux de NC client',
      level: 'CRITICAL', periodStart: '2026-10-01T00:00:00Z',
      periodEnd: '2026-11-01T00:00:00Z', observedValue: 900, breachPeriods: 1,
      title: 'Objectif critique : Taux de NC client', description: 'Le KPI dépasse…',
      effectivenessCriterion: 'Le KPI « NC client » revient à ou sous 500 ppm…',
      status: 'PENDING', capaCaseId: null, rejectionReason: null,
      decidedBy: null, decidedAt: null, createdAt: '2026-11-01T00:00:00Z',
      ...over
    };
  }

  function revue(over: Partial<ObjectiveReview> = {}): ObjectiveReview {
    return {
      asOf: null,
      counters: { tracked: 1, onTarget: 0, warning: 0, critical: 1 },
      objectives: [objectif()],
      pendingProposals: [proposition()],
      ...over
    };
  }

  let fixture: ComponentFixture<ObjectivesReviewComponent>;
  let component: ObjectivesReviewComponent;
  let svc: jasmine.SpyObj<ObjectivesService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snack: jasmine.SpyObj<MatSnackBar>;

  async function build(r: ObjectiveReview = revue(), canManage = true): Promise<void> {
    svc = jasmine.createSpyObj<ObjectivesService>('ObjectivesService',
      ['review', 'openCapa', 'reject', 'archive', 'evaluateAll']);
    svc.review.and.returnValue(of(r));
    svc.openCapa.and.returnValue(of({ capaId: 'c-9', proposalId: 'pr-1' }));
    svc.reject.and.returnValue(of(proposition({ status: 'REJECTED' })));
    svc.archive.and.returnValue(of(undefined));
    svc.evaluateAll.and.returnValue(of([]));

    const docs = jasmine.createSpyObj<DocumentsService>('DocumentsService', ['processes']);
    docs.processes.and.returnValue(of([{ id: 'p-1', name: 'Production', position: 0 }]));

    const authz = jasmine.createSpyObj<AuthzService>('AuthzService', ['can']);
    authz.can.and.returnValue(of(canManage));

    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snack = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);
    snack.open.and.returnValue({ onAction: () => EMPTY } as never);

    await TestBed.resetTestingModule().configureTestingModule({
      declarations: [ObjectivesReviewComponent],
      imports: [SharedModule, UiModule, FormsModule, NoopAnimationsModule, RouterTestingModule],
      providers: [
        { provide: ObjectivesService, useValue: svc },
        { provide: DocumentsService, useValue: docs },
        { provide: AuthzService, useValue: authz },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snack }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ObjectivesReviewComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  }

  it('charge la revue', async () => {
    await build();
    expect(component.review?.objectives.length).toBe(1);
    expect(component.failed).toBeFalse();
  });

  it('signale l\'échec sans laisser une revue à moitié affichée', async () => {
    await build();
    svc.review.and.returnValue(throwError(() => new Error('boom')));
    component.load();
    expect(component.failed).toBeTrue();
    expect(component.review).toBeUndefined();
  });

  /**
   * Le comparateur suit le sens du KPI. Écrire « ≤ » en dur mentirait sur un
   * indicateur qu'on cherche à augmenter — un taux de conformité, par exemple.
   */
  it('écrit la cible dans le sens du KPI, unité comprise', async () => {
    await build();
    expect(component.targetLabel(objectif())).toBe('≤ 500 ppm');
    expect(component.targetLabel(objectif({ kpiDirection: 'HIGHER_IS_BETTER' })))
      .toBe('≥ 500 ppm');
  });

  it('n\'invente pas d\'unité quand le KPI n\'en a pas', async () => {
    await build();
    expect(component.targetLabel(objectif({ kpiUnit: null }))).toBe('≤ 500');
  });

  it('affiche un tiret, et non zéro, quand rien n\'est mesuré', async () => {
    await build();
    const o = objectif({
      assessment: { ...objectif().assessment, observedValue: null, deviationPercent: null }
    });
    expect(component.observedLabel(o)).toBe('—');
    expect(component.deviationLabel(o)).toBe('—');
  });

  it('arrondit le réel au centième, sans étaler la moyenne du serveur', async () => {
    await build();
    const o = objectif({
      assessment: { ...objectif().assessment, observedValue: 92.63333333333333 }
    });
    expect(component.observedLabel(o)).toBe('92.63 ppm');
  });

  it('arrondit aussi le report absolu de l infobulle, au centieme', async () => {
    // La colonne montrait « 92.63 % » quand l'infobulle deroulait
    // « -5.3666666667 » : deux precisions pour une meme mesure, sur une meme
    // ligne, dont une que personne ne peut lire.
    await build();
    const o = objectif({
      assessment: { ...objectif().assessment, deviationAbsolute: -5.3666666667 }
    });
    expect(component.deviationTitle(o)).toBe('-5.37 ppm');
  });

  it('n\'ajoute pas de décimales à un réel qui n\'en a pas', async () => {
    await build();
    expect(component.observedLabel(objectif())).toBe('900 ppm');
    expect(component.observedLabel(objectif({ kpiUnit: null }))).toBe('900');
  });

  it('signe l\'écart et l\'arrondit au dixième', async () => {
    await build();
    expect(component.deviationLabel(objectif())).toBe('+80 %');
    const sous = objectif({
      assessment: { ...objectif().assessment, deviationPercent: -12.34 }
    });
    expect(component.deviationLabel(sous)).toBe('-12.3 %');
  });

  it('met l\'écart absolu en infobulle, avec son unité', async () => {
    await build();
    expect(component.deviationTitle(objectif())).toBe('+400 ppm');
  });

  /**
   * Un objectif retenu par une CAPA ouverte doit DIRE ce qu'il attend. Sans
   * cela, l'utilisateur voit un KPI revenu sous la cible et un statut en alerte,
   * et conclut à un défaut d'affichage.
   */
  it('dit combien de périodes il reste avant de repasser « atteint »', async () => {
    await build();
    const o = objectif({
      recoveryPeriods: 3,
      assessment: { ...objectif().assessment, onTargetPeriods: 1, heldByOpenCapa: true }
    });
    expect(component.recoveryLabel(o)).toContain('1');
    expect(component.recoveryLabel(o)).toContain('3');
    expect(component.recoveryLabel(o)).toContain('2');
  });

  it('ne descend jamais sous zéro période restante', async () => {
    await build();
    const o = objectif({
      recoveryPeriods: 1,
      assessment: { ...objectif().assessment, onTargetPeriods: 4, heldByOpenCapa: true }
    });
    expect(component.recoveryLabel(o)).toContain('0');
  });

  /**
   * La colonne ne dit plus « Proposée » : la proposition n'est plus un objet que
   * l'utilisateur accepte, elle est devenue le signal qui fait apparaître le
   * bouton. Reste à dire les deux états où il n'y a rien à ouvrir.
   */
  it('distingue le dossier en cours de l\'absence de dossier', async () => {
    await build();
    expect(component.capaLabel(objectif())).toBe('Aucune');

    const retenu = objectif({
      assessment: { ...objectif().assessment, heldByOpenCapa: true }
    });
    expect(component.capaLabel(retenu)).toContain('en cours');
  });

  /**
   * La colonne « Action » n'ouvre plus un dossier prérédigé : elle ouvre la
   * fenêtre CAPA, où l'utilisateur écrit l'action qu'il va vraiment faire. Un
   * texte écrit par la règle se signait sans être lu (recette, diapo 5).
   */
  it('ouvre la fenêtre CAPA, sans rien avoir prérédigé', async () => {
    await build();
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);
    component.addAction(objectif());
    expect(dialog.open).toHaveBeenCalled();
    const data = dialog.open.calls.mostRecent().args[1]?.data as Record<string, unknown>;
    expect(data['prefill']).toEqual({ criticity: 'CRITICAL' });
    expect(data['source']).toEqual({ type: 'QUALITY_OBJECTIVE', reference: 'Taux de NC client' });
    expect(data['lockCriticity']).toBeTrue();
  });

  /**
   * Le critère d'efficacité descend VERROUILLÉ dans la fenêtre : le laisser
   * saisir reviendrait à clore sur une cible plus facile que l'engagement pris.
   */
  it('impose le critère d\'efficacité de l\'objectif à la fenêtre', async () => {
    await build();
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);
    component.addAction(objectif());
    const data = dialog.open.calls.mostRecent().args[1]?.data as Record<string, unknown>;
    expect(data['imposedVerification'])
      .toEqual({ criterion: 'Le KPI « NC client » revient à ou sous 500 ppm…' });
  });

  /** Le dossier naît sur la route de l'objectif, qui solde la proposition. */
  it('crée le dossier par la route de l\'objectif, et recharge la revue', async () => {
    await build();
    let recu: { capaId: string } | undefined;
    dialog.open.and.returnValue({ afterClosed: () => of({ capaId: 'c-9' }) } as never);
    component.addAction(objectif());
    const data = dialog.open.calls.mostRecent().args[1]?.data as Record<string, unknown>;
    const open = data['open'] as (r: Record<string, unknown>) => { subscribe: (f: (v: never) => void) => void };
    open({
      title: 'Recaler la presse 3', ownerId: 'u-1', ownerName: 'Awa Diallo',
      dueDate: '2026-12-10'
    }).subscribe(v => (recu = v));
    expect(svc.openCapa).toHaveBeenCalled();
    expect(svc.openCapa.calls.mostRecent().args[0]).toBe('o-1');
    expect(recu?.capaId).toBe('c-9');
    expect(svc.review).toHaveBeenCalledTimes(2);
  });

  /**
   * Le bouton n'apparaît que sur un écart à traiter : un objectif atteint n'a
   * rien à corriger, et un écart déjà tenu par un dossier ouvert en aurait deux.
   */
  it('ne propose d\'agir que sur un écart non encore traité', async () => {
    await build();
    expect(component.aTraiter(objectif())).toBeTrue();
    expect(component.aTraiter(objectif({
      assessment: { ...objectif().assessment, status: 'ON_TARGET' }
    }))).toBeFalse();
    expect(component.aTraiter(objectif({
      assessment: { ...objectif().assessment, heldByOpenCapa: true }
    }))).toBeFalse();
  });

  /** Sur une alerte, la criticité descend à MEDIUM : elle n'est pas critique. */
  it('fait suivre la criticité du niveau atteint', async () => {
    await build();
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);
    component.addAction(objectif({
      assessment: { ...objectif().assessment, status: 'WARNING' }
    }));
    const data = dialog.open.calls.mostRecent().args[1]?.data as Record<string, unknown>;
    expect(data['prefill']).toEqual({ criticity: 'MEDIUM' });
  });

  it('ne rejette RIEN si la fenêtre de motif est annulée', async () => {
    await build();
    dialog.open.and.returnValue({ afterClosed: () => of(undefined) } as never);
    component.reject(proposition());
    expect(svc.reject).not.toHaveBeenCalled();
  });

  it('rejette avec le motif saisi, et rien d\'autre', async () => {
    await build();
    dialog.open.and.returnValue({ afterClosed: () => of('arrêt planifié') } as never);
    component.reject(proposition());
    expect(svc.reject).toHaveBeenCalledWith('pr-1', 'arrêt planifié');
  });

  it('n\'archive qu\'après confirmation', async () => {
    await build();
    dialog.open.and.returnValue({ afterClosed: () => of(false) } as never);
    component.archive(objectif());
    expect(svc.archive).not.toHaveBeenCalled();

    dialog.open.and.returnValue({ afterClosed: () => of(true) } as never);
    component.archive(objectif());
    expect(svc.archive).toHaveBeenCalledWith('o-1');
  });

  it('dit quand aucun écart n\'atteint la règle, plutôt que de ne rien dire', async () => {
    await build();
    component.evaluateAll();
    expect(snack.open).toHaveBeenCalled();
    expect(svc.review).toHaveBeenCalledTimes(2);
  });

  it('recharge quand on change un filtre, en le transmettant au service', async () => {
    await build();
    component.processId = 'p-9';
    component.asOf = '2026-10-31';
    component.load();
    expect(svc.review).toHaveBeenCalledWith('p-9', '2026-10-31');
  });

  /**
   * La recette veut une liste déroulante mm/aaaa, pas un champ de date libre :
   * la revue se lit par période, et une date au hasard dans le mois ne dit rien
   * de plus que le mois.
   */
  it('offre les mois en liste déroulante, le plus récent en tête', async () => {
    await build();
    expect(component.mois.length).toBe(24);
    expect(component.mois[0].value.endsWith('-01')).toBeTrue();
    expect(component.mois[0].label).toMatch(/^\d{2}\/\d{4}$/);
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('[data-test="objectifs-filtre-periode"]')).not.toBeNull();
  });

  it('ne montre les gestes d\'écriture qu\'à qui peut les faire', async () => {
    await build(revue(), false);
    expect(component.canManage).toBeFalse();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('[data-test="objectifs-nouveau"]')).toBeNull();
    expect(html.querySelector('[data-test="objectif-ajouter-action-o-1"]')).toBeNull();
  });

  it('affiche les quatre tuiles et la ligne de l\'objectif', async () => {
    await build();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelectorAll('[data-test="objectifs-tuiles"] .tuile').length).toBe(4);
    expect(html.querySelector('[data-test="objectif-o-1"]')).not.toBeNull();
  });

  /**
   * Le bouton de la colonne « Action », présent sur l'écart à traiter. La diapo
   * 5 de la recette l'exige nommé « Action », et non « CAPA » : ce qu'on ouvre
   * est une action, le dossier n'en est que le support.
   */
  it('porte le bouton d\'action sur la ligne en écart', async () => {
    await build();
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('[data-test="objectif-ajouter-action-o-1"]')).not.toBeNull();
  });

  it('annonce le vide plutôt que de laisser un tableau nu', async () => {
    await build(revue({ objectives: [], pendingProposals: [], counters:
      { tracked: 0, onTarget: 0, warning: 0, critical: 0 } }));
    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('[data-test="objectifs-vide"]')).not.toBeNull();
  });
});
