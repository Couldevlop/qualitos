import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';

import { AuthzService } from '../../../../core/authz/authz.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { StatusTone } from '../../../../shared/ui/status-pill/status-pill.component';
import { DocumentProcess } from '../../../documents/documents.types';
import { DocumentsService } from '../../../documents/documents.service';
import {
  objectiveLevelLabel,
  objectiveStatusLabel,
  objectiveStatusTone,
  periodLabel
} from '../../objectives.labels';
import { ObjectivesService } from '../../objectives.service';
import {
  ObjectiveProposal,
  ObjectiveReview,
  QualityObjective
} from '../../objectives.types';
import { ObjectiveDialogComponent } from '../objective-dialog/objective-dialog.component';
import { ObjectiveRejectDialogComponent } from '../objective-reject-dialog/objective-reject-dialog.component';

/**
 * « Objectifs vs KPI réels » — la revue (ISO 9001 §6.2, §9.3).
 *
 * <p>Quatre tuiles, une ligne par objectif face à son indicateur, et le panneau
 * de la CAPA préremplie quand une proposition attend une décision.
 *
 * <p><b>Le statut n'est pas lu en base.</b> Le serveur le recalcule depuis les
 * relevés à chaque appel : le figer le ferait mentir dès qu'un relevé est
 * corrigé. L'écran ne garde donc rien entre deux chargements.
 *
 * <p>Voir l'ADR 0133.
 */
@Component({
  selector: 'qos-objectives-review',
  templateUrl: './objectives-review.component.html',
  styleUrls: ['./objectives-review.component.scss'],
  standalone: false
})
export class ObjectivesReviewComponent implements OnInit {

  readonly breadcrumbs = [
    { label: $localize`:@@nav.pilotage:Pilotage` },
    { label: $localize`:@@nav.objectifs-qualite:Objectifs qualité` },
    { label: $localize`:@@objectives.review.crumb:Revue` }
  ];

  review?: ObjectiveReview;
  processes: DocumentProcess[] = [];

  /** Le processus filtré, et le mois observé ; les deux filtres de la maquette. */
  processId = '';
  asOf = '';

  /** La proposition dont le panneau latéral montre la CAPA préremplie. */
  selected?: ObjectiveProposal;

  loading = false;
  failed = false;
  deciding = false;
  canManage = false;

  readonly statusLabel = objectiveStatusLabel;
  readonly levelLabel = objectiveLevelLabel;
  readonly period = periodLabel;

  constructor(
    private readonly svc: ObjectivesService,
    private readonly docs: DocumentsService,
    private readonly authz: AuthzService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.authz.can('objective.manage').subscribe(ok => (this.canManage = ok));
    this.docs.processes().subscribe({
      next: list => (this.processes = list ?? []),
      error: () => (this.processes = [])
    });
    this.load();
  }

  load(): void {
    this.loading = true;
    this.failed = false;
    this.svc.review(this.processId || null, this.asOf || null)
      .pipe(finalize(() => (this.loading = false)))
      .subscribe({
        next: r => {
          this.review = r;
          // La proposition ouverte peut avoir été tranchée entre-temps : on la
          // retrouve dans la nouvelle liste, ou on referme le panneau.
          this.selected = this.selected
            ? r.pendingProposals.find(p => p.id === this.selected!.id)
            : r.pendingProposals[0];
        },
        error: () => {
          this.failed = true;
          this.review = undefined;
        }
      });
  }

  tone(o: QualityObjective): StatusTone {
    return objectiveStatusTone(o.assessment.status);
  }

  /** La proposition en attente d'un objectif, s'il y en a une. */
  proposalFor(o: QualityObjective): ObjectiveProposal | undefined {
    return this.review?.pendingProposals.find(p => p.objectiveId === o.id);
  }

  /**
   * La cible, écrite comme la lit une revue : « ≤ 500 ppm » ou « ≥ 95 % ».
   *
   * <p>Le comparateur vient du sens du KPI, et l'unité du KPI aussi : rien n'est
   * écrit en dur, puisque les deux changent d'un indicateur à l'autre.
   */
  targetLabel(o: QualityObjective): string {
    const comparateur = o.kpiDirection === 'LOWER_IS_BETTER' ? '≤' : '≥';
    return `${comparateur} ${o.targetValue}${o.kpiUnit ? ' ' + o.kpiUnit : ''}`;
  }

  observedLabel(o: QualityObjective): string {
    const v = o.assessment.observedValue;
    if (v == null) {
      return '—';
    }
    return `${v}${o.kpiUnit ? ' ' + o.kpiUnit : ''}`;
  }

  /**
   * L'écart, en pourcentage de la cible, signé.
   *
   * <p>Le pourcentage et non les points : la plateforme ne sait pas si l'unité du
   * KPI est elle-même un pourcentage, et parler de « points » sur des ppm serait
   * faux. Le serveur rend aussi l'écart absolu, affiché en infobulle.
   */
  deviationLabel(o: QualityObjective): string {
    const pct = o.assessment.deviationPercent;
    if (pct == null) {
      return '—';
    }
    const arrondi = Math.round(pct * 10) / 10;
    return `${arrondi > 0 ? '+' : ''}${arrondi} %`;
  }

  deviationTitle(o: QualityObjective): string {
    const abs = o.assessment.deviationAbsolute;
    if (abs == null) {
      return '';
    }
    return `${abs > 0 ? '+' : ''}${abs}${o.kpiUnit ? ' ' + o.kpiUnit : ''}`;
  }

  /**
   * Ce que dit la colonne CAPA : le dossier ouvert, la proposition en attente,
   * ou rien.
   */
  capaLabel(o: QualityObjective): string {
    const p = this.proposalFor(o);
    if (p) {
      return p.breachPeriods > 1
        ? $localize`:@@objectives.capa.proposed-periods:Proposée · ${p.breachPeriods}:count: périodes consécutives`
        : $localize`:@@objectives.capa.proposed:Proposée · à valider par le pilote`;
    }
    if (o.assessment.heldByOpenCapa) {
      return $localize`:@@objectives.capa.open:CAPA en cours · retour sous la cible attendu`;
    }
    return $localize`:@@objectives.capa.none:Aucune`;
  }

  /** Ce qu'il reste à tenir avant que l'objectif puisse repasser « atteint ». */
  recoveryLabel(o: QualityObjective): string {
    const reste = Math.max(0, o.recoveryPeriods - o.assessment.onTargetPeriods);
    return $localize`:@@objectives.recovery.remaining:${o.assessment.onTargetPeriods}:tenues: période(s) sous la cible sur ${o.recoveryPeriods}:exigees: — il en reste ${reste}:reste:`;
  }

  select(proposal: ObjectiveProposal | undefined): void {
    this.selected = proposal;
  }

  create(): void {
    this.dialog.open(ObjectiveDialogComponent, {
      panelClass: 'qos-dialog-panel', width: '880px', data: {}
    }).afterClosed().subscribe(saved => {
      if (saved) {
        this.load();
      }
    });
  }

  edit(o: QualityObjective): void {
    this.dialog.open(ObjectiveDialogComponent, {
      panelClass: 'qos-dialog-panel', width: '880px', data: { objective: o }
    }).afterClosed().subscribe(saved => {
      if (saved) {
        this.load();
      }
    });
  }

  /**
   * Confronte tous les objectifs à leurs mesures.
   *
   * <p>Le geste est explicite : la règle ne tourne pas dans le dos de
   * l'utilisateur, et c'est lui qui décide quand la revue se fait.
   */
  evaluateAll(): void {
    this.deciding = true;
    this.svc.evaluateAll().pipe(finalize(() => (this.deciding = false))).subscribe({
      next: nees => {
        this.snack.open(nees.length
          ? $localize`:@@objectives.evaluated.some:${nees.length}:count: CAPA proposée(s) à trancher.`
          : $localize`:@@objectives.evaluated.none:Aucun écart n'atteint la règle de déclenchement.`,
          undefined, { duration: 5000 });
        this.load();
      },
      error: err => this.snack.open(
        safeErrorMessage(err, $localize`:@@objectives.error.evaluate:La confrontation aux mesures n'a pas abouti.`),
        undefined, { duration: 6000 })
    });
  }

  /** « Accepter et enregistrer la CAPA » : le dossier naît, et l'écran y mène. */
  accept(p: ObjectiveProposal): void {
    this.deciding = true;
    this.svc.accept(p.id).pipe(finalize(() => (this.deciding = false))).subscribe({
      next: decided => {
        this.snack.open($localize`:@@objectives.accepted:CAPA enregistrée.`,
          $localize`:@@objectives.open-capa:Ouvrir`, { duration: 8000 })
          .onAction().subscribe(() => {
            if (decided.capaCaseId) {
              this.router.navigate(['/capa', decided.capaCaseId]);
            }
          });
        this.selected = undefined;
        this.load();
      },
      error: err => this.snack.open(
        safeErrorMessage(err, $localize`:@@objectives.error.accept:La CAPA n'a pas pu être enregistrée.`),
        undefined, { duration: 6000 })
    });
  }

  /**
   * La CAPA proposée ne convient pas tout à fait : on l'enregistre, et on va lui
   * ajouter l'action qui manque.
   *
   * <p>C'est la troisième voie que demande la maquette annotée, à côté
   * d'accepter et de rejeter. Rejeter en bloc laisserait l'écart sans traitement
   * alors que le désaccord ne porte que sur le geste à faire.
   */
  acceptAndAddAction(p: ObjectiveProposal): void {
    this.deciding = true;
    this.svc.accept(p.id).pipe(finalize(() => (this.deciding = false))).subscribe({
      next: decided => {
        this.selected = undefined;
        if (decided.capaCaseId) {
          // On mène au dossier, où le panneau d'actions est à portée. Ouvrir la
          // fenêtre d'ajout d'ici demanderait de traverser deux composants du
          // module CAPA, ce que ce lot n'a pas à faire.
          this.router.navigate(['/capa', decided.capaCaseId]);
        } else {
          this.load();
        }
      },
      error: err => this.snack.open(
        safeErrorMessage(err, $localize`:@@objectives.error.accept:La CAPA n'a pas pu être enregistrée.`),
        undefined, { duration: 6000 })
    });
  }

  /** « Rejeter avec justification » : le motif est obligatoire, et il est demandé. */
  reject(p: ObjectiveProposal): void {
    this.dialog.open(ObjectiveRejectDialogComponent, {
      panelClass: 'qos-dialog-panel', width: '560px', data: { proposal: p }
    }).afterClosed().subscribe(reason => {
      if (!reason) {
        return;
      }
      this.deciding = true;
      this.svc.reject(p.id, reason).pipe(finalize(() => (this.deciding = false))).subscribe({
        next: () => {
          this.snack.open($localize`:@@objectives.rejected:Proposition rejetée, avec son motif.`,
            undefined, { duration: 5000 });
          this.selected = undefined;
          this.load();
        },
        error: err => this.snack.open(
          safeErrorMessage(err, $localize`:@@objectives.error.reject:Le rejet n'a pas pu être enregistré.`),
          undefined, { duration: 6000 })
      });
    });
  }

  /**
   * Retire l'objectif du suivi. Il n'est PAS effacé : la raison des CAPA qu'il a
   * fait ouvrir disparaîtrait avec lui, et la confirmation le dit.
   */
  archive(o: QualityObjective): void {
    this.dialog.open(ConfirmDialogComponent, {
      panelClass: 'qos-dialog-panel',
      data: {
        title: $localize`:@@objectives.archive.title:Retirer cet objectif du suivi ?`,
        message: $localize`:@@objectives.archive.message:« ${o.title}:titre: » ne comptera plus dans la revue. Il est conservé, avec les CAPA qu'il a fait ouvrir : rien n'est effacé.`,
        confirmLabel: $localize`:@@objectives.archive.confirm:Retirer du suivi`
      }
    }).afterClosed().subscribe(ok => {
      if (ok) {
        this.svc.archive(o.id).subscribe({
          next: () => this.load(),
          error: err => this.snack.open(
            safeErrorMessage(err, $localize`:@@objectives.error.archive:Le retrait du suivi n'a pas abouti.`),
            undefined, { duration: 6000 })
        });
      }
    });
  }
}
