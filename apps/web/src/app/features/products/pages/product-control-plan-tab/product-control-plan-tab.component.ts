import { Component, Input, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../../core/auth/auth.service';
import { ProductsService } from '../../products.service';
import {
  ControlPlanDetail,
  ControlPlanLineView,
  ControlPlanPhase,
  ControlPlanView
} from '../../products.types';
import {
  ControlPlanLineDialogComponent
} from '../control-plan-line-dialog/control-plan-line-dialog.component';

/**
 * Le control plan du produit, phase par phase.
 *
 * <p>Chaque ligne montre la ligne de PFMEA qui la justifie — et celles qui n'en
 * ont aucune le disent. Un contrôle sans raison d'être coûte du temps au poste
 * sans réduire aucun risque : c'est exactement ce qu'un auditeur cherche.
 */
@Component({
  selector: 'qos-product-control-plan-tab',
  templateUrl: './product-control-plan-tab.component.html',
  styleUrls: ['./product-control-plan-tab.component.scss'],
  standalone: false
})
export class ProductControlPlanTabComponent implements OnInit {

  @Input() productId = '';

  /**
   * Les colonnes de la trame AIAG, dans l'ordre où elle les lit. « Où » —
   * procédure et lieu d'enregistrement — vient en dernier avant les
   * commandes : c'est ce que l'auditeur suit, pas ce que l'opérateur
   * consulte au poste.
   */
  readonly columns = ['sequenceNo', 'characteristic', 'specification', 'control',
    'measuredBy', 'reaction', 'justification', 'trace', 'actions'];
  readonly phases: ControlPlanPhase[] = ['PROTOTYPE', 'PRE_LAUNCH', 'PRODUCTION'];

  plans: ControlPlanView[] = [];
  selected?: ControlPlanDetail;
  loading = false;

  constructor(
    private readonly service: ProductsService,
    private readonly dialog: MatDialog,
    private readonly auth: AuthService,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.reload();
  }

  /**
   * L'autorité reste le serveur, qui répondra 403. Afficher un bouton qui
   * échouera à coup sûr est simplement une mauvaise expérience.
   */
  get canApprove(): boolean {
    return this.auth.hasAnyRole(['DIRECTOR_QUALITY', 'ADMIN_TENANT', 'SUPER_ADMIN']);
  }

  get editable(): boolean {
    return this.selected?.plan.status === 'DRAFT';
  }

  reload(): void {
    // `loading` est un champ simple, posé AVANT l'abonnement : le patron
    // queueMicrotask ne vaut que pour un Subject poussé depuis une souscription.
    // Ici il produirait l'inverse — un flux synchrone rendrait la main avant la
    // micro-tâche, qui rallumerait le voyant sur une liste déjà chargée.
    this.loading = true;
    this.service.controlPlans(this.productId).subscribe({
      next: plans => {
        this.plans = plans;
        this.loading = false;
        const preferred = plans.find(plan => plan.status === 'DRAFT')
          ?? plans.find(plan => plan.status === 'ACTIVE')
          ?? plans[0];
        if (preferred) this.select(preferred);
        else this.selected = undefined;
      },
      error: () => this.fail($localize`:@@controlplan.load-failed:Control plan indisponible.`)
    });
  }

  select(plan: ControlPlanView): void {
    this.service.controlPlan(this.productId, plan.id).subscribe({
      next: detail => (this.selected = detail),
      error: () => this.fail($localize`:@@controlplan.load-failed:Control plan indisponible.`)
    });
  }

  createDraft(phase: ControlPlanPhase): void {
    const code = `CP-${phase}-${new Date().getFullYear()}`;
    this.service.createControlPlan(this.productId, { phase, code }).subscribe({
      next: () => this.reload(),
      error: err => this.fail(err?.status === 409
        ? $localize`:@@controlplan.draft-exists:Un brouillon existe déjà pour cette phase.`
        : $localize`:@@product.save-failed:Enregistrement impossible.`)
    });
  }

  openRevision(): void {
    if (!this.selected) return;
    this.service.openRevision(this.productId, this.selected.plan.id).subscribe({
      next: () => this.reload(),
      error: () => this.fail($localize`:@@controlplan.revision-failed:Ouverture de révision impossible.`)
    });
  }

  approve(): void {
    if (!this.selected) return;
    this.service.approveControlPlan(this.productId, this.selected.plan.id).subscribe({
      next: () => this.reload(),
      error: err => this.isStepUpRequired(err)
        ? this.askForSecondFactor()
        : this.fail($localize`:@@controlplan.approve-failed:Approbation impossible.`)
    });
  }

  /**
   * Le serveur refuse la signature tant que le jeton ne porte pas la trace d'un
   * second facteur. Ce n'est pas une session invalide : inutile de déconnecter,
   * il suffit de redemander le palier.
   */
  private isStepUpRequired(err: unknown): boolean {
    const problem = err as { status?: number; error?: { type?: string } };
    return problem?.status === 403
        && problem?.error?.type === 'https://qualitos.io/errors/step-up-required';
  }

  private askForSecondFactor(): void {
    this.loading = false;
    const snack = this.snack.open(
      $localize`:@@stepup.required:Cette signature exige votre code à usage unique.`,
      $localize`:@@stepup.reauthenticate:Se réauthentifier`,
      { duration: 10000 });
    snack.onAction().subscribe(() => {
      if (!this.auth.stepUp(`/products/${this.productId}`)) {
        this.fail($localize`:@@stepup.unavailable:Second facteur indisponible sur cet environnement.`);
      }
    });
  }

  addLine(): void {
    this.openLineDialog();
  }

  editLine(line: ControlPlanLineView): void {
    this.openLineDialog(line);
  }

  deleteLine(line: ControlPlanLineView): void {
    if (!this.selected) return;
    this.service.deleteLine(this.productId, this.selected.plan.id, line.id).subscribe({
      next: () => this.select(this.selected!.plan),
      error: () => this.fail($localize`:@@product.delete-failed:Suppression impossible.`)
    });
  }

  private openLineDialog(line?: ControlPlanLineView): void {
    if (!this.selected) return;
    const plan = this.selected.plan;
    this.dialog.open(ControlPlanLineDialogComponent, {
      panelClass: 'qos-dialog-panel',
      data: { productId: this.productId, planId: plan.id, line }
    }).afterClosed().subscribe(saved => { if (saved) this.select(plan); });
  }

  private fail(message: string): void {
    this.loading = false;
    this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 4000 });
  }
}
