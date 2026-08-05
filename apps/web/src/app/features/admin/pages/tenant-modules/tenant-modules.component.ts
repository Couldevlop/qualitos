import { Component, OnInit } from '@angular/core';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import {
  ActivationStatus, BillingTier, ModuleRow, TenantModuleSummary
} from '../../admin.types';
import { TenantModulesService } from '../../tenant-modules.service';

/** Ligne de synthèse affichée en tête d'écran. */
interface SummaryTile {
  label: string;
  value: number;
  tone: 'neutral' | 'success' | 'warn' | 'danger';
}

/**
 * Console d'administration — activation des modules du tenant (§10.4).
 *
 * L'API existait sans écran : la promesse « le tenant active uniquement les modules dont
 * il a besoin, désactivation à chaud » n'était pilotable qu'en appelant l'API à la main.
 *
 * Parti pris d'affichage : la liste part du CATALOGUE, pas des activations. Un module
 * jamais activé doit apparaître — sans quoi il serait impossible de l'activer depuis
 * l'interface. Les modules cœur sont montrés mais verrouillés.
 */
@Component({
  selector: 'qos-tenant-modules',
  templateUrl: './tenant-modules.component.html',
  styleUrls: ['./tenant-modules.component.scss'],
  standalone: false
})
export class TenantModulesComponent implements OnInit {

  readonly displayedColumns = ['module', 'category', 'status', 'tier', 'validity', 'actions'];

  rows$!: Observable<ModuleRow[]>;
  summary$!: Observable<TenantModuleSummary | null>;
  tiles$!: Observable<SummaryTile[]>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = this.errorState$.asObservable();

  /** Action en cours sur une ligne : évite le double-clic et signale l'attente. */
  pendingModuleCode: string | null = null;

  private readonly reload$ = new Subject<void>();

  /**
   * Le périmètre facturé d'un tenant se décide chez l'éditeur, pas chez le client :
   * seul le super administrateur ouvre ou ferme un module. L'administrateur du
   * tenant CONSULTE — c'est ainsi qu'il sait de quoi il dispose et ce qu'il peut
   * demander. Afficher des boutons que le serveur refusera en 403 serait promettre
   * une action qui n'existe pas ; l'autorisation reste appliquée par le serveur,
   * cette observable ne fait qu'accorder l'écran à la règle.
   */
  canManage$!: Observable<boolean>;

  constructor(
    private readonly svc: TenantModulesService,
    private readonly auth: AuthService
  ) {}

  ngOnInit(): void {
    this.canManage$ = this.auth.user().pipe(
      map(user => (user?.roles ?? []).includes('super_admin'))
    );
    const overview$ = this.reload$.pipe(
      startWith(undefined),
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.svc.overview().pipe(
        catchError(err => {
          this.errorState$.next(safeErrorMessage(err,
            $localize`:@@admin.modules.load-error:Impossible de charger les modules.`));
          return [];
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      // refCount:false : plusieurs sections de la page s'abonnent (tuiles, tableau) et
      // certaines sont derrière un *ngIf — un abonnement tardif ne doit pas relancer
      // deux requêtes.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.rows$ = overview$.pipe(map(o => o.rows));
    this.summary$ = overview$.pipe(map(o => o.summary));
    this.tiles$ = overview$.pipe(map(o => this.toTiles(o.summary)));
  }

  // ---- Actions ---------------------------------------------------------------

  activate(row: ModuleRow): void {
    this.run(row, this.svc.activate(row.entry.code));
  }

  /** Essai de 30 jours : durée standard, l'ajustement fin passe par la conversion. */
  startTrial(row: ModuleRow): void {
    const endsAt = new Date();
    endsAt.setDate(endsAt.getDate() + 30);
    this.run(row, this.svc.startTrial(row.entry.code, endsAt.toISOString()));
  }

  convertTrial(row: ModuleRow): void {
    if (!row.activation) return;
    this.run(row, this.svc.convertTrial(row.activation.id));
  }

  suspend(row: ModuleRow): void {
    if (!row.activation) return;
    this.run(row, this.svc.suspend(row.activation.id));
  }

  resume(row: ModuleRow): void {
    if (!row.activation) return;
    this.run(row, this.svc.resume(row.activation.id));
  }

  disable(row: ModuleRow): void {
    if (!row.activation) return;
    this.run(row, this.svc.disable(row.activation.id));
  }

  changeTier(row: ModuleRow, tier: BillingTier): void {
    if (!row.activation) return;
    this.run(row, this.svc.changeTier(row.activation.id, tier));
  }

  /**
   * Exécute une action de cycle de vie puis recharge la vue.
   *
   * Le rechargement complet est volontaire : activer un module peut en entraîner
   * d'autres par dépendance, et le serveur est seul juge de l'état résultant. Recopier
   * la réponse dans la ligne donnerait une vue partiellement fausse.
   */
  private run(row: ModuleRow, action$: Observable<unknown>): void {
    if (this.pendingModuleCode) return;
    this.pendingModuleCode = row.entry.code;
    this.errorState$.next(null);
    action$.pipe(finalize(() => { this.pendingModuleCode = null; })).subscribe({
      next: () => this.reload$.next(),
      error: err => this.errorState$.next(safeErrorMessage(err,
        $localize`:@@admin.modules.action-error:L'opération sur le module a échoué.`))
    });
  }

  // ---- Présentation ----------------------------------------------------------

  /** Un module cœur ne se désactive pas : la plateforme en dépend. */
  isLocked(row: ModuleRow): boolean {
    return row.entry.coreModule;
  }

  /** Le module est-il ouvert (utilisable) pour le tenant ? */
  isOpen(row: ModuleRow): boolean {
    return !!row.activation?.enabled;
  }

  status(row: ModuleRow): ActivationStatus | null {
    return row.activation?.status ?? null;
  }

  statusTone(status: ActivationStatus | null): 'neutral' | 'success' | 'warn' | 'danger' {
    switch (status) {
      case 'ACTIVE': return 'success';
      case 'TRIAL': return 'warn';
      case 'SUSPENDED': return 'warn';
      case 'EXPIRED': return 'danger';
      case 'DISABLED': return 'neutral';
      default: return 'neutral';
    }
  }

  /** Échéance pertinente selon l'état : fin d'essai, sinon fin d'abonnement. */
  validityDate(row: ModuleRow): string | null {
    if (!row.activation) return null;
    return row.activation.status === 'TRIAL'
      ? row.activation.trialEndsAt
      : row.activation.expiresAt;
  }

  /**
   * Le palier du tenant est-il suffisant pour ce module ? Sert à expliquer pourquoi une
   * activation serait refusée, plutôt que de laisser l'utilisateur essuyer une erreur.
   */
  tierSufficient(row: ModuleRow, tenantTier: BillingTier | undefined): boolean {
    if (!tenantTier) return true;
    return TIER_ORDER.indexOf(tenantTier) >= TIER_ORDER.indexOf(row.entry.minimumTier);
  }

  trackByCode(_index: number, row: ModuleRow): string {
    return row.entry.code;
  }

  private toTiles(summary: TenantModuleSummary): SummaryTile[] {
    return [
      { label: $localize`:@@admin.modules.tile-enabled:Modules actifs`, value: summary.enabledCount, tone: 'success' },
      { label: $localize`:@@admin.modules.tile-trial:En essai`, value: summary.trialCount, tone: 'warn' },
      { label: $localize`:@@admin.modules.tile-suspended:Suspendus`, value: summary.suspendedCount, tone: 'warn' },
      { label: $localize`:@@admin.modules.tile-expired:Expirés`, value: summary.expiredCount, tone: 'danger' },
      { label: $localize`:@@admin.modules.tile-total:Total activations`, value: summary.totalActivations, tone: 'neutral' }
    ];
  }
}

/** Ordre croissant des paliers : sert à comparer le palier tenant au minimum requis. */
const TIER_ORDER: BillingTier[] = ['FREE', 'STANDARD', 'PRO', 'ENTERPRISE'];
