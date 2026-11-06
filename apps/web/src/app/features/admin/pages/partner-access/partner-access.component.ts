import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { PartnerService } from '../../../../core/partner/partner.service';
import { EngagementScope, PartnerEngagement } from '../../../../core/partner/partner.types';
import { ConfirmDialogComponent } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';

/**
 * Ce qu'on saisit pour accorder un accès. Le terme est obligatoire, et c'est
 * tout l'objet de cet écran : un accès sans date de fin est un accès oublié.
 */
export interface GrantDraft {
  scope: EngagementScope;
  endsOn: string;
  purpose: string;
}

/** Le terme proposé par défaut : trois mois, soit la durée d'une mission courante. */
export const DEFAULT_GRANT_MONTHS = 3;

/**
 * « Cabinets », réservé à l'administrateur du client : qui, hors de la maison,
 * peut entrer dans ses données — et jusqu'à quand.
 *
 * <p><b>Pourquoi l'écran est chez le client et non chez l'éditeur.</b> L'éditeur
 * constitue le portefeuille d'un cabinet : il déclare le cabinet, ses
 * consultants, et <i>propose</i> un client. Mais il n'ouvre rien. Seul
 * l'administrateur du client accorde, et lui seul reprend. Sans cette
 * séparation, un client découvrirait après coup qu'un tiers a lu son dossier
 * qualité — et aucune date de fin ne le rassurerait.
 *
 * <p><b>Pourquoi un terme est obligatoire.</b> Un accès accordé « jusqu'à
 * nouvel ordre » n'est jamais repris : personne ne se souvient de le faire. Le
 * serveur refuse d'ailleurs un accord accordé sans dates, jusque dans une
 * contrainte de la base. Ici, le formulaire propose trois mois et laisse
 * corriger, pour que la valeur par défaut soit raisonnable plutôt qu'absente.
 *
 * <p><b>Refuser et reprendre ne se confondent pas.</b> Le premier n'a jamais
 * rien ouvert, le second ferme un accès qui existait. Les deux restent dans
 * l'historique : effacer une décision passée réécrirait l'histoire, et c'est
 * précisément celle qu'un auditeur vient lire.
 */
@Component({
  selector: 'qos-partner-access',
  templateUrl: './partner-access.component.html',
  styleUrls: ['./partner-access.component.scss'],
  standalone: false
})
export class PartnerAccessComponent implements OnInit {

  engagements: PartnerEngagement[] = [];

  loading = false;
  failed = false;
  /** L'accord dont une décision est en cours : il verrouille ses seuls boutons. */
  busyId: string | null = null;

  /** L'accord dont on remplit le formulaire d'octroi, et son brouillon. */
  grantingId: string | null = null;
  draft: GrantDraft = PartnerAccessComponent.emptyDraft();

  constructor(
    private readonly partner: PartnerService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.partner.myEngagements().subscribe({
      next: accords => {
        this.engagements = accords;
        this.loading = false;
      },
      error: () => {
        this.engagements = [];
        this.loading = false;
        this.failed = true;
      }
    });
  }

  // ---------- les trois groupes ----------

  /** Ce qui attend une décision. En tête : c'est la seule chose à faire ici. */
  get pending(): PartnerEngagement[] {
    return this.engagements.filter(a => a.status === 'PROPOSED');
  }

  /** Ce qui est ouvert, ou le sera : un accord accordé dont le début est à venir. */
  get granted(): PartnerEngagement[] {
    return this.engagements.filter(a => a.status === 'GRANTED');
  }

  /** Ce qui a été refusé ou repris. Conservé : c'est la trace de la décision. */
  get history(): PartnerEngagement[] {
    return this.engagements.filter(a => a.status === 'DECLINED' || a.status === 'REVOKED');
  }

  // ---------- accorder ----------

  ouvrirOctroi(accord: PartnerEngagement): void {
    this.grantingId = accord.id;
    // La portée proposée est celle que l'éditeur a demandée : la corriger vers
    // le bas est un geste conscient, et vers le haut aussi.
    this.draft = { ...PartnerAccessComponent.emptyDraft(), scope: accord.scope };
  }

  annulerOctroi(): void {
    this.grantingId = null;
    this.draft = PartnerAccessComponent.emptyDraft();
  }

  /** Vrai quand le brouillon peut partir : un terme dans le futur, et rien d'autre. */
  get draftValid(): boolean {
    if (!this.draft.endsOn) {
      return false;
    }
    const terme = new Date(this.draft.endsOn + 'T00:00:00');
    if (Number.isNaN(terme.getTime())) {
      return false;
    }
    const aujourdhui = new Date();
    aujourdhui.setHours(0, 0, 0, 0);
    return terme.getTime() > aujourdhui.getTime();
  }

  accorder(accord: PartnerEngagement): void {
    if (!this.draftValid) {
      return;
    }
    this.busyId = accord.id;
    this.partner.grant(accord.id, {
      scope: this.draft.scope,
      endsOn: this.draft.endsOn,
      purpose: this.draft.purpose.trim() || null
    }).subscribe({
      next: maj => {
        this.remplacer(maj);
        this.annulerOctroi();
        this.busyId = null;
        this.snack.open(
          $localize`:@@partner.access.granted:Accès accordé, et il prendra fin de lui-même à son terme.`,
          $localize`:@@common.close:Fermer`, { duration: 5000 });
      },
      error: (err: unknown) => this.echec(err,
        $localize`:@@partner.access.grant-failed:L’accès n’a pas pu être accordé.`)
    });
  }

  // ---------- refuser, reprendre ----------

  refuser(accord: PartnerEngagement): void {
    this.busyId = accord.id;
    this.partner.decline(accord.id).subscribe({
      next: maj => {
        this.remplacer(maj);
        this.busyId = null;
        this.snack.open(
          $localize`:@@partner.access.declined:Proposition refusée. Aucun accès n’a été ouvert.`,
          $localize`:@@common.close:Fermer`, { duration: 5000 });
      },
      error: (err: unknown) => this.echec(err,
        $localize`:@@partner.access.decline-failed:La proposition n’a pas pu être refusée.`)
    });
  }

  reprendre(accord: PartnerEngagement): void {
    // Reprendre ferme un accès ouvert, parfois au milieu d'une mission : la
    // confirmation nomme le cabinet, pour qu'on ne reprenne pas le mauvais.
    this.dialog.open(ConfirmDialogComponent, {
      panelClass: 'qos-dialog-panel',
      data: {
        title: $localize`:@@partner.access.revoke-title:Reprendre cet accès ?`,
        message: $localize`:@@partner.access.revoke-message:Le cabinet perdra l’accès immédiatement. La décision reste dans l’historique.`,
        confirmLabel: $localize`:@@partner.access.revoke-action:Reprendre`,
        destructive: true
      }
    }).afterClosed().subscribe(ok => {
      if (!ok) {
        return;
      }
      this.busyId = accord.id;
      this.partner.revoke(accord.id).subscribe({
        next: maj => {
          this.remplacer(maj);
          this.busyId = null;
          this.snack.open(
            $localize`:@@partner.access.revoked:Accès repris.`,
            $localize`:@@common.close:Fermer`, { duration: 5000 });
        },
        error: (err: unknown) => this.echec(err,
          $localize`:@@partner.access.revoke-failed:L’accès n’a pas pu être repris.`)
      });
    });
  }

  // ---------- libellés ----------

  scopeLabel(scope: EngagementScope): string {
    return scope === 'INTERVENE'
      ? $localize`:@@partner.scope.intervene:Intervention`
      : $localize`:@@partner.scope.read:Consultation`;
  }

  statusLabel(accord: PartnerEngagement): string {
    switch (accord.status) {
      case 'DECLINED':
        return $localize`:@@partner.status.declined:Refusé`;
      case 'REVOKED':
        return $localize`:@@partner.status.revoked:Repris`;
      case 'GRANTED':
        return accord.openToday
          ? $localize`:@@partner.status.open:Ouvert`
          // Un accord accordé mais hors de ses dates n'ouvre rien : le dire,
          // sans quoi on croit l'accès actif alors qu'il commence plus tard.
          : $localize`:@@partner.status.scheduled:Accordé, hors période`;
      default:
        return $localize`:@@partner.status.proposed:En attente de votre décision`;
    }
  }

  // ---------- interne ----------

  private remplacer(maj: PartnerEngagement): void {
    this.engagements = this.engagements.map(a => a.id === maj.id ? maj : a);
  }

  private echec(err: unknown, parDefaut: string): void {
    this.busyId = null;
    this.snack.open(safeErrorMessage(err, parDefaut),
      $localize`:@@common.close:Fermer`, { duration: 8000 });
  }

  private static emptyDraft(): GrantDraft {
    const terme = new Date();
    terme.setMonth(terme.getMonth() + DEFAULT_GRANT_MONTHS);
    return { scope: 'READ', endsOn: terme.toISOString().slice(0, 10), purpose: '' };
  }
}
