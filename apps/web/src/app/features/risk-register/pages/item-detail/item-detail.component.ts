import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PageBreadcrumb } from '../../../../shared/ui/page-header/page-header.component';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { RiskRegisterService } from '../../risk-register.service';
import {
  actionStatusLabel, capaStatusLabel, eventText, opportunityDecisionLabel, opportunityLevelLabel,
  opportunityStatusLabel, originLabel, requirementLabel, riskDecisionLabel, riskLevelLabel, riskStatusLabel,
  typeLabel, GAINS, FEASIBILITIES
} from '../../risk-register.labels';
import {
  OpportunityAction, OpportunitySheet, RegisterEvent, RegisterRequirement, RiskSheet
} from '../../risk-register.types';
import { ROLES_PILOTAGE } from '../register/register.component';
import { ItemKind } from '../item-form/item-form.component';
import {
  ActionDialogComponent, ActionDialogData, ActionDialogResult
} from '../action-dialog/action-dialog.component';

/**
 * La fiche d'un risque ou d'une opportunité qui existe déjà.
 *
 * <p>Elle s'enrichit avec le temps — actions liées, cotation résiduelle,
 * historique des cotations, prochaine revue, vérification d'efficacité — et
 * c'est par elle, non par le formulaire, qu'on consulte une ligne du registre.
 * « Modifier » rouvre le formulaire, prérempli.
 */
@Component({
  selector: 'qos-risk-item-detail',
  templateUrl: './item-detail.component.html',
  styleUrls: ['./item-detail.component.scss'],
  standalone: false
})
export class ItemDetailComponent implements OnInit {

  readonly editable: boolean;

  kind: ItemKind = 'risk';
  id = '';
  risk: RiskSheet | null = null;
  opportunity: OpportunitySheet | null = null;
  loading = false;
  failed = false;
  busy = false;

  constructor(
    private readonly service: RiskRegisterService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    auth: AuthService
  ) {
    this.editable = auth.hasAnyRole(ROLES_PILOTAGE);
  }

  ngOnInit(): void {
    this.kind = this.route.snapshot.data['kind'] === 'opportunity' ? 'opportunity' : 'risk';
    this.id = this.route.snapshot.paramMap.get('id') ?? '';
    this.charger();
  }

  get isRisk(): boolean {
    return this.kind === 'risk';
  }

  get reference(): string {
    return (this.isRisk ? this.risk?.risk.reference : this.opportunity?.opportunity.reference) ?? '…';
  }

  get breadcrumbs(): PageBreadcrumb[] {
    return [
      { label: $localize`:@@rr.title:Risques et opportunités`, route: '/risques' },
      this.isRisk
        ? { label: $localize`:@@rr.crumb.register:Registre`, route: '/risques' }
        : { label: $localize`:@@rr.tab-opportunities:Opportunités`, route: '/risques/opportunites' },
      { label: this.reference }
    ];
  }

  /**
   * Le lien vers l'objet source, quand l'écran sait l'ouvrir : la non-conformité
   * et le changement ont leur page. Une ligne d'AMDEC et un constat d'audit
   * n'ont pas d'adresse propre — leur référence suffit à les retrouver.
   */
  get sourceLink(): string[] | null {
    const r = this.risk?.risk;
    if (!r?.sourceId) return null;
    if (r.origin === 'NON_CONFORMITY') return ['/nc', r.sourceId];
    if (r.origin === 'CHANGE') return ['/changes', r.sourceId];
    return null;
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    const fin = () => this.loading = false;
    const echec = (err: unknown) => {
      fin();
      this.failed = true;
      this.echouer(err);
    };
    if (this.isRisk) {
      this.service.risk(this.id).subscribe({ next: s => { this.risk = s; fin(); }, error: echec });
    } else {
      this.service.opportunity(this.id).subscribe({ next: s => { this.opportunity = s; fin(); }, error: echec });
    }
  }

  edit(): void {
    this.router.navigate(this.isRisk
      ? ['/risques', this.id, 'modifier']
      : ['/risques', 'opportunites', this.id, 'modifier']);
  }

  // ---------- actions ----------

  /** « Créer une action CAPA » (risque) ou « Créer une action » (opportunité). */
  createAction(): void {
    if (!this.editable || this.busy) return;
    this.ouvrir({ mode: this.isRisk ? 'capa' : 'action', reference: this.reference }, result => {
      if (this.isRisk) {
        this.busy = true;
        this.service.openCapa(this.id, {
          title: result.title, description: result.description, dueDate: result.dueDate
        }).subscribe({
          next: () => {
            this.busy = false;
            this.snack.open($localize`:@@rr.detail.capa-opened:Dossier CAPA ouvert.`, undefined, { duration: 2500 });
            this.charger();
          },
          error: err => { this.busy = false; this.echouer(err); }
        });
      } else {
        this.busy = true;
        this.service.addAction(this.id, {
          title: result.title, dueDate: result.dueDate, status: result.status
        }).subscribe({
          next: () => { this.busy = false; this.charger(); },
          error: err => { this.busy = false; this.echouer(err); }
        });
      }
    });
  }

  editAction(a: OpportunityAction): void {
    if (!this.editable) return;
    this.ouvrir({
      mode: 'action', reference: this.reference, title: a.title, dueDate: a.dueDate, status: a.status,
      number: a.number
    }, result => {
      this.service.reviseAction(this.id, a.id, {
        title: result.title, dueDate: result.dueDate, status: result.status
      }).subscribe({ next: () => this.charger(), error: err => this.echouer(err) });
    });
  }

  deleteAction(a: OpportunityAction): void {
    if (!this.editable) return;
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(ConfirmDialogComponent, {
      data: {
        title: $localize`:@@rr.detail.delete-action-title:Supprimer ACT-${a.number}:number: ?`,
        message: $localize`:@@rr.detail.delete-action-message:L'action quitte la fiche. La suppression est tracée dans le journal d'audit.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(ok => {
      if (!ok) return;
      this.service.deleteAction(this.id, a.id).subscribe({
        next: () => this.charger(),
        error: err => this.echouer(err)
      });
    });
  }

  private ouvrir(data: ActionDialogData, then: (r: ActionDialogResult) => void): void {
    this.dialog.open<ActionDialogComponent, ActionDialogData, ActionDialogResult>(ActionDialogComponent, {
      data, panelClass: 'qos-dialog-panel', restoreFocus: true
    }).afterClosed().subscribe(result => {
      if (result) then(result);
    });
  }

  // ---------- gabarit ----------

  /** Largeur d'une barre de note (1 à 5), en pourcentage. */
  bar(note: number | null | undefined): number {
    return note ? note * 20 : 0;
  }

  typeText = typeLabel;
  originText = originLabel;
  riskStatusText = riskStatusLabel;
  riskDecisionText = riskDecisionLabel;
  riskLevelText = riskLevelLabel;
  opportunityStatusText = opportunityStatusLabel;
  opportunityDecisionText = opportunityDecisionLabel;
  opportunityLevelText = opportunityLevelLabel;
  capaStatusText = capaStatusLabel;
  actionStatusText = actionStatusLabel;
  requirementText = requirementLabel;

  /** « 4 · Fort » → « Fort » : le mot de l'échelle, sans la note qu'on affiche déjà. */
  gainWord(n: number): string {
    return (GAINS.find(g => g.value === n)?.label ?? '').replace(/^\d\s·\s/, '');
  }

  feasibilityWord(n: number): string {
    return (FEASIBILITIES.find(f => f.value === n)?.label ?? '').replace(/^\d\s·\s/, '');
  }

  eventLine(e: RegisterEvent): string {
    return eventText(e, this.kind);
  }

  trackById(_i: number, row: { id: string }): string {
    return row.id;
  }

  trackByCode(_i: number, code: RegisterRequirement): string {
    return code;
  }

  private echouer(err: unknown): void {
    this.snack.open(safeErrorMessage(err, $localize`:@@rr.detail.failed:La fiche est indisponible.`),
      undefined, { duration: 5000 });
  }
}
