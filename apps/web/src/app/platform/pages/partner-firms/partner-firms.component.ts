import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';

import { safeErrorMessage } from '../../../core/http/error-message';
import { PartnerService } from '../../../core/partner/partner.service';
import {
  EngagementScope, PartnerConsultant, PartnerEngagement, PartnerFirm
} from '../../../core/partner/partner.types';
import { TenantSummary } from '../../../features/admin/admin.types';
import { ConfirmDialogComponent } from '../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { ClientsService } from '../../clients.service';
import {
  FirmEditDialogComponent, FirmEditResult
} from '../firm-edit-dialog/firm-edit-dialog.component';

/** Le brouillon d'un cabinet. Le repère (`slug`) ne se corrige plus ensuite. */
export interface FirmDraft {
  slug: string;
  name: string;
  registrationNumber: string;
  contactEmail: string;
  contactPhone: string;
}

/** La forme d'un repère, comme le serveur l'admet : minuscules, chiffres, tiret. */
const SLUG = /^[a-z0-9][a-z0-9-]{1,39}$/;

/**
 * « Cabinets », dans la console de l'éditeur : qui sont les cabinets
 * partenaires, qui y travaille, et à quels clients ils ont été proposés.
 *
 * <p><b>Ce que l'éditeur peut, et ce qu'il ne peut pas.</b> Il déclare un
 * cabinet, inscrit ses consultants, et <i>propose</i> un client. Il n'ouvre
 * aucun accès : seul l'administrateur du client accorde, depuis son propre
 * paramétrage. C'est la même frontière que partout ailleurs dans la console —
 * l'éditeur définit le périmètre, le client est souverain dedans.
 *
 * <p><b>Pourquoi l'accord porte sur le cabinet et non sur la personne.</b> Un
 * client contracte avec un cabinet, pas avec un consultant nommé : si la
 * personne change en cours de mission, l'accès n'a pas à être redemandé. Qui
 * s'est réellement servi de l'accès est journalisé nominativement, ce qui donne
 * la traçabilité sans transformer chaque remplacement en nouvelle négociation.
 *
 * <p><b>Désactiver un cabinet ne touche pas ses accords.</b> Un cabinet inactif
 * n'ouvre plus rien — la vérification est faite à chaque accès — mais ses
 * accords restent tels quels : les effacer effacerait la trace des décisions
 * que des clients ont prises, et c'est celle qu'un auditeur vient lire.
 */
@Component({
  selector: 'qos-partner-firms',
  templateUrl: './partner-firms.component.html',
  styleUrls: ['./partner-firms.component.scss'],
  standalone: false
})
export class PartnerFirmsComponent implements OnInit {

  firms: PartnerFirm[] = [];
  loading = false;
  failed = false;

  /** Le cabinet déplié : ses consultants et son portefeuille. Un seul à la fois. */
  openFirmId: string | null = null;
  consultants: PartnerConsultant[] = [];
  portfolio: PartnerEngagement[] = [];
  detailLoading = false;

  /** Création d'un cabinet. */
  creating = false;
  draft: FirmDraft = PartnerFirmsComponent.emptyFirm();

  /** Inscription d'un consultant dans le cabinet déplié. */
  consultantName = '';
  consultantEmail = '';

  /** Proposition d'un client au cabinet déplié. */
  clients: TenantSummary[] = [];
  proposeTenantId = '';
  proposeScope: EngagementScope = 'READ';
  proposePurpose = '';

  busy = false;

  constructor(
    private readonly partner: PartnerService,
    private readonly clientsService: ClientsService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.loading = true;
    this.failed = false;
    this.partner.listFirms().subscribe({
      next: page => {
        this.firms = page.content;
        this.loading = false;
      },
      error: () => {
        this.firms = [];
        this.loading = false;
        this.failed = true;
      }
    });
  }

  // ---------- créer un cabinet ----------

  ouvrirCreation(): void {
    this.creating = true;
    this.draft = PartnerFirmsComponent.emptyFirm();
  }

  annulerCreation(): void {
    this.creating = false;
    this.draft = PartnerFirmsComponent.emptyFirm();
  }

  /** Vrai quand le brouillon peut partir : un repère valide et un nom. */
  get firmDraftValid(): boolean {
    return SLUG.test(this.draft.slug.trim()) && this.draft.name.trim().length >= 2;
  }

  creer(): void {
    if (!this.firmDraftValid) {
      return;
    }
    this.busy = true;
    this.partner.createFirm({
      slug: this.draft.slug.trim(),
      name: this.draft.name.trim(),
      registrationNumber: this.draft.registrationNumber.trim() || null,
      contactEmail: this.draft.contactEmail.trim() || null,
      contactPhone: this.draft.contactPhone.trim() || null
    }).subscribe({
      next: cabinet => {
        this.firms = [cabinet, ...this.firms];
        this.annulerCreation();
        this.busy = false;
      },
      error: (err: unknown) => this.echec(err,
        $localize`:@@platform.firms.create-failed:Le cabinet n’a pas pu être créé.`)
    });
  }

  // ---------- corriger la fiche ----------

  /**
   * Corrige la fiche d'un cabinet : raison sociale, immatriculation, contact.
   *
   * <p>Ces informations vieillissent — un cabinet déménage, change de référent,
   * régularise son immatriculation — et la console ne savait que les saisir une
   * fois. Le serveur acceptait déjà la correction et la consigne dans la piste
   * d'audit de la plateforme ; il n'y manquait que le chemin pour la faire.
   */
  corriger(cabinet: PartnerFirm): void {
    this.dialog.open<FirmEditDialogComponent, PartnerFirm, FirmEditResult>(
      FirmEditDialogComponent,
      { data: cabinet, panelClass: 'qos-dialog-panel', autoFocus: 'first-tabbable' }
    ).afterClosed().subscribe(fiche => {
      if (!fiche) return;
      this.busy = true;
      this.partner.updateFirm(cabinet.id, fiche).subscribe({
        next: maj => {
          this.firms = this.firms.map(f => f.id === maj.id ? maj : f);
          this.busy = false;
          this.snack.open($localize`:@@platform.firms.saved:Fiche du cabinet mise à jour.`,
            undefined, { duration: 4000 });
        },
        error: (err: unknown) => this.echec(err,
          $localize`:@@platform.firms.save-failed:La fiche du cabinet n’a pas pu être enregistrée.`)
      });
    });
  }

  // ---------- activer, désactiver ----------

  basculer(cabinet: PartnerFirm): void {
    if (cabinet.active) {
      // Désactiver ferme TOUS les accès du cabinet, d'un coup : la confirmation
      // dit combien de clients le perdent, sinon le geste paraît anodin.
      this.dialog.open(ConfirmDialogComponent, {
        panelClass: 'qos-dialog-panel',
        data: {
          title: $localize`:@@platform.firms.deactivate-title:Désactiver ce cabinet ?`,
          message: $localize`:@@platform.firms.deactivate-message:Ses consultants perdront immédiatement tous leurs accès. Les accords des clients sont conservés et reprendront effet si vous le réactivez.`,
          confirmLabel: $localize`:@@platform.firms.deactivate-action:Désactiver`,
          destructive: true
        }
      }).afterClosed().subscribe(ok => {
        if (ok) {
          this.appliquerActivation(cabinet, false);
        }
      });
      return;
    }
    this.appliquerActivation(cabinet, true);
  }

  private appliquerActivation(cabinet: PartnerFirm, actif: boolean): void {
    this.busy = true;
    this.partner.setFirmActive(cabinet.id, actif).subscribe({
      next: maj => {
        this.firms = this.firms.map(f => f.id === maj.id ? maj : f);
        this.busy = false;
      },
      error: (err: unknown) => this.echec(err,
        $localize`:@@platform.firms.toggle-failed:L’état du cabinet n’a pas pu être changé.`)
    });
  }

  // ---------- déplier un cabinet ----------

  basculerDetail(cabinet: PartnerFirm): void {
    if (this.openFirmId === cabinet.id) {
      this.openFirmId = null;
      return;
    }
    this.openFirmId = cabinet.id;
    this.consultants = [];
    this.portfolio = [];
    this.consultantName = '';
    this.consultantEmail = '';
    this.proposeTenantId = '';
    this.proposePurpose = '';
    this.chargerDetail(cabinet.id);
    this.chargerClients();
  }

  private chargerDetail(firmId: string): void {
    this.detailLoading = true;
    this.partner.listConsultants(firmId).subscribe({
      next: liste => this.consultants = liste,
      error: (err: unknown) => this.echec(err,
        $localize`:@@platform.firms.consultants-failed:Les consultants n’ont pas pu être chargés.`)
    });
    this.partner.portfolioOf(firmId).subscribe({
      next: accords => {
        this.portfolio = accords;
        this.detailLoading = false;
      },
      error: (err: unknown) => {
        this.detailLoading = false;
        this.echec(err,
          $localize`:@@platform.firms.portfolio-failed:Le portefeuille n’a pas pu être chargé.`);
      }
    });
  }

  private chargerClients(): void {
    if (this.clients.length) {
      return;
    }
    this.clientsService.list().subscribe({
      next: page => this.clients = page.content.filter(c => c.active),
      // Pas d'alerte : sans la liste, la proposition est simplement
      // indisponible, et le reste de l'écran reste utilisable.
      error: () => this.clients = []
    });
  }

  // ---------- inscrire un consultant ----------

  get consultantValid(): boolean {
    return this.consultantName.trim().length >= 2 && this.consultantEmail.includes('@');
  }

  inscrire(): void {
    if (!this.openFirmId || !this.consultantValid) {
      return;
    }
    this.busy = true;
    this.partner.addConsultant(this.openFirmId, {
      displayName: this.consultantName.trim(),
      email: this.consultantEmail.trim()
    }).subscribe({
      next: fiche => {
        this.consultants = [...this.consultants, fiche];
        this.consultantName = '';
        this.consultantEmail = '';
        this.busy = false;
        this.majCompteur(fiche.firmId, 1);
        this.snack.open(
          $localize`:@@platform.firms.consultant-added:Consultant inscrit. Son accès s’ouvrira à sa première connexion, par ce courriel.`,
          $localize`:@@common.close:Fermer`, { duration: 6000 });
      },
      error: (err: unknown) => this.echec(err,
        $localize`:@@platform.firms.consultant-failed:Le consultant n’a pas pu être inscrit.`)
    });
  }

  basculerConsultant(fiche: PartnerConsultant): void {
    if (!this.openFirmId) {
      return;
    }
    this.busy = true;
    this.partner.setConsultantActive(this.openFirmId, fiche.id, !fiche.active).subscribe({
      next: maj => {
        this.consultants = this.consultants.map(c => c.id === maj.id ? maj : c);
        this.busy = false;
        this.majCompteur(maj.firmId, maj.active ? 1 : -1);
      },
      error: (err: unknown) => this.echec(err,
        $localize`:@@platform.firms.consultant-toggle-failed:L’état du consultant n’a pas pu être changé.`)
    });
  }

  // ---------- proposer un client ----------

  get proposeValid(): boolean {
    return this.proposeTenantId.length > 0;
  }

  proposer(): void {
    if (!this.openFirmId || !this.proposeValid) {
      return;
    }
    this.busy = true;
    this.partner.propose(this.openFirmId, {
      tenantId: this.proposeTenantId,
      scope: this.proposeScope,
      purpose: this.proposePurpose.trim() || null
    }).subscribe({
      next: accord => {
        this.portfolio = [accord, ...this.portfolio];
        this.proposeTenantId = '';
        this.proposePurpose = '';
        this.busy = false;
        this.snack.open(
          // Le message dit explicitement qu'aucun accès n'est ouvert : c'est la
          // frontière de l'écran, et elle doit être dite au moment du geste.
          $localize`:@@platform.firms.proposed:Client proposé. Aucun accès n’est ouvert : son administrateur décide.`,
          $localize`:@@common.close:Fermer`, { duration: 7000 });
      },
      error: (err: unknown) => this.echec(err,
        $localize`:@@platform.firms.propose-failed:Le client n’a pas pu être proposé.`)
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
      case 'GRANTED':
        return accord.openToday
          ? $localize`:@@partner.status.open:Ouvert`
          : $localize`:@@partner.status.scheduled:Accordé, hors période`;
      case 'DECLINED':
        return $localize`:@@partner.status.declined:Refusé`;
      case 'REVOKED':
        return $localize`:@@partner.status.revoked:Repris`;
      default:
        return $localize`:@@platform.firms.awaiting:En attente du client`;
    }
  }

  clientName(tenantId: string): string {
    return this.clients.find(c => c.id === tenantId)?.name || tenantId;
  }

  // ---------- interne ----------

  /** Le compteur de la carte suit l'inscription, sans recharger toute la liste. */
  private majCompteur(firmId: string, delta: number): void {
    this.firms = this.firms.map(f => f.id === firmId
      ? { ...f, consultantCount: Math.max(0, f.consultantCount + delta) }
      : f);
  }

  private echec(err: unknown, parDefaut: string): void {
    this.busy = false;
    this.snack.open(safeErrorMessage(err, parDefaut),
      $localize`:@@common.close:Fermer`, { duration: 8000 });
  }

  private static emptyFirm(): FirmDraft {
    return { slug: '', name: '', registrationNumber: '', contactEmail: '', contactPhone: '' };
  }
}
