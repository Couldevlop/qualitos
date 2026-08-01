import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { PageEvent } from '@angular/material/paginator';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { AuditLogService } from '../../audit-log.service';
import {
  ActorType, AuditEvent, AuditEventFilter, AuditEventPage, ChainVerification, EffectiveCriterion
} from '../../audit-log.types';

// Le serveur rabote silencieusement toute taille de page au-delà de 100
// (`spring.data.web.pageable.max-page-size: 100`) : il ne renvoie AUCUNE erreur.
// Proposer 200 rendait la moitié du journal inatteignable — le paginateur comptait
// ses pages sur 200 pendant que le serveur en servait 100. Sur un journal d'audit,
// une preuve silencieusement hors d'atteinte est un défaut de conformité (§11.5).
const PAGE_SIZE_OPTIONS = [25, 50, 100];
const MAX_PAGE_SIZE = 100;

/** Le serveur type `resourceId`/`actorUserId` en UUID : une saisie libre partirait en 400. */
const UUID_PATTERN = /^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$/;

/** Page vide servie après une erreur : remet les compteurs à zéro sous le bandeau. */
const EMPTY_PAGE: AuditEventPage = {
  content: [], totalElements: 0, totalPages: 0, number: 0, size: 0
};

/** Bornes de séquence de la page affichée, pour amorcer la vérification de chaîne. */
interface SeqRange {
  from: number;
  to: number;
}

/**
 * Journal d'audit — écran de consultation (§11.5).
 *
 * Trois partis pris :
 *
 * 1. APPEND-ONLY assumé jusque dans l'UI : aucune action d'écriture, de modification ni
 *    de suppression n'est offerte. L'ancrage blockchain est montré (référence de
 *    transaction), jamais déclenché depuis ici.
 * 2. Les filtres NE SE COMBINENT PAS côté serveur (cascade if/else). Plutôt que de laisser
 *    l'utilisateur déduire un ET inexistant de ses résultats, l'écran annonce en clair le
 *    critère qui sera réellement appliqué.
 * 3. La vérification d'intégrité travaille sur une fenêtre de séquences, pas sur des
 *    dates : elle est donc amorcée depuis les bornes de la page affichée, ce qui évite à
 *    l'utilisateur de deviner des numéros.
 */
@Component({
  selector: 'qos-audit-log',
  templateUrl: './audit-log.component.html',
  styleUrls: ['./audit-log.component.scss'],
  standalone: false
})
export class AuditLogComponent implements OnInit {

  readonly displayedColumns = ['seq', 'occurredAt', 'actor', 'action', 'resource', 'integrity', 'detail'];
  readonly pageSizeOptions = PAGE_SIZE_OPTIONS;

  pageIndex = 0;
  pageSize = 50;

  readonly filterForm = new FormGroup({
    action: new FormControl('', { nonNullable: true }),
    resourceType: new FormControl('', { nonNullable: true }),
    resourceId: new FormControl('', {
      nonNullable: true, validators: [Validators.pattern(UUID_PATTERN)]
    }),
    actorUserId: new FormControl('', {
      nonNullable: true, validators: [Validators.pattern(UUID_PATTERN)]
    }),
    from: new FormControl('', { nonNullable: true }),
    to: new FormControl('', { nonNullable: true })
  });

  readonly verifyForm = new FormGroup({
    fromSeq: new FormControl<number | null>(null, [Validators.required, Validators.min(1)]),
    toSeq: new FormControl<number | null>(null, [Validators.required, Validators.min(1)])
  });

  events$!: Observable<AuditEvent[]>;
  total$!: Observable<number>;
  /** `null` quand la page est vide : il n'y a alors aucune fenêtre à vérifier. */
  range$!: Observable<SeqRange | null>;
  criterion$!: Observable<EffectiveCriterion>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  /** Événement ouvert dans le panneau de détail (chargé à part pour avoir le payload). */
  detail: AuditEvent | null = null;
  detailLoading = false;
  detailError: string | null = null;
  private detailPendingId: string | null = null;

  verifyResult: ChainVerification | null = null;
  verifying = false;
  verifyError: string | null = null;

  private readonly applied$ = new BehaviorSubject<AuditEventFilter>({});
  private readonly paging$ = new BehaviorSubject<{ index: number; size: number }>({ index: 0, size: 50 });

  constructor(private readonly svc: AuditLogService) {}

  ngOnInit(): void {
    const page$ = combineLatest([this.applied$, this.paging$]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([filter, p]) => this.svc.list(filter, p.index, p.size).pipe(
        catchError(err => {
          this.errorState$.next(safeErrorMessage(err,
            $localize`:@@admin.audit.load-error:Impossible de charger le journal d'audit.`));
          return of(EMPTY_PAGE);
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      // refCount:false : le tableau, le compteur du paginateur et le bloc d'intégrité
      // s'abonnent séparément, certains derrière un *ngIf — un abonnement tardif ne doit
      // pas relancer une requête sur un journal potentiellement volumineux.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.events$ = page$.pipe(map(p => p.content));
    this.total$ = page$.pipe(map(p => p.totalElements));
    this.range$ = page$.pipe(map(p => this.rangeOf(p.content)));
    this.criterion$ = this.applied$.pipe(map(f => this.svc.effectiveCriterion(f)));
  }

  // ---- Filtres ---------------------------------------------------------------

  /** Un filtre est actif dès qu'un champ est renseigné ; sert à activer « Réinitialiser ». */
  get hasFilter(): boolean {
    return Object.values(this.filterForm.getRawValue()).some(v => !!v);
  }

  apply(): void {
    if (this.filterForm.invalid) {
      this.filterForm.markAllAsTouched();
      return;
    }
    this.closeDetail();
    this.pageIndex = 0;
    this.paging$.next({ index: 0, size: this.pageSize });
    this.applied$.next(this.buildFilter());
  }

  reset(): void {
    this.filterForm.reset();
    this.closeDetail();
    this.pageIndex = 0;
    this.paging$.next({ index: 0, size: this.pageSize });
    this.applied$.next({});
  }

  /**
   * Traduit le formulaire en critères serveur. Les champs `datetime-local` donnent une
   * heure locale sans fuseau (« 2026-07-31T08:00 ») que `Instant` refuserait : on la
   * convertit en instant absolu.
   */
  private buildFilter(): AuditEventFilter {
    const raw = this.filterForm.getRawValue();
    const filter: AuditEventFilter = {};
    if (raw.action) filter.action = raw.action.trim();
    if (raw.resourceType) filter.resourceType = raw.resourceType.trim();
    if (raw.resourceId) filter.resourceId = raw.resourceId.trim();
    if (raw.actorUserId) filter.actorUserId = raw.actorUserId.trim();
    const from = this.toInstant(raw.from);
    const to = this.toInstant(raw.to);
    if (from) filter.from = from;
    if (to) filter.to = to;
    return filter;
  }

  private toInstant(local: string): string | undefined {
    if (!local) return undefined;
    const d = new Date(local);
    return isNaN(d.getTime()) ? undefined : d.toISOString();
  }

  /** Message d'aide : quel critère le serveur retiendra réellement. */
  criterionLabel(criterion: EffectiveCriterion): string {
    switch (criterion) {
      case 'resource':
        return $localize`:@@admin.audit.criterion-resource:Filtre appliqué : la ressource ciblée. Les autres critères sont ignorés par le serveur.`;
      case 'action':
        return $localize`:@@admin.audit.criterion-action:Filtre appliqué : le type d'action. Les autres critères sont ignorés par le serveur.`;
      case 'actor':
        return $localize`:@@admin.audit.criterion-actor:Filtre appliqué : l'acteur. Les autres critères sont ignorés par le serveur.`;
      case 'period':
        return $localize`:@@admin.audit.criterion-period:Filtre appliqué : la période. Renseignez les deux bornes pour qu'elle soit prise en compte.`;
      default:
        return $localize`:@@admin.audit.criterion-none:Aucun filtre actif : le journal complet est affiché, du plus récent au plus ancien.`;
    }
  }

  onPage(e: PageEvent): void {
    this.pageIndex = Math.max(0, e.pageIndex);
    this.pageSize = Math.min(MAX_PAGE_SIZE, Math.max(1, e.pageSize));
    this.closeDetail();
    this.paging$.next({ index: this.pageIndex, size: this.pageSize });
  }

  // ---- Détail ----------------------------------------------------------------

  /**
   * Charge le détail complet. La ligne du tableau ne porte pas le payload en pratique
   * lisible ; on relit l'événement pour afficher payload, IP, agent et chaînage.
   */
  openDetail(event: AuditEvent): void {
    if (this.detailPendingId === event.id) return;
    this.detailPendingId = event.id;
    this.detailLoading = true;
    this.detailError = null;
    this.detail = event;
    this.svc.get(event.id).pipe(
      finalize(() => { this.detailLoading = false; this.detailPendingId = null; })
    ).subscribe({
      next: full => { this.detail = full; },
      error: err => {
        this.detailError = safeErrorMessage(err,
          $localize`:@@admin.audit.detail-error:Impossible de charger le détail de cet événement.`);
      }
    });
  }

  closeDetail(): void {
    this.detail = null;
    this.detailError = null;
  }

  /** Indente le payload JSON quand il est parsable ; sinon le rend tel quel. */
  prettyPayload(payload: string | null): string {
    if (!payload) return '';
    try {
      return JSON.stringify(JSON.parse(payload), null, 2);
    } catch {
      return payload;
    }
  }

  // ---- Intégrité (lecture seule) ---------------------------------------------

  private rangeOf(events: AuditEvent[]): SeqRange | null {
    if (!events.length) return null;
    const seqs = events.map(e => e.sequenceNo);
    return { from: Math.min(...seqs), to: Math.max(...seqs) };
  }

  /** Amorce la fenêtre avec les bornes visibles, puis vérifie : un clic, zéro saisie. */
  verifyVisible(range: SeqRange): void {
    this.verifyForm.setValue({ fromSeq: range.from, toSeq: range.to });
    this.verify();
  }

  /** La fenêtre doit être ordonnée : le serveur rejette `toSeq < fromSeq`. */
  get verifyRangeOrdered(): boolean {
    const { fromSeq, toSeq } = this.verifyForm.getRawValue();
    return fromSeq !== null && toSeq !== null && toSeq >= fromSeq;
  }

  verify(): void {
    if (this.verifying) return;
    if (this.verifyForm.invalid || !this.verifyRangeOrdered) {
      this.verifyForm.markAllAsTouched();
      this.verifyError = $localize`:@@admin.audit.verify-range-invalid:Indiquez une fenêtre de séquences valide (début ≥ 1 et fin ≥ début).`;
      return;
    }
    const { fromSeq, toSeq } = this.verifyForm.getRawValue();
    this.verifying = true;
    this.verifyError = null;
    this.verifyResult = null;
    this.svc.verifyChain(fromSeq as number, toSeq as number).pipe(
      finalize(() => { this.verifying = false; })
    ).subscribe({
      next: result => { this.verifyResult = result; },
      error: err => {
        this.verifyError = safeErrorMessage(err,
          $localize`:@@admin.audit.verify-error:La vérification d'intégrité a échoué.`);
      }
    });
  }

  // ---- Présentation ----------------------------------------------------------

  actorTone(actorType: ActorType): 'neutral' | 'accent' {
    return actorType === 'USER' ? 'accent' : 'neutral';
  }

  /** Un hash SHA-256 complet noierait la cellule : on montre une amorce lisible. */
  shortHash(hash: string | null): string {
    if (!hash) return '—';
    return hash.length > 12 ? hash.slice(0, 12) + '…' : hash;
  }

  isAnchored(event: AuditEvent): boolean {
    return !!event.blockchainTxRef;
  }

  isSelected(event: AuditEvent): boolean {
    return this.detail?.id === event.id;
  }

  trackBySeq(_index: number, event: AuditEvent): number {
    return event.sequenceNo;
  }

  trackByBreak(index: number, brk: { sequenceNo: number }): string {
    return `${brk.sequenceNo}-${index}`;
  }
}
