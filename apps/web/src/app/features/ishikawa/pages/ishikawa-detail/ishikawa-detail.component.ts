import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { IshikawaService } from '../../ishikawa.service';
import {
  CauseCategory,
  IshikawaCauseResponse,
  IshikawaDiagramResponse,
  IshikawaStatus
} from '../../ishikawa.types';
import {
  IshikawaCauseDialogComponent,
  IshikawaCauseDialogData
} from '../ishikawa-cause-dialog/ishikawa-cause-dialog.component';

// OWASP A03 — refuse malformed route params client-side.
const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

interface CauseNode {
  cause: IshikawaCauseResponse;
  children: CauseNode[];
}

interface BranchView {
  category: CauseCategory;
  label: string;
  /** Top-level causes only (no parentId); children nested via tree(). */
  roots: CauseNode[];
}

@Component({
  selector: 'qos-ishikawa-detail',
  templateUrl: './ishikawa-detail.component.html',
  styleUrls: ['./ishikawa-detail.component.scss'],
  standalone: false
})
export class IshikawaDetailComponent implements OnInit {

  diagram$!: Observable<IshikawaDiagramResponse | null>;
  loading$ = new BehaviorSubject<boolean>(false);
  error$ = new BehaviorSubject<string | null>(null);

  private diagramId = '';
  private readonly reload$ = new Subject<void>();

  // For accepting either a UUID or the demo mock-store ids ("ish-1" …) so
  // the page works both against the real backend AND useMockApi=true.
  private isMockId(s: string): boolean {
    return /^ish-/.test(s);
  }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly ishikawa: IshikawaService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open('Identifiant invalide.', 'OK', { duration: 3000 });
      this.router.navigate(['/ishikawa']);
      return;
    }
    this.diagramId = raw;
    this.diagram$ = this.reload$.pipe(
      tap(() => { this.loading$.next(true); this.error$.next(null); }),
      switchMap(() => this.ishikawa.getDiagram(this.diagramId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[ishikawa-detail] getDiagram failed', err?.status, err?.error?.title);
          this.error$.next(safeErrorMessage(err, 'Diagramme introuvable.'));
          return of(null);
        }),
        finalize(() => this.loading$.next(false))
      ))
    );
    this.reload$.next();
  }

  goBack(): void {
    this.router.navigate(['/ishikawa']);
  }

  /**
   * OWASP A04 — destructive actions go through a confirm dialog. The backend
   * also revalidates ownership via the JWT tenant_id claim; the front guard
   * is UX defense-in-depth (prevent accidental clicks).
   */
  deleteDiagram(label: string): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: 'Supprimer ce diagramme ?',
        message: `« ${label} » et toutes ses causes seront supprimés définitivement.`,
        confirmLabel: 'Supprimer',
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.ishikawa.deleteDiagram(this.diagramId).subscribe({
        next: () => {
          this.snack.open('Diagramme supprimé.', 'OK', { duration: 2000 });
          this.router.navigate(['/ishikawa']);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[ishikawa-detail] delete failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, 'Erreur lors de la suppression.'),
            'OK', { duration: 4000 }
          );
        }
      });
    });
  }

  openAddCause(d: IshikawaDiagramResponse): void {
    const data: IshikawaCauseDialogData = { diagramId: d.id, mode: d.mode };
    this.dialog
      .open(IshikawaCauseDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(cause => {
        if (cause) {
          this.reload$.next();
        }
      });
  }

  /** Add a sub-cause (5-Whys child) of an existing cause. */
  openAddSubCause(d: IshikawaDiagramResponse, parent: IshikawaCauseResponse): void {
    const data: IshikawaCauseDialogData = {
      diagramId: d.id,
      mode: d.mode,
      parent: { id: parent.id, label: parent.label, category: parent.category }
    };
    this.dialog
      .open(IshikawaCauseDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(cause => {
        if (cause) this.reload$.next();
      });
  }

  /** Group causes by category and build a parent→children tree per branch. */
  branches(d: IshikawaDiagramResponse): BranchView[] {
    const order: { value: CauseCategory; label: string }[] = [
      { value: 'METHODS',      label: 'Méthodes' },
      { value: 'MANPOWER',     label: 'Main-d\'œuvre' },
      { value: 'MACHINES',     label: 'Machines' },
      { value: 'MATERIALS',    label: 'Matières' },
      { value: 'MEASUREMENTS', label: 'Mesures' },
      { value: 'ENVIRONMENT',  label: 'Milieu' }
    ];
    if (d.mode === 'SEVEN_M' || d.mode === 'EIGHT_M') {
      order.push({ value: 'MANAGEMENT', label: 'Management' });
    }
    if (d.mode === 'EIGHT_M') {
      order.push({ value: 'MONEY', label: 'Moyens financiers' });
    }
    const byParent = new Map<string, IshikawaCauseResponse[]>();
    for (const c of d.causes) {
      const key = c.parentId ?? '__root__';
      const arr = byParent.get(key);
      if (arr) arr.push(c); else byParent.set(key, [c]);
    }
    const buildNode = (c: IshikawaCauseResponse): CauseNode => ({
      cause: c,
      children: (byParent.get(c.id) ?? []).map(buildNode)
    });
    return order.map(o => ({
      category: o.value,
      label: o.label,
      roots: (byParent.get('__root__') ?? [])
        .filter(c => c.category === o.value)
        .map(buildNode)
    }));
  }

  statusBadge(status: IshikawaStatus): string {
    return 'badge badge-' + status.toLowerCase();
  }

  scoreClass(score?: number): string {
    if (score === undefined || score === null) return 'score score-none';
    if (score >= 0.7) return 'score score-high';
    if (score >= 0.4) return 'score score-mid';
    return 'score score-low';
  }
}
