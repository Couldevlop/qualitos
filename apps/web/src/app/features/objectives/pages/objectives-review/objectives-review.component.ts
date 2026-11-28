import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { finalize, map } from 'rxjs/operators';

import { AuthzService } from '../../../../core/authz/authz.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import {
  CapaCreateDialogComponent
} from '../../../capa/pages/capa-create-dialog/capa-create-dialog.component';
import { StatusTone } from '../../../../shared/ui/status-pill/status-pill.component';
import { DocumentProcess } from '../../../documents/documents.types';
import { DocumentsService } from '../../../documents/documents.service';
import {
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
 * <p>Quatre tuiles, et une ligne par objectif face à son indicateur.
 *
 * <p><b>Aucune CAPA préremplie.</b> Sur une ligne en alerte ou critique, on
 * ouvre LA fenêtre CAPA de la plateforme et on écrit l'action qu'on va faire ;
 * le dossier naît de ces mots-là, sous la source « Objectif qualité ». C'est ce
 * que la recette a demandé : un dossier prérédigé par la règle se signait sans
 * être lu (ADR 0143).
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

  loading = false;
  failed = false;
  deciding = false;
  canManage = false;

  readonly statusLabel = objectiveStatusLabel;
  readonly period = periodLabel;

  /**
   * Les vingt-quatre derniers mois, du plus récent au plus ancien.
   *
   * <p>La valeur est le PREMIER jour du mois : le serveur borne à la fin du
   * mois demandé, et n'importe quel jour de ce mois donne la même revue. Deux
   * ans couvrent la comparaison annuelle d'une revue de direction (§9.3).
   */
  readonly mois = ObjectivesReviewComponent.derniersMois(24);

  constructor(
    private readonly svc: ObjectivesService,
    private readonly docs: DocumentsService,
    private readonly authz: AuthzService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
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
        next: r => (this.review = r),
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

  /**
   * Le réel, arrondi au centième.
   *
   * <p>C'est une moyenne calculée sur la période : elle arrive brute du serveur
   * avec toutes ses décimales, et un « 92.6333333333333 % » déborde de sa
   * colonne, entre une cible et un écart tous deux lisibles.
   *
   * <p>Deux décimales et non une comme l'écart : à une seule, un réel de 97,96
   * s'écrirait « 98 » face à une cible « ≥ 98 », et la ligne contredirait son
   * propre statut. L'arrondi est d'affichage seul — le serveur garde la valeur
   * exacte, dont découlent l'écart et le statut.
   */
  observedLabel(o: QualityObjective): string {
    const v = o.assessment.observedValue;
    if (v == null) {
      return '—';
    }
    const arrondi = Math.round(v * 100) / 100;
    return `${arrondi}${o.kpiUnit ? ' ' + o.kpiUnit : ''}`;
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

  /**
   * L'écart absolu, en unités du KPI, pour l'infobulle.
   *
   * <p>Arrondi au centième comme {@link observedLabel}, et pour la même raison :
   * la colonne voisine montrait « 92.63 % » quand l'infobulle déroulait
   * « -5.3666666667 » — deux précisions pour une même mesure sur une même
   * ligne, dont une que personne ne peut lire.
   */
  deviationTitle(o: QualityObjective): string {
    const abs = o.assessment.deviationAbsolute;
    if (abs == null) {
      return '';
    }
    const arrondi = Math.round(abs * 100) / 100;
    return `${arrondi > 0 ? '+' : ''}${arrondi}${o.kpiUnit ? ' ' + o.kpiUnit : ''}`;
  }

  /**
   * L'écart appelle-t-il une action aujourd'hui ?
   *
   * <p>Le STATUT et non la seule proposition : la règle ne propose qu'au bout
   * de ses périodes consécutives, et la recette veut le bouton dès la première
   * ligne passée en alerte. Un dossier déjà ouvert le retire : on ne traite pas
   * deux fois le même écart.
   */
  aTraiter(o: QualityObjective): boolean {
    const s = o.assessment.status;
    return (s === 'WARNING' || s === 'CRITICAL') && !o.assessment.heldByOpenCapa;
  }

  /** Ce que dit la colonne Action quand il n'y a rien à ouvrir. */
  capaLabel(o: QualityObjective): string {
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
   * « Ajouter une action » : LA fenêtre CAPA s'ouvre, vide, et le dossier naît
   * de ce qu'on y écrit.
   *
   * <p>La source, la criticité, la vérification d'efficacité et son critère ne
   * sont pas demandés : le serveur les impose. Ce qui se saisit ici, c'est
   * l'action — son intitulé, qui en répond, et pour quand.
   */
  addAction(o: QualityObjective): void {
    this.dialog.open(CapaCreateDialogComponent, {
      panelClass: 'qos-dialog-panel', width: '720px', maxWidth: '96vw',
      data: {
        source: { type: 'QUALITY_OBJECTIVE' as const, reference: o.title },
        lockCriticity: true,
        prefill: { criticity: o.assessment.status === 'CRITICAL' ? 'CRITICAL' : 'MEDIUM' },
        imposedVerification: { criterion: o.effectivenessCriterion },
        open: (req: { title: string; description?: string; ownerId: string;
                      ownerName?: string; dueDate?: string }) =>
          this.svc.openCapa(o.id, {
            title: req.title,
            description: req.description ?? null,
            ownerId: req.ownerId,
            ownerName: req.ownerName ?? null,
            dueDate: req.dueDate ?? ''
          }).pipe(map(opened => ({ capaId: opened.capaId })))
      }
    }).afterClosed().subscribe(opened => {
      if (opened) {
        this.load();
      }
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

  /** Les `n` derniers mois, libellés « mm/aaaa », valeur `AAAA-MM-01`. */
  private static derniersMois(n: number): ReadonlyArray<{ value: string; label: string }> {
    const aujourdhui = new Date();
    const liste: { value: string; label: string }[] = [];
    for (let i = 0; i < n; i++) {
      const d = new Date(aujourdhui.getFullYear(), aujourdhui.getMonth() - i, 1);
      const mois = String(d.getMonth() + 1).padStart(2, '0');
      liste.push({ value: `${d.getFullYear()}-${mois}-01`, label: `${mois}/${d.getFullYear()}` });
    }
    return liste;
  }
}
