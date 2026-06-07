import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute } from '@angular/router';
import { forkJoin } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { IndustryPacksService } from '../../industry-packs.service';
import { ActivationResponse, PackManifest, PackResponse } from '../../industry-packs.types';

/**
 * Détail riche d'un Industry Pack : en-tête (état + bouton activer/désactiver)
 * et onglets Normes / KPIs / Ishikawa / Poka-Yoke / Glossaire — tout depuis le
 * manifeste embarqué, aucun appel additionnel (ADR 0019 Phase 2).
 */
@Component({
  selector: 'qos-packs-detail',
  templateUrl: './packs-detail.component.html',
  styleUrls: ['./packs-detail.component.scss'],
  standalone: false
})
export class PacksDetailComponent implements OnInit {

  pack?: PackResponse;
  manifest?: PackManifest;
  active = false;
  loading = true;
  error = false;
  busy = false;
  code = '';

  readonly kpiCols = ['name', 'formula', 'unit', 'target', 'thresholds', 'owner'];

  private static readonly ADMIN_ROLES = ['super_admin', 'admin', 'tenant_admin', 'admin_tenant'];
  canManage = true;

  constructor(
    private readonly svc: IndustryPacksService,
    private readonly auth: AuthService,
    private readonly route: ActivatedRoute,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const roles = this.auth.snapshot()?.roles ?? [];
    this.canManage = roles.length === 0
      || roles.some(r => PacksDetailComponent.ADMIN_ROLES.includes(r));
    this.code = this.route.snapshot.paramMap.get('code') ?? '';
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = false;
    forkJoin({
      pack: this.svc.get(this.code),
      mine: this.svc.myActivations()
    }).subscribe({
      next: ({ pack, mine }) => {
        this.pack = pack;
        this.manifest = this.svc.parseManifest(pack.manifestJson);
        this.active = mine.some(a => a.packCode === pack.code);
        this.loading = false;
      },
      error: () => { this.error = true; this.loading = false; }
    });
  }

  activate(): void {
    const by = this.auth.snapshot()?.userId;
    if (!by || !this.pack) return;
    this.busy = true;
    this.svc.activate(this.pack.code, { activatedBy: by }).subscribe({
      next: res => {
        this.active = true;
        this.busy = false;
        this.snack.open(this.activationMessage(res), $localize`:@@common.ok:OK`, { duration: 4000 });
      },
      error: () => {
        this.busy = false;
        this.snack.open(
          $localize`:@@industry-packs.activate-error:Échec de l'activation du pack`,
          $localize`:@@common.close:Fermer`, { duration: 3500 });
      }
    });
  }

  private activationMessage(res: ActivationResponse): string {
    if (res.kpisCreated != null || res.kpisSkipped != null) {
      const created = res.kpisCreated ?? 0;
      const skipped = res.kpisSkipped ?? 0;
      return $localize`:@@industry-packs.activate-success-counts:Pack activé — ${created}:created: KPIs provisionnés, ${skipped}:skipped: ignorés`;
    }
    return $localize`:@@industry-packs.activate-success:Pack activé`;
  }

  deactivate(): void {
    const by = this.auth.snapshot()?.userId;
    if (!by || !this.pack) return;
    const data: ConfirmDialogData = {
      title: $localize`:@@industry-packs.deactivate-title:Désactiver le pack ?`,
      message: $localize`:@@industry-packs.deactivate-message:Le pack sera désactivé pour ce tenant. Ses contenus restent disponibles dans le catalogue et peuvent être réactivés.`,
      confirmLabel: $localize`:@@industry-packs.deactivate:Désactiver`,
      cancelLabel: $localize`:@@common.cancel:Annuler`,
      destructive: true
    };
    this.dialog.open(ConfirmDialogComponent, { data, panelClass: 'qos-dialog-panel' })
      .afterClosed().subscribe(ok => {
        if (!ok || !this.pack) return;
        this.busy = true;
        this.svc.deactivate(this.pack.code, by).subscribe({
          next: () => {
            this.active = false;
            this.busy = false;
            this.snack.open(
              $localize`:@@industry-packs.deactivate-success:Pack désactivé`,
              $localize`:@@common.ok:OK`, { duration: 3000 });
          },
          error: () => {
            this.busy = false;
            this.snack.open(
              $localize`:@@industry-packs.deactivate-error:Échec de la désactivation`,
              $localize`:@@common.close:Fermer`, { duration: 3500 });
          }
        });
      });
  }
}
