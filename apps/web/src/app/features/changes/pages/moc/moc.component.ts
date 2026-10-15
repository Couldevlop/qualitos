import { HttpResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';

import { AuthzService } from '../../../../core/authz/authz.service';
import { mocClassificationLabel, mocErrorMessage, mocStatusLabel } from '../../moc.labels';
import { MocService } from '../../moc.service';
import { MocDetail, MocStatus, MocSummary } from '../../moc.types';
import { MocRequestDialogComponent, MocRequestDialogData } from '../moc-request-dialog/moc-request-dialog.component';

/** Un filtre de la colonne de gauche : un statut, tout, ou « à valider par moi ». */
export type MocFilter = 'ALL' | MocStatus | 'MINE';

/**
 * Gestion des changements (MOC, ADR 0087) : la liste des demandes à gauche,
 * filtrée par statut, et la demande sélectionnée à droite.
 *
 * <p>La sélection vit dans l'adresse (`/changes/:id`) : un lien de notification,
 * du registre des risques ou du tableau de bord SMI ouvre la demande
 * sélectionnée, et le retour arrière du navigateur revient à la précédente.
 */
@Component({
  selector: 'qos-moc',
  templateUrl: './moc.component.html',
  styleUrls: ['./moc.component.scss'],
  standalone: false
})
export class MocComponent implements OnInit {

  readonly filters: { value: MocFilter; label: string }[] = [
    { value: 'ALL', label: $localize`:@@moc.filter.all:Tous` },
    { value: 'DRAFT', label: mocStatusLabel('DRAFT') },
    { value: 'IN_ANALYSIS', label: mocStatusLabel('IN_ANALYSIS') },
    { value: 'IN_VALIDATION', label: mocStatusLabel('IN_VALIDATION') },
    { value: 'APPROVED', label: mocStatusLabel('APPROVED') },
    { value: 'REJECTED', label: mocStatusLabel('REJECTED') }
  ];

  items: MocSummary[] = [];
  filter: MocFilter = 'ALL';
  selectedId: string | null = null;
  detail: MocDetail | null = null;

  loading = false;
  failed = false;
  detailLoading = false;
  exporting = false;

  canRequest = false;
  isAdmin = false;

  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private readonly service: MocService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    authz: AuthzService
  ) {
    authz.can('change.request').pipe(takeUntilDestroyed()).subscribe(v => this.canRequest = v);
    // Les paramètres MOC appartiennent à l'administrateur du client (authz.manage).
    authz.can('authz.manage').pipe(takeUntilDestroyed()).subscribe(v => this.isAdmin = v);
  }

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(p => {
      this.selectedId = p.get('id');
      if (this.selectedId) {
        this.loadDetail(this.selectedId);
      } else {
        this.detail = null;
      }
    });
    this.reload();
  }

  // ---------- liste ----------

  reload(): void {
    this.loading = true;
    this.failed = false;
    this.service.list().subscribe({
      next: items => {
        this.items = items;
        this.loading = false;
        // Sans sélection, la plus récente s'ouvre : l'écran n'est jamais une moitié vide.
        if (!this.selectedId && items.length) {
          this.select(items[0], true);
        }
      },
      error: err => {
        this.loading = false;
        this.failed = true;
        this.fail(err);
      }
    });
  }

  get visible(): MocSummary[] {
    if (this.filter === 'ALL') return this.items;
    if (this.filter === 'MINE') return this.items.filter(i => i.awaitingMe);
    return this.items.filter(i => i.status === this.filter);
  }

  get awaitingMeCount(): number {
    return this.items.filter(i => i.awaitingMe).length;
  }

  count(f: MocFilter): number {
    if (f === 'ALL') return this.items.length;
    if (f === 'MINE') return this.awaitingMeCount;
    return this.items.filter(i => i.status === f).length;
  }

  setFilter(f: MocFilter): void {
    this.filter = f;
  }

  select(item: MocSummary, replace = false): void {
    if (item.id === this.selectedId) return;
    this.router.navigate(['/changes', item.id], { replaceUrl: replace });
  }

  statusLabel = mocStatusLabel;
  classificationLabel = mocClassificationLabel;

  trackById(_i: number, s: MocSummary): string {
    return s.id;
  }

  // ---------- demande sélectionnée ----------

  /** La lecture en cours : un clic sur une autre demande l'annule, sa réponse tardive ne s'affichera pas. */
  private detailSub?: Subscription;

  private loadDetail(id: string): void {
    this.detailLoading = true;
    this.detailSub?.unsubscribe();
    this.detailSub = this.service.get(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: d => {
        this.detail = d;
        this.detailLoading = false;
      },
      error: err => {
        this.detail = null;
        this.detailLoading = false;
        this.fail(err);
      }
    });
  }

  /** La demande a changé dans le panneau : la carte de gauche suit, sans relire toute la liste. */
  onChanged(d: MocDetail): void {
    this.detail = d;
    this.items = this.items.map(x => x.id !== d.id ? x : {
      ...x, title: d.title, type: d.type, status: d.status,
      classification: d.classification, score: d.score,
      awaitingMe: d.validations.some(v => v.canDecide)
    });
  }

  onDeleted(id: string): void {
    this.items = this.items.filter(i => i.id !== id);
    this.detail = null;
    this.selectedId = null;
    const suivante = this.items[0];
    if (suivante) {
      this.router.navigate(['/changes', suivante.id], { replaceUrl: true });
    } else {
      this.router.navigate(['/changes'], { replaceUrl: true });
    }
  }

  // ---------- actions ----------

  create(): void {
    this.dialog.open<MocRequestDialogComponent, MocRequestDialogData, MocDetail>(MocRequestDialogComponent, {
      data: {}, width: '640px', panelClass: 'qos-dialog-panel'
    }).afterClosed().subscribe(created => {
      if (!created) return;
      this.items = [{
        id: created.id, code: created.code, title: created.title, type: created.type, status: created.status,
        classification: created.classification, score: created.score, requesterName: created.requesterName,
        createdAt: created.createdAt, awaitingMe: false
      }, ...this.items];
      this.filter = 'ALL';
      this.router.navigate(['/changes', created.id]);
    });
  }

  /** Le classeur de toutes les demandes, nommé par le serveur. */
  exportXlsx(): void {
    if (this.exporting) return;
    this.exporting = true;
    this.service.exportXlsx().subscribe({
      next: response => {
        this.exporting = false;
        if (!response.body) {
          this.snack.open(this.exportFailed, undefined, { duration: 4000 });
          return;
        }
        this.saveAs(response.body, this.filenameOf(response) ?? 'demandes-de-changement.xlsx');
      },
      error: () => {
        this.exporting = false;
        this.snack.open(this.exportFailed, undefined, { duration: 4000 });
      }
    });
  }

  private readonly exportFailed = $localize`:@@moc.export-failed:L'extraction Excel a échoué.`;

  private filenameOf(response: HttpResponse<Blob>): string | null {
    const header = response.headers.get('Content-Disposition');
    const match = header ? /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(header) : null;
    return match ? decodeURIComponent(match[1]) : null;
  }

  private saveAs(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    link.click();
    URL.revokeObjectURL(url);
  }

  private fail(err: unknown): void {
    this.snack.open(mocErrorMessage(err, $localize`:@@moc.load-failed:La demande n'a pas pu être chargée.`),
      undefined, { duration: 5000 });
  }
}
