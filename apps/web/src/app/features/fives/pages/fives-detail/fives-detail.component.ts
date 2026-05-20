import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { FivesService } from '../../fives.service';
import { FiveSAuditResponse, FiveSAuditStatus, FiveSPillar } from '../../fives.types';
import {
  FivesEditDialogComponent,
  FivesEditDialogData
} from '../fives-edit-dialog/fives-edit-dialog.component';

// OWASP A03 — refuse malformed UUIDs client-side. Demo mock ids ("5s-1"…)
// are also accepted so the page stays usable with useMockApi=true.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

interface PillarRow {
  pillar: FiveSPillar;
  label: string;
  hint: string;
}

@Component({
  selector: 'qos-fives-detail',
  templateUrl: './fives-detail.component.html',
  styleUrls: ['./fives-detail.component.scss'],
  standalone: false
})
export class FivesDetailComponent implements OnInit {

  audit$!: Observable<FiveSAuditResponse | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string | null>(null);
  acting$ = new BehaviorSubject<boolean>(false);

  readonly pillars: PillarRow[] = [
    { pillar: 'SEIRI',    label: 'Seiri',    hint: 'Trier — éliminer l\'inutile' },
    { pillar: 'SEITON',   label: 'Seiton',   hint: 'Ranger — une place pour chaque chose' },
    { pillar: 'SEISO',    label: 'Seiso',    hint: 'Nettoyer — détecter les anomalies en nettoyant' },
    { pillar: 'SEIKETSU', label: 'Seiketsu', hint: 'Standardiser — figer les pratiques' },
    { pillar: 'SHITSUKE', label: 'Shitsuke', hint: 'Maintenir — discipliner & auditer' }
  ];

  /** Per-pillar score form (one FormGroup per pillar). */
  forms: Record<FiveSPillar, FormGroup> = {} as Record<FiveSPillar, FormGroup>;
  saving: Record<FiveSPillar, boolean> = {} as Record<FiveSPillar, boolean>;

  private auditId = '';
  private readonly reload$ = new Subject<void>();
  private isMockId(s: string): boolean { return /^5s-/.test(s); }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly fives: FivesService,
    private readonly snack: MatSnackBar,
    private readonly fb: FormBuilder,
    private readonly dialog: MatDialog
  ) {
    for (const p of this.pillars) {
      this.forms[p.pillar] = this.fb.nonNullable.group({
        score: [0, [Validators.required, Validators.min(0), Validators.max(10)]],
        note: ['']
      });
      this.saving[p.pillar] = false;
    }
  }

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open('Identifiant invalide.', 'OK', { duration: 3000 });
      this.router.navigate(['/fives']);
      return;
    }
    this.auditId = raw;
    this.audit$ = this.reload$.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(() => this.fives.getAudit(this.auditId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[fives-detail] getAudit failed', err?.status, err?.error?.title);
          this.error$.next(safeErrorMessage(err, 'Audit introuvable.'));
          return of(null);
        }),
        finalize(() => this.loading$.next(false))
      )),
      tap(a => {
        if (a) {
          for (const p of this.pillars) {
            const item = a.items.find(i => i.pillar === p.pillar);
            if (item) {
              this.forms[p.pillar].setValue({
                score: item.score,
                note: item.note ?? ''
              });
            }
          }
        }
      })
    );
    this.reload$.next();
  }

  goBack(): void {
    this.router.navigate(['/fives']);
  }

  openEdit(a: FiveSAuditResponse): void {
    const data: FivesEditDialogData = { audit: a };
    this.dialog
      .open(FivesEditDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(updated => { if (updated) this.reload$.next(); });
  }

  /** OWASP A04 — destructive action gated by a confirm dialog. */
  deleteAudit(zone: string): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: 'Supprimer cet audit ?',
        message: `L'audit de « ${zone} » et ses scores seront supprimés définitivement.`,
        confirmLabel: 'Supprimer',
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.fives.deleteAudit(this.auditId).subscribe({
        next: () => {
          this.snack.open('Audit supprimé.', 'OK', { duration: 2000 });
          this.router.navigate(['/fives']);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fives-detail] delete failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, 'Erreur lors de la suppression.'),
            'OK', { duration: 4000 }
          );
        }
      });
    });
  }

  score(pillar: FiveSPillar): void {
    if (this.saving[pillar]) return;
    const form = this.forms[pillar];
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }
    this.saving[pillar] = true;
    const { score, note } = form.getRawValue();
    this.fives
      .scorePillar(this.auditId, {
        pillar,
        score,
        note: note?.trim() || undefined
      })
      .pipe(finalize(() => (this.saving[pillar] = false)))
      .subscribe({
        next: () => {
          this.snack.open(`${pillar} enregistré.`, 'OK', { duration: 2000 });
          this.reload$.next();
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[fives-detail] score failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, 'Erreur lors de l\'enregistrement.'),
            'OK', { duration: 4000 }
          );
        }
      });
  }

  start(): void { this.transition('start'); }
  complete(): void { this.transition('complete'); }
  cancel(): void { this.transition('cancel'); }

  private transition(action: 'start' | 'complete' | 'cancel'): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    const call =
      action === 'start' ? this.fives.startAudit(this.auditId)
      : action === 'complete' ? this.fives.completeAudit(this.auditId)
      : this.fives.cancelAudit(this.auditId);
    call.pipe(finalize(() => this.acting$.next(false))).subscribe({
      next: () => {
        const label = action === 'start' ? 'démarré' : action === 'complete' ? 'clôturé' : 'annulé';
        this.snack.open(`Audit ${label}.`, 'OK', { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[fives-detail] transition failed', action, err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, 'Erreur lors de la transition.'),
          'OK', { duration: 4000 }
        );
      }
    });
  }

  isTerminal(status: FiveSAuditStatus): boolean {
    return status === 'COMPLETED' || status === 'CANCELLED';
  }

  canStart(status: FiveSAuditStatus): boolean { return status === 'DRAFT'; }
  canComplete(status: FiveSAuditStatus): boolean { return status === 'IN_PROGRESS'; }
  canScore(status: FiveSAuditStatus): boolean { return status === 'IN_PROGRESS' || status === 'DRAFT'; }

  badgeClass(status: FiveSAuditStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  scoreColor(score: number): string {
    if (score >= 8) return 'pillar-score pillar-score-high';
    if (score >= 5) return 'pillar-score pillar-score-mid';
    return 'pillar-score pillar-score-low';
  }

  /** Typed accessor for the score value of a given pillar (avoids TS4111 in the template). */
  scoreOf(pillar: FiveSPillar): number {
    const ctrl = this.forms[pillar].get('score');
    const v = ctrl?.value;
    return typeof v === 'number' ? v : 0;
  }
}
