import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { MarketplaceService } from '../../marketplace.service';
import { CatalogEntry, InstallationView } from '../../marketplace.types';

/**
 * Catalogue public des packs marketplace PUBLIÉS. Croise le catalogue avec les
 * installations du tenant pour afficher le bon bouton (installer / désinstaller)
 * et la notation. Premium : cartes, prix, note, badges normes.
 */
@Component({
  selector: 'qos-marketplace-catalog',
  templateUrl: './catalog.component.html',
  styleUrls: ['./catalog.component.scss'],
  standalone: false
})
export class CatalogComponent implements OnInit {

  entries: CatalogEntry[] = [];
  filtered: CatalogEntry[] = [];
  loading = true;
  error = false;
  query = '';
  busy?: string;

  /** Rôles habilités à installer/désinstaller (admin tenant / plateforme). */
  private static readonly TENANT_ADMIN_ROLES = ['super_admin', 'admin', 'admin_tenant', 'tenant_admin'];
  canManage = true;

  constructor(
    private readonly svc: MarketplaceService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.computeRole();
    this.load();
  }

  private computeRole(): void {
    const roles = this.auth.snapshot()?.roles ?? [];
    this.canManage = roles.length === 0
      || roles.some(r => CatalogComponent.TENANT_ADMIN_ROLES.includes(r));
  }

  load(): void {
    this.loading = true;
    this.error = false;
    forkJoin({
      packs: this.svc.listPublished(),
      mine: this.svc.myInstallations()
    }).subscribe({
      next: ({ packs, mine }) => {
        const byPackId = new Map<string, InstallationView>();
        mine.forEach(i => byPackId.set(i.marketplacePackId, i));
        this.entries = packs.map(pack => ({
          pack,
          installation: byPackId.get(pack.id)
        }));
        this.applyFilter();
        this.loading = false;
      },
      error: () => { this.error = true; this.loading = false; }
    });
  }

  applyFilter(): void {
    const q = this.query.trim().toLowerCase();
    if (!q) { this.filtered = this.entries; return; }
    this.filtered = this.entries.filter(e => {
      const hay = [
        e.pack.title, e.pack.packId, e.pack.publisher,
        e.pack.description ?? '', e.pack.sector, ...e.pack.norms
      ].join(' ').toLowerCase();
      return hay.includes(q);
    });
  }

  priceLabel(e: CatalogEntry): string {
    if (e.pack.priceCents === 0) {
      return $localize`:@@marketplace.free:Gratuit`;
    }
    const amount = (e.pack.priceCents / 100).toFixed(2);
    return `${amount} ${e.pack.currency}`;
  }

  stars(e: CatalogEntry): number[] {
    const full = Math.round(e.pack.ratingAvg);
    return Array.from({ length: 5 }, (_, i) => (i < full ? 1 : 0));
  }

  install(e: CatalogEntry, ev: Event): void {
    ev.stopPropagation();
    this.busy = e.pack.id;
    this.svc.install(e.pack.id).subscribe({
      next: inst => {
        e.installation = inst;
        this.busy = undefined;
        this.snack.open(
          $localize`:@@marketplace.installed:Pack installé`,
          $localize`:@@common.ok:OK`, { duration: 3000 });
      },
      error: () => {
        this.busy = undefined;
        this.snack.open(
          $localize`:@@marketplace.install-error:Échec de l'installation`,
          $localize`:@@common.close:Fermer`, { duration: 3500 });
      }
    });
  }

  uninstall(e: CatalogEntry, ev: Event): void {
    ev.stopPropagation();
    const inst = e.installation;
    if (!inst) { return; }
    const data: ConfirmDialogData = {
      title: $localize`:@@marketplace.uninstall-title:Désinstaller le pack ?`,
      message: $localize`:@@marketplace.uninstall-message:Le pack sera désinstallé pour ce tenant. Vous pourrez le réinstaller depuis le catalogue.`,
      confirmLabel: $localize`:@@marketplace.uninstall:Désinstaller`,
      cancelLabel: $localize`:@@common.cancel:Annuler`,
      destructive: true
    };
    this.dialog.open(ConfirmDialogComponent, { data, panelClass: 'qos-dialog-panel' })
      .afterClosed().subscribe(ok => {
        if (!ok) { return; }
        this.busy = e.pack.id;
        this.svc.uninstall(inst.id).subscribe({
          next: () => {
            e.installation = undefined;
            this.busy = undefined;
            this.snack.open(
              $localize`:@@marketplace.uninstalled:Pack désinstallé`,
              $localize`:@@common.ok:OK`, { duration: 3000 });
          },
          error: () => {
            this.busy = undefined;
            this.snack.open(
              $localize`:@@marketplace.uninstall-error:Échec de la désinstallation`,
              $localize`:@@common.close:Fermer`, { duration: 3500 });
          }
        });
      });
  }

  rate(e: CatalogEntry, stars: number, ev: Event): void {
    ev.stopPropagation();
    this.busy = e.pack.id;
    this.svc.rate(e.pack.id, stars).subscribe({
      next: updated => {
        e.pack = updated;
        this.busy = undefined;
        this.snack.open(
          $localize`:@@marketplace.rated:Merci pour votre note`,
          $localize`:@@common.ok:OK`, { duration: 2500 });
      },
      error: () => {
        this.busy = undefined;
        this.snack.open(
          $localize`:@@marketplace.rate-error:Notation impossible (installez d'abord le pack)`,
          $localize`:@@common.close:Fermer`, { duration: 3500 });
      }
    });
  }
}
