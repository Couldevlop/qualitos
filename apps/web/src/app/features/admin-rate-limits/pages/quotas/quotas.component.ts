import { Component, OnInit } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { BehaviorSubject, Observable, Subject } from 'rxjs';
import { catchError, filter, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import {
  ConfirmDialogComponent, ConfirmDialogData
} from '../../../../shared/ui/confirm-dialog/confirm-dialog.component';
import { RateLimitsService } from '../../rate-limits.service';
import {
  RateLimitPolicy, RateLimitRow, RateLimitTotals, UsageLevel
} from '../../rate-limits.types';

/** Tuile de synthèse affichée en tête d'écran. */
interface SummaryTile {
  label: string;
  value: number;
  tone: 'neutral' | 'success' | 'warn' | 'danger';
}

/**
 * Périmètre : minuscule initiale, puis minuscules, chiffres et `. _ : -`, 100 max.
 * Copie exacte de la contrainte serveur (RateLimitPolicy.SCOPE_FORMAT) pour refuser
 * la saisie AVANT l'aller-retour HTTP plutôt qu'afficher un 400 opaque.
 */
const SCOPE_PATTERN = /^[a-z][a-z0-9._:-]{0,99}$/;

/** Bornes serveur (RateLimitPolicy) : fenêtre ≤ 24 h, ≤ 1 000 000 requêtes. */
const MAX_WINDOW_SECONDS = 86400;
const MAX_REQUESTS = 1000000;

/** Fenêtres proposées en un clic — les trois cadences réellement utilisées. */
const WINDOW_PRESETS = [60, 3600, 86400];

/**
 * Console d'administration — quotas et limites d'API du tenant (§13.1).
 *
 * Parti pris : l'écran ne montre pas seulement la LIMITE configurée mais la
 * CONSOMMATION courante face à elle (`GET /check/{scope}`, lecture sans incrément).
 * Un quota se règle en regardant ce qu'il coûte, pas dans l'abstrait — et un périmètre
 * qui frôle son plafond doit sauter aux yeux avant que les 429 n'arrivent chez le client.
 */
@Component({
  selector: 'qos-rate-limit-quotas',
  templateUrl: './quotas.component.html',
  styleUrls: ['./quotas.component.scss'],
  standalone: false
})
export class QuotasComponent implements OnInit {

  readonly displayedColumns = ['scope', 'window', 'usage', 'reset', 'state', 'actions'];
  readonly windowPresets = WINDOW_PRESETS;

  /** La jauge est un `progressbar` : sans nom accessible, un lecteur d'écran n'annonce rien. */
  readonly usageAria = $localize`:@@admin.rate-limits.usage-aria:Consommation du quota sur la fenêtre en cours`;

  rows$!: Observable<RateLimitRow[]>;
  tiles$!: Observable<SummaryTile[]>;

  /**
   * Horodatage de la mesure affichée — une jauge sans âge n'est pas lisible.
   * Dérivé du flux et non affecté depuis un `tap` : écrire un champ pendant que la
   * vue est déjà en cours de vérification déclencherait NG0100.
   */
  measuredAt$!: Observable<Date>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = this.errorState$.asObservable();

  /** Politique en cours d'édition ; `null` = le formulaire crée une nouvelle politique. */
  editing: RateLimitPolicy | null = null;

  /** Enregistrement en cours : verrouille le formulaire et évite le double envoi. */
  saving = false;

  /** Périmètre dont une action de ligne est en vol : évite le double-clic. */
  pendingScope: string | null = null;

  readonly form = this.fb.nonNullable.group({
    scope: ['', [Validators.required, Validators.maxLength(100), Validators.pattern(SCOPE_PATTERN)]],
    windowSeconds: [60, [Validators.required, Validators.min(1), Validators.max(MAX_WINDOW_SECONDS)]],
    maxRequests: [1000, [Validators.required, Validators.min(1), Validators.max(MAX_REQUESTS)]],
    enabled: [true]
  });

  private readonly reload$ = new Subject<void>();

  constructor(
    private readonly svc: RateLimitsService,
    private readonly fb: FormBuilder,
    private readonly dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const overview$ = this.reload$.pipe(
      startWith(undefined),
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(() => this.svc.overview().pipe(
        catchError(err => {
          this.errorState$.next(safeErrorMessage(err,
            $localize`:@@admin.rate-limits.load-error:Impossible de charger les quotas d'API.`));
          return [];
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      // refCount:false : les tuiles, le tableau et l'état vide s'abonnent séparément et
      // certains sont derrière un *ngIf — un abonnement tardif ne doit pas relancer la
      // liste ET toutes les sondes de consommation.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    this.rows$ = overview$.pipe(map(o => o.rows));
    this.tiles$ = overview$.pipe(map(o => this.toTiles(o.totals)));
    this.measuredAt$ = overview$.pipe(map(() => new Date()));
  }

  // ---- Actions ---------------------------------------------------------------

  refresh(): void {
    this.reload$.next();
  }

  /** Charge une politique dans le formulaire pour la modifier. */
  edit(row: RateLimitRow): void {
    const p = row.policy;
    this.editing = p;
    this.errorState$.next(null);
    this.form.reset({
      scope: p.scope,
      windowSeconds: p.windowSeconds,
      maxRequests: p.maxRequests,
      enabled: p.enabled
    });
    // Le périmètre IDENTIFIE la politique côté serveur : le modifier ne renommerait
    // pas la règle, il en créerait une seconde. On le verrouille pendant l'édition.
    this.form.controls.scope.disable();
  }

  cancelEdit(): void {
    this.editing = null;
    this.form.reset({ scope: '', windowSeconds: 60, maxRequests: 1000, enabled: true });
    this.form.controls.scope.enable();
  }

  submit(): void {
    if (this.form.invalid || this.saving) {
      this.form.markAllAsTouched();
      return;
    }
    // getRawValue : le périmètre est désactivé pendant l'édition, `value` l'omettrait.
    const raw = this.form.getRawValue();
    this.saving = true;
    this.errorState$.next(null);
    this.svc.savePolicy({
      scope: raw.scope,
      windowSeconds: raw.windowSeconds,
      maxRequests: raw.maxRequests,
      enabled: raw.enabled
    }).pipe(
      finalize(() => { this.saving = false; })
    ).subscribe({
      next: () => { this.cancelEdit(); this.reload$.next(); },
      error: err => this.errorState$.next(safeErrorMessage(err,
        $localize`:@@admin.rate-limits.save-error:L'enregistrement du quota a échoué.`))
    });
  }

  /** Suspend une limite appliquée (ou la remet en application). */
  setEnabled(row: RateLimitRow, enabled: boolean): void {
    const p = row.policy;
    this.run(p.scope, () => this.svc.savePolicy({
      scope: p.scope,
      windowSeconds: p.windowSeconds,
      maxRequests: p.maxRequests,
      enabled
    }));
  }

  remove(row: RateLimitRow): void {
    const data: ConfirmDialogData = {
      title: $localize`:@@admin.rate-limits.delete-title:Supprimer ce quota ?`,
      message: $localize`:@@admin.rate-limits.delete-message:Le périmètre ne sera plus limité du tout : les appels passeront sans plafond.`,
      confirmLabel: $localize`:@@admin.rate-limits.delete-confirm:Supprimer`,
      destructive: true
    };
    this.dialog.open<ConfirmDialogComponent, ConfirmDialogData, boolean>(
      ConfirmDialogComponent, { data, panelClass: 'qos-dialog-panel' }
    ).afterClosed().pipe(filter(ok => ok === true)).subscribe(() => {
      // La ligne éditée disparaît : on ne laisse pas un formulaire pointer un id supprimé.
      if (this.editing?.id === row.policy.id) this.cancelEdit();
      this.run(row.policy.scope, () => this.svc.delete(row.policy.id));
    });
  }

  /**
   * Retire les espaces autour du périmètre à la sortie du champ.
   *
   * Un périmètre se colle depuis une documentation ou un ticket, souvent avec un
   * espace en trop : le motif serveur le refuse. Autant le corriger que renvoyer
   * l'utilisateur à un « format invalide » qu'il ne verra pas.
   */
  trimScope(): void {
    const control = this.form.controls.scope;
    const trimmed = control.value.trim();
    if (trimmed !== control.value) control.setValue(trimmed);
  }

  /** Applique une fenêtre usuelle en un clic (60 s, 1 h, 24 h). */
  applyPreset(seconds: number): void {
    this.form.controls.windowSeconds.setValue(seconds);
    this.form.controls.windowSeconds.markAsDirty();
  }

  /**
   * Exécute une action de ligne puis recharge la vue.
   *
   * L'action est passée en FABRIQUE, pas en observable : une action rejetée par le
   * verrou ne doit même pas être construite (un appel refusé ne coûte rien).
   *
   * Rechargement complet volontaire : la consommation affichée est une mesure vivante,
   * recopier la réponse dans la ligne laisserait une jauge périmée à l'écran.
   */
  private run(scope: string, action: () => Observable<unknown>): void {
    if (this.pendingScope) return;
    this.pendingScope = scope;
    this.errorState$.next(null);
    action().pipe(finalize(() => { this.pendingScope = null; })).subscribe({
      next: () => this.reload$.next(),
      error: err => this.errorState$.next(safeErrorMessage(err,
        $localize`:@@admin.rate-limits.action-error:L'opération sur le quota a échoué.`))
    });
  }

  // ---- Présentation ----------------------------------------------------------

  /** Part consommée en pourcentage entier — sert la jauge et son libellé. */
  percent(row: RateLimitRow): number {
    return Math.round(row.ratio * 100);
  }

  /** Fenêtre lisible : 86400 s se lit « 24 h », pas « 86400 s ». */
  windowLabel(seconds: number): string {
    // Le zéro (compteur sur le point de repartir) doit se lire « 0 s », pas « 0 h ».
    if (seconds > 0 && seconds % 3600 === 0) {
      return `${seconds / 3600} ${$localize`:@@admin.rate-limits.unit-hour:h`}`;
    }
    if (seconds > 0 && seconds % 60 === 0) {
      return `${seconds / 60} ${$localize`:@@admin.rate-limits.unit-minute:min`}`;
    }
    return `${Math.max(0, seconds)} ${$localize`:@@admin.rate-limits.unit-second:s`}`;
  }

  /** Temps restant avant remise à zéro du compteur, dans la même unité lisible. */
  resetLabel(row: RateLimitRow): string | null {
    if (!row.usage || row.level === 'unmeasured') return null;
    return this.windowLabel(Math.max(0, row.usage.resetSeconds));
  }

  /** Libellé du niveau, affiché à côté de la jauge pour ne pas dépendre de la couleur. */
  levelLabel(level: UsageLevel): string {
    switch (level) {
      case 'exceeded': return $localize`:@@admin.rate-limits.level-exceeded:Limite atteinte`;
      case 'critical': return $localize`:@@admin.rate-limits.level-critical:Proche de la limite`;
      case 'warning': return $localize`:@@admin.rate-limits.level-warning:Soutenu`;
      case 'normal': return $localize`:@@admin.rate-limits.level-normal:Normal`;
      case 'idle': return $localize`:@@admin.rate-limits.level-idle:Inactif`;
      default: return $localize`:@@admin.rate-limits.level-unmeasured:Non appliqué`;
    }
  }

  levelTone(level: UsageLevel): 'neutral' | 'success' | 'warn' | 'danger' {
    switch (level) {
      case 'exceeded': return 'danger';
      case 'critical': return 'danger';
      case 'warning': return 'warn';
      case 'normal': return 'success';
      case 'idle': return 'success';
      default: return 'neutral';
    }
  }

  trackByPolicy(_index: number, row: RateLimitRow): string {
    return row.policy.id;
  }

  private toTiles(totals: RateLimitTotals): SummaryTile[] {
    return [
      {
        label: $localize`:@@admin.rate-limits.tile-policies:Quotas définis`,
        value: totals.policies, tone: 'neutral'
      },
      {
        label: $localize`:@@admin.rate-limits.tile-enforced:Appliqués`,
        value: totals.enforced, tone: 'success'
      },
      {
        label: $localize`:@@admin.rate-limits.tile-near:Proches de la limite`,
        value: totals.nearLimit, tone: 'warn'
      },
      {
        label: $localize`:@@admin.rate-limits.tile-exceeded:Limite atteinte`,
        value: totals.exceeded, tone: 'danger'
      }
    ];
  }
}
