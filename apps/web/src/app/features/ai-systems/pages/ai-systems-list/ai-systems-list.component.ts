import { Component, OnInit } from '@angular/core';
import { FormControl } from '@angular/forms';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { BehaviorSubject, Observable, combineLatest, of } from 'rxjs';
import { catchError, finalize, map, shareReplay, startWith, switchMap, tap } from 'rxjs/operators';

import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import {
  Tone, riskBasis, riskLabel, riskTone, roleLabel, statusLabel, statusTone
} from '../../ai-systems.labels';
import { isProhibited, isStuckBeforeUse } from '../../ai-systems.rules';
import { AiSystemsService } from '../../ai-systems.service';
import {
  AiRiskClassification, AiSystemRegistry, AiSystemRole, AiSystemStatus, AiSystemView, RISK_SEVERITY
} from '../../ai-systems.types';
import { AiSystemFormDialogComponent } from '../ai-system-form-dialog/ai-system-form-dialog.component';

/** Raccourci de tête d'écran : une lecture du registre ET un filtre applicable. */
export type TileKey = 'all' | 'prohibited' | 'high' | 'draft' | 'in-use';

export interface RegistryTile {
  key: TileKey;
  label: string;
  hint: string;
  value: number;
  tone: Tone;
  active: boolean;
}

/**
 * Registre des systèmes d'IA — AI Act (règlement UE 2024/1689).
 *
 * Socle du module : les écrans QMS, conformité, incidents, base UE, FRIA et suivi
 * post-marché référencent tous un système d'IA, sans qu'aucun ne permette de le
 * créer. La classification de risque est l'information centrale — elle conditionne
 * les obligations —, d'où le tri par sévérité et les compteurs de tête.
 */
@Component({
  selector: 'qos-ai-systems-list',
  templateUrl: './ai-systems-list.component.html',
  styleUrls: ['./ai-systems-list.component.scss'],
  standalone: false
})
export class AiSystemsListComponent implements OnInit {

  readonly displayedColumns = ['reference', 'risk', 'role', 'status', 'provider', 'updated'];

  readonly statuses: AiSystemStatus[] =
    ['DRAFT', 'REGISTERED', 'IN_USE', 'DECOMMISSIONED', 'WITHDRAWN'];
  readonly risks: AiRiskClassification[] = RISK_SEVERITY;

  readonly statusFilter = new FormControl<AiSystemStatus | ''>('', { nonNullable: true });
  readonly riskFilter = new FormControl<AiRiskClassification | ''>('', { nonNullable: true });
  readonly search = new FormControl<string>('', { nonNullable: true });

  rows$!: Observable<AiSystemView[]>;
  tiles$!: Observable<RegistryTile[]>;
  /** Registre vide (aucun système du tout) : l'état vide n'est pas le même qu'un filtre trop étroit. */
  empty$!: Observable<boolean>;

  private readonly loadingState$ = new BehaviorSubject<boolean>(false);
  readonly loading$ = deferredView(this.loadingState$);
  private readonly errorState$ = new BehaviorSubject<string | null>(null);
  readonly error$ = deferredView(this.errorState$);

  private readonly reload$ = new BehaviorSubject<void>(undefined);

  constructor(
    private readonly svc: AiSystemsService,
    private readonly dialog: MatDialog,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    const registry$ = combineLatest([
      current(this.statusFilter),
      current(this.riskFilter),
      this.reload$
    ]).pipe(
      tap(() => { this.errorState$.next(null); this.loadingState$.next(true); }),
      switchMap(([status, risk]) => this.svc.registry({
        status: status || null,
        risk: risk || null
      }).pipe(
        catchError(err => {
          this.errorState$.next(safeErrorMessage(err,
            $localize`:@@ai-systems.list.load-error:Impossible de charger le registre des systèmes d'IA.`));
          return of<AiSystemRegistry>({ all: [], rows: [] });
        }),
        finalize(() => this.loadingState$.next(false))
      )),
      // refCount:false : le tableau, les compteurs et l'état vide s'abonnent
      // séparément, certains derrière un *ngIf — un abonnement tardif ne doit pas
      // relancer une requête.
      shareReplay({ bufferSize: 1, refCount: false })
    );

    // La recherche textuelle reste locale : le serveur n'expose pas de route de
    // recherche plein texte, et le registre d'un tenant tient en mémoire.
    this.rows$ = combineLatest([registry$, current(this.search)])
      .pipe(map(([registry, term]) => matching(registry.rows, term)));

    this.tiles$ = registry$.pipe(map(registry => this.toTiles(registry.all)));
    this.empty$ = registry$.pipe(map(registry => registry.all.length === 0));
  }

  // ---- Actions ---------------------------------------------------------------

  create(): void {
    this.dialog.open<AiSystemFormDialogComponent, { mode: 'create' }, AiSystemView | undefined>(
      AiSystemFormDialogComponent,
      {
        data: { mode: 'create' }, panelClass: 'qos-dialog-panel',
        autoFocus: 'first-tabbable', restoreFocus: true
      }
    ).afterClosed().subscribe(created => {
      if (created) this.router.navigate(['/ai-systems', created.id]);
    });
  }

  refresh(): void {
    this.reload$.next();
  }

  /** Un compteur cliqué devient un filtre : la lecture mène directement à l'action. */
  applyTile(key: TileKey): void {
    switch (key) {
      case 'prohibited':
        this.statusFilter.setValue('');
        this.riskFilter.setValue('UNACCEPTABLE');
        break;
      case 'high':
        this.statusFilter.setValue('');
        this.riskFilter.setValue('HIGH');
        break;
      case 'draft':
        this.riskFilter.setValue('');
        this.statusFilter.setValue('DRAFT');
        break;
      case 'in-use':
        this.riskFilter.setValue('');
        this.statusFilter.setValue('IN_USE');
        break;
      default:
        this.resetFilters();
    }
  }

  resetFilters(): void {
    this.statusFilter.setValue('');
    this.riskFilter.setValue('');
    this.search.setValue('');
  }

  hasFilters(): boolean {
    return !!this.statusFilter.value || !!this.riskFilter.value || !!this.search.value.trim();
  }

  // ---- Présentation ----------------------------------------------------------

  riskLabel(risk: AiRiskClassification): string { return riskLabel(risk); }
  riskBasis(risk: AiRiskClassification): string { return riskBasis(risk); }
  riskTone(risk: AiRiskClassification): Tone { return riskTone(risk); }
  statusLabel(status: AiSystemStatus): string { return statusLabel(status); }
  statusTone(status: AiSystemStatus): Tone { return statusTone(status); }
  roleLabel(role: AiSystemRole): string { return roleLabel(role); }

  /** Signale les fiches qui exigent une décision : interdites ou bloquées avant service. */
  needsAttention(system: AiSystemView): boolean {
    return isProhibited(system) || isStuckBeforeUse(system);
  }

  trackById(_index: number, system: AiSystemView): string {
    return system.id;
  }

  private toTiles(all: AiSystemView[]): RegistryTile[] {
    const count = (predicate: (s: AiSystemView) => boolean) => all.filter(predicate).length;
    return [
      {
        key: 'all',
        label: $localize`:@@ai-systems.tile.all:Systèmes recensés`,
        hint: $localize`:@@ai-systems.tile.all-hint:Tout le registre du tenant`,
        value: all.length, tone: 'neutral',
        active: !this.statusFilter.value && !this.riskFilter.value
      },
      {
        key: 'prohibited',
        label: $localize`:@@ai-systems.tile.prohibited:Risque inacceptable`,
        hint: $localize`:@@ai-systems.tile.prohibited-hint:Interdits de mise sur le marché (Art. 5)`,
        value: count(s => s.riskClassification === 'UNACCEPTABLE'), tone: 'danger',
        active: this.riskFilter.value === 'UNACCEPTABLE'
      },
      {
        key: 'high',
        label: $localize`:@@ai-systems.tile.high:Haut risque`,
        hint: $localize`:@@ai-systems.tile.high-hint:Obligations renforcées (Annexe III)`,
        value: count(s => s.riskClassification === 'HIGH'), tone: 'warn',
        active: this.riskFilter.value === 'HIGH'
      },
      {
        key: 'draft',
        label: $localize`:@@ai-systems.tile.draft:Brouillons`,
        hint: $localize`:@@ai-systems.tile.draft-hint:Fiches encore modifiables`,
        value: count(s => s.status === 'DRAFT'), tone: 'info',
        active: this.statusFilter.value === 'DRAFT'
      },
      {
        key: 'in-use',
        label: $localize`:@@ai-systems.tile.in-use:En service`,
        hint: $localize`:@@ai-systems.tile.in-use-hint:Systèmes réellement exploités`,
        value: count(s => s.status === 'IN_USE'), tone: 'success',
        active: this.statusFilter.value === 'IN_USE'
      }
    ];
  }
}

/**
 * Valeur courante d'un filtre, à l'abonnement puis à chaque changement.
 *
 * POURQUOI ne pas écrire `startWith(control.value)` : l'argument est figé à la
 * construction du flux. Un abonné tardif — une section du gabarit derrière un
 * `*ngIf` — repartirait alors de la valeur initiale et afficherait un résultat
 * qui ne correspond plus au filtre visible à l'écran.
 */
function current<T>(control: FormControl<T>): Observable<T> {
  return control.valueChanges.pipe(startWith(null), map(() => control.value));
}

/** Recherche sur les seuls champs qu'un utilisateur retient : référence, nom, fournisseur, finalité. */
function matching(rows: AiSystemView[], term: string): AiSystemView[] {
  const needle = (term ?? '').trim().toLowerCase();
  if (!needle) return rows;
  return rows.filter(s =>
    s.reference.toLowerCase().includes(needle)
    || s.name.toLowerCase().includes(needle)
    || (s.providerName ?? '').toLowerCase().includes(needle)
    || s.intendedPurpose.toLowerCase().includes(needle));
}
