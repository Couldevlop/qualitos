import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Router } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { IndustryPacksService } from '../../industry-packs.service';
import { ActivationResponse, PackView } from '../../industry-packs.types';

/**
 * Catalogue des Industry Packs (cartes premium). Croise le catalogue
 * (GET '') avec les activations du tenant (GET '/my') pour afficher le badge
 * « Actif » et le bon bouton activer/désactiver.
 */
@Component({
  selector: 'qos-packs-list',
  templateUrl: './packs-list.component.html',
  styleUrls: ['./packs-list.component.scss'],
  standalone: false
})
export class PacksListComponent implements OnInit {

  views: PackView[] = [];
  filtered: PackView[] = [];
  loading = true;
  error = false;
  query = '';
  /** code du pack en cours d'activation/désactivation (désactive son bouton). */
  busy?: string;

  /** Rôles habilités à activer/désactiver un pack (admin tenant / plateforme). */
  private static readonly ADMIN_ROLES = ['super_admin', 'admin', 'tenant_admin', 'admin_tenant'];
  canManage = true;

  constructor(
    private readonly svc: IndustryPacksService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.computeRole();
    this.load();
  }

  private computeRole(): void {
    const roles = this.auth.snapshot()?.roles ?? [];
    // Role-gating : boutons réservés aux admins si un rôle admin est présent
    // dans le JWT. Si l'utilisateur n'a AUCUN rôle (contexte non typé), on
    // reste permissif pour ne pas bloquer le chantier.
    this.canManage = roles.length === 0
      || roles.some(r => PacksListComponent.ADMIN_ROLES.includes(r));
  }

  load(): void {
    this.loading = true;
    this.error = false;
    forkJoin({
      page: this.svc.list(),
      mine: this.svc.myActivations()
    }).subscribe({
      next: ({ page, mine }) => {
        const activeCodes = new Set(mine.map(a => a.packCode));
        this.views = page.content.map(pack => ({
          pack,
          manifest: this.svc.parseManifest(pack.manifestJson),
          active: activeCodes.has(pack.code)
        }));
        this.applyFilter();
        this.loading = false;
      },
      error: () => { this.error = true; this.loading = false; }
    });
  }

  applyFilter(): void {
    const q = this.query.trim().toLowerCase();
    if (!q) { this.filtered = this.views; return; }
    this.filtered = this.views.filter(v => {
      const hay = [
        v.pack.name, v.pack.code, v.pack.description ?? '',
        ...v.pack.tags, ...v.manifest.sectors
      ].join(' ').toLowerCase();
      return hay.includes(q);
    });
  }

  open(v: PackView): void {
    this.router.navigate(['/industry-packs', v.pack.code]);
  }

  activate(v: PackView, ev: Event): void {
    ev.stopPropagation();
    const by = this.auth.snapshot()?.userId;
    if (!by) return;
    this.busy = v.pack.code;
    this.svc.activate(v.pack.code, { activatedBy: by }).subscribe({
      next: res => {
        v.active = true;
        this.busy = undefined;
        this.snack.open(this.activationMessage(res), $localize`:@@common.ok:OK`, { duration: 4000 });
      },
      error: () => {
        this.busy = undefined;
        this.snack.open(
          $localize`:@@industry-packs.activate-error:Échec de l'activation du pack`,
          $localize`:@@common.close:Fermer`, { duration: 3500 });
      }
    });
  }

  /** Message snackbar : enrichi des compteurs de provisionnement SI présents. */
  private activationMessage(res: ActivationResponse): string {
    if (res.kpisCreated != null || res.kpisSkipped != null) {
      const created = res.kpisCreated ?? 0;
      const skipped = res.kpisSkipped ?? 0;
      return $localize`:@@industry-packs.activate-success-counts:Pack activé — ${created}:created: KPIs provisionnés, ${skipped}:skipped: ignorés`;
    }
    return $localize`:@@industry-packs.activate-success:Pack activé`;
  }

  deactivate(v: PackView, ev: Event): void {
    ev.stopPropagation();
    const by = this.auth.snapshot()?.userId;
    if (!by) return;
    const data: ConfirmDialogData = {
      title: $localize`:@@industry-packs.deactivate-title:Désactiver le pack ?`,
      message: $localize`:@@industry-packs.deactivate-message:Le pack sera désactivé pour ce tenant. Ses contenus restent disponibles dans le catalogue et peuvent être réactivés.`,
      confirmLabel: $localize`:@@industry-packs.deactivate:Désactiver`,
      cancelLabel: $localize`:@@common.cancel:Annuler`,
      destructive: true
    };
    this.dialog.open(ConfirmDialogComponent, { data, panelClass: 'qos-dialog-panel' })
      .afterClosed().subscribe(ok => {
        if (!ok) return;
        this.busy = v.pack.code;
        this.svc.deactivate(v.pack.code, by).subscribe({
          next: () => {
            v.active = false;
            this.busy = undefined;
            this.snack.open(
              $localize`:@@industry-packs.deactivate-success:Pack désactivé`,
              $localize`:@@common.ok:OK`, { duration: 3000 });
          },
          error: () => {
            this.busy = undefined;
            this.snack.open(
              $localize`:@@industry-packs.deactivate-error:Échec de la désactivation`,
              $localize`:@@common.close:Fermer`, { duration: 3500 });
          }
        });
      });
  }
}
