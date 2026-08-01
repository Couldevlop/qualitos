import { Component, OnInit } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { deferredView } from '../../../../core/rx/deferred-view';
import { StatusTone } from '../../../../shared/ui/status-pill/status-pill.component';
import {
  ANCHOR_BATCH_MAX, ANCHOR_BATCH_MIN, StandardsExtrasService
} from '../../standards-extras.service';
import {
  AnchorableEvent, AnchorBatchResult, AnchorStatus, AnchorVerification, CoverageCellView,
  CoverageOverview, CoverageRelation, CoverageRow, MockAuditCriticality, MockAuditReport,
  StandardAdoption, StandardCatalogEntry
} from '../../standards-extras.types';

/** Un hash d'intégrité est un SHA-256 hexadécimal : 64 caractères, rien d'autre. */
const INTEGRITY_HASH = /^[0-9a-fA-F]{64}$/;

/** Rôles habilités à lancer un audit blanc IA (cf. `@PreAuthorize` du contrôleur). */
const AUDIT_ROLES = [
  'quality_manager', 'director_quality', 'auditor', 'admin', 'admin_tenant', 'super_admin'
];

/** Rôles habilités à déclencher un lot d'ancrage (cf. `SecurityConfig`). */
const ANCHOR_ROLES = ['quality_manager', 'admin', 'admin_tenant', 'super_admin'];

/** Compteur de tête d'onglet. */
export interface HubTile {
  label: string;
  value: string;
  hint: string;
  tone: 'neutral' | 'success' | 'warn' | 'danger';
}

/**
 * Standards Hub — les trois capacités livrées sans interface, réunies.
 *
 * 1. Matrice de co-couverture IMS (§8.9) : quelles clauses sont couvertes par
 *    PLUSIEURS normes à la fois. C'est l'argument d'économie d'effort d'un
 *    système de management intégré — d'où la mise en avant du nombre de clauses
 *    mutualisées et du filtre dédié, plutôt qu'un simple déversement de liens.
 * 2. Audit blanc IA (§8.4 onglet 7) : la simulation d'audit PAR LLM, persistée et
 *    historisée. Elle ne remplace pas l'audit blanc « règles » de l'écran
 *    Standards Hub, qui reste un calcul instantané non conservé : les deux sont
 *    explicitement distingués dans l'interface pour ne pas passer pour un doublon.
 * 3. Ancrage blockchain (§11.3) : déclenchement d'un lot et vérification de
 *    l'intégrité d'une preuve.
 *
 * Le tenant vient du JWT côté serveur : aucun identifiant de tenant ne circule ici.
 */
@Component({
  selector: 'qos-ims-hub',
  templateUrl: './ims-hub.component.html',
  styleUrls: ['./ims-hub.component.scss'],
  standalone: false
})
export class ImsHubComponent implements OnInit {

  // ---- Onglet 1 : matrice de co-couverture ----------------------------------

  readonly codesControl = new FormControl<string[]>([], { nonNullable: true });
  standards: StandardCatalogEntry[] = [];
  coverage: CoverageOverview | null = null;
  visibleRows: CoverageRow[] = [];
  tiles: HubTile[] = [];
  /** Filtre §8.9 : ne garder que les clauses réellement mutualisables. */
  sharedOnly = false;

  private readonly coverageLoadingState$ = new BehaviorSubject<boolean>(false);
  readonly coverageLoading$ = deferredView(this.coverageLoadingState$);
  private readonly coverageErrorState$ = new BehaviorSubject<string | null>(null);
  readonly coverageError$ = this.coverageErrorState$.asObservable();

  /** Sélection déjà chargée : évite de relancer la requête à chaque fermeture du menu. */
  private loadedCodesKey: string | null = null;

  // ---- Onglet 2 : audit blanc IA --------------------------------------------

  readonly adoptionControl = new FormControl<string>('', { nonNullable: true });
  adoptions: StandardAdoption[] = [];
  history: MockAuditReport[] = [];
  running = false;

  private readonly auditLoadingState$ = new BehaviorSubject<boolean>(false);
  readonly auditLoading$ = deferredView(this.auditLoadingState$);
  private readonly auditErrorState$ = new BehaviorSubject<string | null>(null);
  readonly auditError$ = this.auditErrorState$.asObservable();

  canRunAudit = true;

  // ---- Onglet 3 : ancrage blockchain ----------------------------------------

  readonly batchMin = ANCHOR_BATCH_MIN;
  readonly batchMax = ANCHOR_BATCH_MAX;

  readonly batchControl = new FormControl<number>(100, {
    nonNullable: true,
    validators: [Validators.required, Validators.min(ANCHOR_BATCH_MIN), Validators.max(ANCHOR_BATCH_MAX)]
  });
  readonly hashControl = new FormControl<string>('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(INTEGRITY_HASH)]
  });

  batchResult: AnchorBatchResult | null = null;
  verification: AnchorVerification | null = null;
  events: AnchorableEvent[] = [];
  anchoring = false;
  verifying = false;

  private readonly eventsLoadingState$ = new BehaviorSubject<boolean>(false);
  readonly eventsLoading$ = deferredView(this.eventsLoadingState$);
  private readonly anchorErrorState$ = new BehaviorSubject<string | null>(null);
  readonly anchorError$ = this.anchorErrorState$.asObservable();

  canAnchor = true;

  constructor(
    private readonly svc: StandardsExtrasService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    this.computeRoles();
    this.loadStandards();
    this.loadCoverage();
    this.loadAdoptions();
    this.loadEvents();
  }

  /**
   * Sans rôle exploitable (mode dev, claims absents) on n'ampute pas l'écran : le
   * serveur reste seul juge et renverra 403 si besoin. C'est la convention des
   * autres écrans de la plateforme.
   */
  private computeRoles(): void {
    const roles = this.auth.snapshot()?.roles ?? [];
    this.canRunAudit = roles.length === 0 || roles.some(r => AUDIT_ROLES.includes(r));
    this.canAnchor = roles.length === 0 || roles.some(r => ANCHOR_ROLES.includes(r));
  }

  // ===========================================================================
  // Onglet 1 — matrice de co-couverture
  // ===========================================================================

  private loadStandards(): void {
    this.svc.selectableStandards().subscribe({
      next: list => (this.standards = list),
      // Le sélecteur n'est qu'un confort : sans catalogue, la matrice reste
      // utilisable sur les normes adoptées (comportement serveur par défaut).
      error: () => (this.standards = [])
    });
  }

  loadCoverage(): void {
    const codes = this.codesControl.value;
    this.loadedCodesKey = ImsHubComponent.codesKey(codes);
    this.coverageErrorState$.next(null);
    this.coverageLoadingState$.next(true);
    this.svc.coverageOverview(codes)
      .pipe(finalize(() => this.coverageLoadingState$.next(false)))
      .subscribe({
        next: overview => {
          this.coverage = overview;
          this.tiles = ImsHubComponent.buildTiles(overview);
          this.applyCoverageFilter();
        },
        error: err => {
          this.coverage = null;
          this.visibleRows = [];
          this.tiles = [];
          this.coverageErrorState$.next(safeErrorMessage(err,
            $localize`:@@standards-extras.coverage.error:Impossible de charger la matrice de co-couverture.`));
        }
      });
  }

  /** Le menu multi-sélection ne déclenche une requête que si la sélection a changé. */
  onStandardsClosed(): void {
    if (ImsHubComponent.codesKey(this.codesControl.value) === this.loadedCodesKey) {
      return;
    }
    this.loadCoverage();
  }

  clearStandards(): void {
    this.codesControl.setValue([]);
    this.loadCoverage();
  }

  toggleSharedOnly(checked: boolean): void {
    this.sharedOnly = checked;
    this.applyCoverageFilter();
  }

  private applyCoverageFilter(): void {
    const rows = this.coverage?.rows ?? [];
    this.visibleRows = this.sharedOnly ? rows.filter(r => r.sharedCount > 0) : rows;
  }

  /** Une seule norme comparée : il ne peut y avoir aucune mutualisation à montrer. */
  get singleStandard(): boolean {
    return (this.coverage?.columns.length ?? 0) <= 1;
  }

  relationLabel(relation: CoverageRelation): string {
    switch (relation) {
      case 'EQUIVALENT': return $localize`:@@standards-extras.relation.equivalent:Équivalente`;
      case 'COVERS': return $localize`:@@standards-extras.relation.covers:Couvre`;
      case 'RELATED': return $localize`:@@standards-extras.relation.related:Liée`;
      default: return $localize`:@@standards-extras.relation.references:Cite`;
    }
  }

  relationTone(relation: CoverageRelation): StatusTone {
    switch (relation) {
      case 'EQUIVALENT': return 'success';
      case 'COVERS': return 'accent';
      case 'RELATED': return 'warn';
      default: return 'neutral';
    }
  }

  /** Infobulle d'une couverture : lecture complète sans surcharger la cellule. */
  coverageTooltip(standardCode: string, clauseCode: string, relation: CoverageRelation,
                  confidence: number): string {
    return `${standardCode} ${clauseCode} — ${this.relationLabel(relation)} (${confidence} %)`;
  }

  /** Libellé du compteur de mutualisation d'une ligne. */
  sharedLabel(row: CoverageRow): string {
    return $localize`:@@standards-extras.coverage.shared-count:${row.sharedCount}:count: norme(s)`;
  }

  trackByRow(_index: number, row: CoverageRow): string {
    return `${row.sourceStandardCode} ${row.sourceClauseCode}`;
  }

  trackByCell(_index: number, cell: CoverageCellView): string {
    return cell.targetStandardCode;
  }

  trackByCode(_index: number, code: string): string {
    return code;
  }

  trackByStandard(_index: number, entry: StandardCatalogEntry): string {
    return entry.id;
  }

  private static codesKey(codes: string[]): string {
    return [...codes].sort().join(',');
  }

  // ===========================================================================
  // Onglet 2 — audit blanc IA
  // ===========================================================================

  private loadAdoptions(): void {
    this.auditLoadingState$.next(true);
    this.svc.adoptions()
      .pipe(finalize(() => this.auditLoadingState$.next(false)))
      .subscribe({
        next: list => {
          this.adoptions = list;
          const first = list[0];
          if (first && !this.adoptionControl.value) {
            this.adoptionControl.setValue(first.id);
            this.loadHistory();
          }
        },
        error: err => this.auditErrorState$.next(safeErrorMessage(err,
          $localize`:@@standards-extras.audit.error-adoptions:Impossible de charger les normes adoptées.`))
      });
  }

  loadHistory(): void {
    const adoptionId = this.adoptionControl.value;
    if (!adoptionId) {
      this.history = [];
      return;
    }
    this.auditErrorState$.next(null);
    this.auditLoadingState$.next(true);
    this.svc.mockAuditHistory(adoptionId)
      .pipe(finalize(() => this.auditLoadingState$.next(false)))
      .subscribe({
        next: runs => (this.history = runs),
        error: err => {
          this.history = [];
          this.auditErrorState$.next(safeErrorMessage(err,
            $localize`:@@standards-extras.audit.error-history:Impossible de charger l'historique des audits blancs IA.`));
        }
      });
  }

  /** Lance l'audit, puis ouvre directement le rapport produit : c'est ce qu'on vient chercher. */
  runAudit(): void {
    const adoptionId = this.adoptionControl.value;
    if (!adoptionId || this.running) {
      return;
    }
    this.running = true;
    this.auditErrorState$.next(null);
    this.svc.runMockAudit(adoptionId)
      .pipe(finalize(() => (this.running = false)))
      .subscribe({
        next: report => this.openReport(report),
        error: err => this.auditErrorState$.next(this.runErrorMessage(err))
      });
  }

  openReport(report: MockAuditReport): void {
    this.router.navigate(['audit-blanc-ia', report.adoptionId, report.id], { relativeTo: this.route });
  }

  /** Adoption courante : sert à proposer le lien vers l'audit blanc « règles ». */
  get selectedAdoption(): StandardAdoption | null {
    const id = this.adoptionControl.value;
    return this.adoptions.find(a => a.id === id) ?? null;
  }

  criticalityLabel(criticality: MockAuditCriticality): string {
    switch (criticality) {
      case 'MAJOR': return $localize`:@@standards-extras.criticality.major:NC majeure`;
      case 'MINOR': return $localize`:@@standards-extras.criticality.minor:NC mineure`;
      default: return $localize`:@@standards-extras.criticality.observation:Observation`;
    }
  }

  /** Le pipe `number` peut renvoyer `null` : on formate en TS pour garder un libellé sûr. */
  readinessLabel(readiness: number): string {
    return `${Math.round(readiness)} %`;
  }

  readinessTone(readiness: number): StatusTone {
    if (readiness >= 80) return 'success';
    if (readiness >= 50) return 'warn';
    return 'danger';
  }

  trackByReport(_index: number, report: MockAuditReport): string {
    return report.id;
  }

  trackByAdoption(_index: number, adoption: StandardAdoption): string {
    return adoption.id;
  }

  private runErrorMessage(err: unknown): string {
    const status = (err as { status?: number } | null)?.status;
    if (status === 409) {
      return $localize`:@@standards-extras.audit.error-no-clause:Cette norme adoptée n'expose aucune clause exploitable : chargez son référentiel avant de lancer un audit blanc IA.`;
    }
    return safeErrorMessage(err,
      $localize`:@@standards-extras.audit.error-run:L'audit blanc IA n'a pas pu être exécuté.`);
  }

  // ===========================================================================
  // Onglet 3 — ancrage blockchain
  // ===========================================================================

  loadEvents(): void {
    this.eventsLoadingState$.next(true);
    this.svc.recentAuditEvents(20)
      .pipe(finalize(() => this.eventsLoadingState$.next(false)))
      .subscribe({
        next: list => (this.events = list),
        error: err => {
          this.events = [];
          this.anchorErrorState$.next(safeErrorMessage(err,
            $localize`:@@standards-extras.anchor.error-events:Impossible de charger les derniers événements d'audit.`));
        }
      });
  }

  runAnchoring(): void {
    if (this.batchControl.invalid || this.anchoring) {
      this.batchControl.markAsTouched();
      return;
    }
    this.anchoring = true;
    this.anchorErrorState$.next(null);
    this.svc.anchorBatch(this.batchControl.value)
      .pipe(finalize(() => (this.anchoring = false)))
      .subscribe({
        next: result => {
          this.batchResult = result;
          // Les txRef des événements viennent de changer : la liste doit suivre.
          this.loadEvents();
        },
        error: err => this.anchorErrorState$.next(safeErrorMessage(err,
          $localize`:@@standards-extras.anchor.error-run:Le lot d'ancrage n'a pas pu être déclenché.`))
      });
  }

  verifyHash(): void {
    if (this.hashControl.invalid || this.verifying) {
      this.hashControl.markAsTouched();
      return;
    }
    this.verifying = true;
    this.anchorErrorState$.next(null);
    this.svc.verifyAnchor(this.hashControl.value)
      .pipe(finalize(() => (this.verifying = false)))
      .subscribe({
        next: result => (this.verification = result),
        error: err => {
          this.verification = null;
          this.anchorErrorState$.next(safeErrorMessage(err,
            $localize`:@@standards-extras.anchor.error-verify:La vérification d'intégrité a échoué.`));
        }
      });
  }

  /** Reprend le hash d'un événement listé puis lance sa vérification. */
  verifyEvent(event: AnchorableEvent): void {
    this.hashControl.setValue(event.integrityHash);
    this.verifyHash();
  }

  anchorStatusLabel(status: AnchorStatus): string {
    switch (status) {
      case 'VERIFIED': return $localize`:@@standards-extras.anchor.status-verified:Intégrité confirmée`;
      case 'TAMPERED': return $localize`:@@standards-extras.anchor.status-tampered:Altération détectée`;
      default: return $localize`:@@standards-extras.anchor.status-not-anchored:Pas encore ancré`;
    }
  }

  anchorStatusTone(status: AnchorStatus): StatusTone {
    switch (status) {
      case 'VERIFIED': return 'success';
      case 'TAMPERED': return 'danger';
      default: return 'neutral';
    }
  }

  trackByEvent(_index: number, event: AnchorableEvent): string {
    return event.id;
  }

  // ===========================================================================
  // Compteurs de tête
  // ===========================================================================

  /** Statique et pure : les compteurs sont recalculés au chargement, pas à chaque cycle de rendu. */
  private static buildTiles(c: CoverageOverview): HubTile[] {
    return [
      {
        label: $localize`:@@standards-extras.tile.standards:Normes comparées`,
        value: String(c.columns.length),
        hint: c.columns.join(' · '),
        tone: 'neutral'
      },
      {
        label: $localize`:@@standards-extras.tile.clauses:Clauses sources reliées`,
        value: String(c.matrix.totalSourceClauses),
        hint: $localize`:@@standards-extras.tile.clauses-hint:Clauses possédant au moins un lien vers une autre norme du périmètre.`,
        tone: 'neutral'
      },
      {
        label: $localize`:@@standards-extras.tile.shared:Clauses mutualisables`,
        value: String(c.sharedClauseCount),
        hint: $localize`:@@standards-extras.tile.shared-hint:Clauses dont la preuve vaut pour au moins une autre norme (lien « Équivalente » ou « Couvre »).`,
        tone: c.sharedClauseCount > 0 ? 'success' : 'neutral'
      },
      {
        label: $localize`:@@standards-extras.tile.reuse:Taux de mutualisation`,
        value: `${c.matrix.reuseRatioPercent.toFixed(1)} %`,
        hint: $localize`:@@standards-extras.tile.reuse-hint:Part des paires (clause × norme) déjà couvertes par une preuve existante.`,
        tone: c.matrix.reuseRatioPercent >= 30 ? 'success' : 'warn'
      },
      {
        label: $localize`:@@standards-extras.tile.mappings:Liens de couverture`,
        value: String(c.matrix.totalMappings),
        hint: $localize`:@@standards-extras.tile.mappings-hint:Nombre total de correspondances clause à clause dans le périmètre.`,
        tone: 'neutral'
      }
    ];
  }

  trackByTile(_index: number, tile: HubTile): string {
    return tile.label;
  }
}
