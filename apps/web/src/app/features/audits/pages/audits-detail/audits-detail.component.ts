import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, Observable, of, Subject } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { deferredView } from '../../../../core/rx/deferred-view';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { AuditsService } from '../../audits.service';
import {
  AuditsEditDialogComponent,
  AuditsEditDialogData
} from '../audits-edit-dialog/audits-edit-dialog.component';
import {
  AuditsFindingDialogComponent,
  AuditsFindingDialogData
} from '../audits-finding-dialog/audits-finding-dialog.component';
import { AuditPlanResponse, AuditStatus, ChecklistItemResponse, FindingResponse } from '../../audits.types';
import { leadAuditorDisplay } from '../../lead-auditor';
import {
  CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened
} from '../../../capa/pages/capa-create-dialog/capa-create-dialog.component';
import {
  ChecklistFromStandardDialogComponent,
  ChecklistFromStandardDialogData
} from '../checklist-from-standard-dialog/checklist-from-standard-dialog.component';
import {
  AuditsChecklistDialogComponent,
  AuditsChecklistDialogData
} from '../audits-checklist-dialog/audits-checklist-dialog.component';
import {
  AuditsResponseDialogComponent,
  AuditsResponseDialogData
} from '../audits-response-dialog/audits-response-dialog.component';

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

@Component({
  selector: 'qos-audits-detail',
  templateUrl: './audits-detail.component.html',
  styleUrls: ['./audits-detail.component.scss'],
  standalone: false
})
export class AuditsDetailComponent implements OnInit {

  readonly checklistColumns = ['orderIndex', 'question', 'clauseRef', 'weight', 'response', 'actions'];
  readonly findingColumns = ['type', 'description', 'clauseRef', 'raisedAt'];

  readonly notFoundLabel = $localize`:@@audits.detail.not-found:Audit introuvable`;

  plan$!: Observable<AuditPlanResponse | null>;
  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);
  acting$ = new BehaviorSubject<boolean>(false);
  /** Rapport émis : il verrouille les documents de l'audit. */
  reportIssued = false;

  private planId = '';
  private readonly reload$ = new BehaviorSubject<void>(undefined);
  private isMockId(s: string): boolean { return /^a-?[a-z0-9-]+$/i.test(s) && /^a[0-9-]/i.test(s); }

  constructor(
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly audits: AuditsService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly auth: AuthService
  ) {}

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id') ?? '';
    if (!UUID_RE.test(raw) && !this.isMockId(raw)) {
      this.snack.open($localize`:@@common.invalid-id:Identifiant invalide.`, $localize`:@@common.ok:OK`, { duration: 3000 });
      this.router.navigate(['/audits']);
      return;
    }
    this.planId = raw;
    this.plan$ = this.reload$.pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.audits.getPlan(this.planId).pipe(
        catchError(err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-detail] getPlan failed', err?.status, err?.error?.title);
          this.errorState$.next(safeErrorMessage(err, $localize`:@@audits.detail.plan-not-found:Plan d'audit introuvable.`));
          return of(null);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      shareReplay({ bufferSize: 1, refCount: false }) // refCount:false : gated sur loading, evite la boucle teardown
    );
    this.reload$.next();
  }

  goBack(): void { this.router.navigate(['/audits']); }

  openEdit(p: AuditPlanResponse): void {
    const data: AuditsEditDialogData = { plan: p };
    this.dialog
      .open(AuditsEditDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(updated => { if (updated) this.reload$.next(); });
  }

  /** OWASP A04 — destructive action gated by a confirm dialog. */
  deletePlan(title: string): void {
    this.dialog.open(ConfirmDialogComponent, {
      data: <ConfirmDialogData>{
        title: $localize`:@@audits.detail.delete-title:Supprimer ce plan ?`,
        message: $localize`:@@audits.detail.delete-message:« ${title}:title: », sa checklist et ses findings seront supprimés définitivement.`,
        confirmLabel: $localize`:@@common.delete:Supprimer`,
        destructive: true
      },
      autoFocus: false,
      restoreFocus: true
    }).afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.audits.deletePlan(this.planId).subscribe({
        next: () => {
          this.snack.open($localize`:@@audits.detail.deleted:Plan supprimé.`, $localize`:@@common.ok:OK`, { duration: 2000 });
          this.router.navigate(['/audits']);
        },
        error: err => {
          // eslint-disable-next-line no-console
          console.warn('[audits-detail] delete failed', err?.status, err?.error?.title);
          this.snack.open(
            safeErrorMessage(err, $localize`:@@common.error-delete:Erreur lors de la suppression.`),
            $localize`:@@common.ok:OK`, { duration: 4000 }
          );
        }
      });
    });
  }

  openResponse(item: ChecklistItemResponse): void {
    const data: AuditsResponseDialogData = {
      planId: this.planId,
      itemId: item.id,
      question: item.question,
      clauseRef: item.clauseRef,
      initialResponse: item.response,
      initialConformant: item.conformant
    };
    this.dialog
      .open(AuditsResponseDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(updated => {
        if (updated) this.reload$.next();
      });
  }

  openAddFinding(checklistItemId?: string): void {
    const data: AuditsFindingDialogData = { planId: this.planId, checklistItemId };
    this.dialog
      .open(AuditsFindingDialogComponent, {
        data, autoFocus: 'first-tabbable', restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(finding => { if (finding) this.reload$.next(); });
  }

  findingBadge(type: string): string {
    return 'finding-badge finding-' + type.toLowerCase().replace('_', '-');
  }

  /**
   * Proposé seulement tant que l'audit se PRÉPARE et que sa checklist est vide :
   * le serveur refuse dans tous les autres cas (409), et offrir le bouton
   * reviendrait à promettre un geste qui échoue.
   */
  canGenerateFromStandard(p: AuditPlanResponse): boolean {
    return p.status === 'PLANNED' && !(p.checklist?.length);
  }

  openGenerateFromStandard(): void {
    const data: ChecklistFromStandardDialogData = { planId: this.planId };
    this.dialog
      .open(ChecklistFromStandardDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(generated => {
        // 0 question générée : le référentiel était vide, mais l'audit le vise
        // désormais — la fiche a donc changé et doit être relue.
        if (generated !== undefined) this.reload$.next();
      });
  }

  openAddChecklist(): void {
    const data: AuditsChecklistDialogData = { planId: this.planId };
    this.dialog
      .open(AuditsChecklistDialogComponent, {
        data,
        autoFocus: 'first-tabbable',
        restoreFocus: true,
        panelClass: 'qos-dialog-panel'
      })
      .afterClosed()
      .subscribe(item => {
        if (item) this.reload$.next();
      });
  }

  start(): void { this.transition('start'); }
  complete(): void { this.transition('complete'); }
  cancel(): void { this.transition('cancel'); }

  /** L'auditeur responsable, par son nom — jamais l'identifiant de son compte. */
  leadAuditorLabel(p: AuditPlanResponse): string {
    return leadAuditorDisplay(p, this.auth.snapshot());
  }

  /**
   * Pourquoi « Ajouter un constat » est barré, s'il l'est.
   *
   * Le serveur n'accepte un constat que pendant l'audit (IN_PROGRESS). Le bouton
   * restait actif sur un audit PLANIFIÉ — l'état de tout audit qui vient d'être
   * créé — et le refus revenait en « État incompatible — rechargez la page »,
   * qu'aucun rechargement ne levait. La règle est désormais dite à l'écran, avec
   * le geste qui la lève.
   */
  findingBlocker(p: AuditPlanResponse): string | null {
    switch (p.status) {
      case 'IN_PROGRESS':
        return null;
      case 'PLANNED':
        return p.checklist?.length
          ? $localize`:@@audits.detail.finding-blocked-planned:Les constats se relèvent pendant l'audit : démarrez-le pour en ajouter.`
          : $localize`:@@audits.detail.finding-blocked-no-checklist:Les constats se relèvent pendant l'audit. Ajoutez d'abord une question à la checklist, puis démarrez l'audit.`;
      case 'COMPLETED':
        return $localize`:@@audits.detail.finding-blocked-completed:Audit clôturé : plus aucun constat ne peut être ajouté.`;
      default:
        return $localize`:@@audits.detail.finding-blocked-cancelled:Audit annulé : plus aucun constat ne peut être ajouté.`;
    }
  }

  /** Le démarrage exige au moins une question : le serveur refuse sinon. */
  startBlocker(p: AuditPlanResponse): string | null {
    if (p.status !== 'PLANNED') return null;
    return p.checklist?.length
      ? null
      : $localize`:@@audits.detail.start-blocked:Ajoutez au moins une question à la checklist pour démarrer l'audit.`;
  }

  /** Raison du verrou des documents : audit annulé ou rapport émis. */
  documentsLockReason(p: AuditPlanResponse): string | null {
    if (p.status === 'CANCELLED') {
      return $localize`:@@audits.detail.documents-locked-cancelled:Audit annulé : ses documents ne se modifient plus.`;
    }
    return this.reportIssued
      ? $localize`:@@audits.detail.documents-locked-issued:Rapport émis : les documents de l'audit sont figés avec lui.`
      : null;
  }

  /**
   * Le panneau du rapport annonce son état après un chargement. Différé d'une
   * microtâche : l'annonce peut tomber pendant la détection de changements du
   * parent, et modifier une liaison à ce moment-là lèverait NG0100.
   */
  onReportIssued(issued: boolean): void {
    if (issued === this.reportIssued) return;
    queueMicrotask(() => (this.reportIssued = issued));
  }

  private transition(action: 'start' | 'complete' | 'cancel'): void {
    if (this.acting$.value) return;
    this.acting$.next(true);
    const call =
      action === 'start' ? this.audits.startPlan(this.planId)
      : action === 'complete' ? this.audits.completePlan(this.planId)
      : this.audits.cancelPlan(this.planId);
    call.pipe(finalize(() => this.acting$.next(false))).subscribe({
      next: () => {
        const msg = action === 'start'
          ? $localize`:@@audits.detail.started-msg:Audit démarré.`
          : action === 'complete'
            ? $localize`:@@audits.detail.completed-msg:Audit clôturé.`
            : $localize`:@@audits.detail.cancelled-msg:Audit annulé.`;
        this.snack.open(msg, $localize`:@@common.ok:OK`, { duration: 2000 });
        this.reload$.next();
      },
      error: err => {
        // eslint-disable-next-line no-console
        console.warn('[audits-detail] transition failed', action, err?.status, err?.error?.title);
        this.snack.open(
          safeErrorMessage(err, $localize`:@@audits.detail.transition-error:Erreur lors de la transition.`),
          $localize`:@@common.ok:OK`, { duration: 4000 }
        );
      }
    });
  }

  canStart(s: AuditStatus): boolean { return s === 'PLANNED'; }
  canComplete(s: AuditStatus): boolean { return s === 'IN_PROGRESS'; }
  isTerminal(s: AuditStatus): boolean { return s === 'COMPLETED' || s === 'CANCELLED'; }

  statusBadge(s: AuditStatus): string { return 'badge badge-' + s.toLowerCase(); }
  scoreClass(s?: number): string {
    if (s == null) return 'score';
    if (s >= 85) return 'score score-high';
    if (s >= 70) return 'score score-mid';
    return 'score score-low';
  }

  // ---- CAPA d'un constat (ADR 0088) ------------------------------------------

  /** Un écart sans CAPA appelle le bouton ; une observation ou une piste, non. */
  canOpenFindingCapa(f: FindingResponse): boolean {
    return (f.type === 'MINOR_NC' || f.type === 'MAJOR_NC') && !f.capaId && !f.pendingSync;
  }

  /**
   * Ouvre LA fenêtre de création CAPA, source verrouillée sur ce constat : le
   * serveur crée le dossier, son action unique et le lien au constat d'une
   * seule écriture. La référence affichée est celle que l'auditeur cite.
   */
  openFindingCapa(p: AuditPlanResponse, f: FindingResponse): void {
    const rang = (p.findings ?? []).findIndex(x => x.id === f.id) + 1;
    const reference = $localize`:@@audits.detail.finding-capa-ref:${p.reference}:audit: / constat ${rang}:rank:`;
    const data: CapaCreateDialogData = {
      source: { type: 'AUDIT', reference },
      prefill: {
        title: f.description.slice(0, 255),
        description: f.clauseRef ? `${p.title} — ${f.clauseRef}` : p.title,
        type: 'CORRECTIVE',
        criticity: f.type === 'MAJOR_NC' ? 'HIGH' : 'MEDIUM'
      },
      open: request => this.audits.openFindingCapa(p.id, f.id, request)
        .pipe(map(r => ({ capaId: r.capaId })))
    };
    this.dialog.open<CapaCreateDialogComponent, CapaCreateDialogData, CapaOpened>(CapaCreateDialogComponent, {
      data, autoFocus: 'first-tabbable', restoreFocus: true, panelClass: 'qos-dialog-panel'
    }).afterClosed().subscribe(opened => { if (opened) this.reload$.next(); });
  }
}
