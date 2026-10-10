import { Component, DestroyRef, inject, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, finalize, map } from 'rxjs/operators';

import { AuthzService } from '../../../../core/authz/authz.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { PageBreadcrumb } from '../../../../shared/ui/page-header/page-header.component';
import { RiskRegisterService } from '../../risk-register.service';
import {
  Choice, FEASIBILITIES, GAINS, OPPORTUNITY_DECISIONS, OPPORTUNITY_ORIGINS, OPPORTUNITY_REQUIREMENTS,
  OPPORTUNITY_STATUSES, PROBABILITIES, RISK_DECISIONS, RISK_ORIGINS, RISK_REQUIREMENTS, RISK_STATUSES,
  SEVERITIES, TYPES, levelOf, opportunityLevelLabel, originLabel, riskLevelLabel
} from '../../risk-register.labels';
import {
  OpportunityLevel, OpportunityRequest, OpportunityView, RegisterOrigin, RegisterRequirement,
  RegisterSuggestions, RiskDraft, RiskLevel, RiskRequest, RiskSourceOrigin, RiskView
} from '../../risk-register.types';

export type ItemKind = 'risk' | 'opportunity';

const SOURCE_ORIGINS: RiskSourceOrigin[] = ['FMEA', 'NON_CONFORMITY', 'AUDIT', 'CHANGE'];

/**
 * Le formulaire d'un risque ou d'une opportunité — création et modification.
 *
 * <p>À la création, il ne demande que ce qu'on sait déjà : identification,
 * description, origine, cotation brute, décision, exigences. La cotation
 * résiduelle, le statut, la prochaine revue et le critère d'efficacité
 * n'apparaissent qu'en modification : ils n'existent qu'une fois le risque
 * vécu.
 *
 * <p>Ouvert depuis une AMDEC, une NC, un audit ou un changement
 * (`?origine=FMEA&source=<id>`), il demande au serveur le brouillon de l'objet
 * source : le serveur relit l'objet dans le client du jeton, et l'origine est
 * figée — le risque ne peut pas changer d'histoire.
 */
@Component({
  selector: 'qos-risk-item-form',
  templateUrl: './item-form.component.html',
  styleUrls: ['./item-form.component.scss'],
  standalone: false
})
export class ItemFormComponent implements OnInit {

  readonly types = TYPES;
  readonly severities = SEVERITIES;
  readonly probabilities = PROBABILITIES;
  readonly gains = GAINS;
  readonly feasibilities = FEASIBILITIES;
  readonly riskStatuses = RISK_STATUSES;
  readonly opportunityStatuses = OPPORTUNITY_STATUSES;
  /** Faux tant que les droits ne sont pas lus ; {@link rightsKnown} dit quand ils le sont. */
  editable = false;
  rightsKnown = false;
  private readonly destroyRef = inject(DestroyRef);

  readonly newRiskTitle = $localize`:@@rr.form.new-risk:Nouveau risque`;
  readonly editRiskTitle = $localize`:@@rr.form.edit-risk:Modifier le risque`;
  readonly newOpportunityTitle = $localize`:@@rr.form.new-opportunity:Nouvelle opportunité`;
  readonly editOpportunityTitle = $localize`:@@rr.form.edit-opportunity:Modifier l'opportunité`;
  readonly riskTitlePlaceholder = $localize`:@@rr.form.risk-title-placeholder:Décrire le risque en une phrase`;
  readonly opportunityTitlePlaceholder =
    $localize`:@@rr.form.opportunity-title-placeholder:Décrire l'opportunité en une phrase`;
  readonly causePlaceholder = $localize`:@@rr.form.cause-placeholder:Qu'est-ce qui peut provoquer ce risque ?`;
  readonly effectPlaceholder = $localize`:@@rr.form.effect-placeholder:Quelles conséquences s'il se produit ?`;
  readonly contextPlaceholder =
    $localize`:@@rr.form.context-placeholder:Qu'est-ce qui rend cette opportunité possible ?`;
  readonly benefitPlaceholder = $localize`:@@rr.form.benefit-placeholder:Quel gain en attendez-vous ?`;

  kind: ItemKind = 'risk';
  /** Identifiant de la fiche modifiée ; nul en création. */
  itemId: string | null = null;
  reference: string | null = null;
  /** Le brouillon de l'objet source, en création depuis une AMDEC, une NC, un audit ou un changement. */
  draft: RiskDraft | null = null;
  /** Vrai quand le risque modifié est issu d'un objet : son origine ne se change plus. */
  originLocked = false;

  suggestions: RegisterSuggestions = { processes: [], sites: [], owners: [] };
  loading = false;
  saving = false;
  failed = false;

  form!: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly service: RiskRegisterService,
    private readonly route: ActivatedRoute,
    private readonly router: Router,
    private readonly snack: MatSnackBar,
    private readonly authz: AuthzService
  ) {}

  ngOnInit(): void {
    const snap = this.route.snapshot;
    this.kind = snap.data['kind'] === 'opportunity' ? 'opportunity' : 'risk';
    this.itemId = snap.paramMap.get('id');
    // Risque ou opportunité : chacun son action (ADR 0078). Le serveur tranche ;
    // l'écran n'affiche que ce qui servira.
    this.authz.can(this.kind === 'risk' ? 'risk.manage' : 'opportunity.manage')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(v => { this.editable = v; this.rightsKnown = true; });
    this.form = this.construire();
    this.service.suggestions().pipe(catchError(() => of(this.suggestions)))
      .subscribe(s => this.suggestions = s);

    if (this.itemId) {
      this.chargerFiche(this.itemId);
      return;
    }
    const origine = snap.queryParamMap.get('origine') as RiskSourceOrigin | null;
    const source = snap.queryParamMap.get('source');
    if (this.kind === 'risk' && origine && source && SOURCE_ORIGINS.includes(origine)) {
      this.chargerBrouillon(origine, source);
    }
  }

  // ---------- gabarit ----------

  get isRisk(): boolean { return this.kind === 'risk'; }
  get isEdit(): boolean { return this.itemId !== null; }

  /** Risques & opportunités › Registre|Opportunités › Nouveau risque|R-014. */
  get breadcrumbs(): PageBreadcrumb[] {
    const racine: PageBreadcrumb = { label: $localize`:@@rr.title:Risques et opportunités`, route: '/risques' };
    const registre: PageBreadcrumb = this.isRisk
      ? { label: $localize`:@@rr.crumb.register:Registre`, route: '/risques' }
      : { label: $localize`:@@rr.tab-opportunities:Opportunités`, route: '/risques/opportunites' };
    const fin = this.isEdit
      ? (this.reference ?? '…')
      : (this.isRisk ? this.newRiskTitle : this.newOpportunityTitle);
    return [racine, registre, { label: fin }];
  }

  get origins(): ReadonlyArray<Choice<RegisterOrigin>> {
    return this.isRisk ? RISK_ORIGINS : OPPORTUNITY_ORIGINS;
  }

  get requirements(): ReadonlyArray<Choice<RegisterRequirement>> {
    return this.isRisk ? RISK_REQUIREMENTS : OPPORTUNITY_REQUIREMENTS;
  }

  get decisions(): ReadonlyArray<Choice<string>> {
    return this.isRisk ? RISK_DECISIONS : OPPORTUNITY_DECISIONS;
  }

  get originText(): string {
    return originLabel(this.form.get('origin')?.value);
  }

  /** L'aperçu du niveau, mêmes seuils que le serveur — le serveur reste celui qui l'enregistre. */
  get grossScore(): number {
    const v = this.form.getRawValue();
    return this.isRisk ? v.grossSeverity * v.grossProbability : v.gain * v.feasibility;
  }

  get grossLevel(): RiskLevel | OpportunityLevel {
    const niveau = levelOf(this.grossScore);
    return !this.isRisk && niveau === 'CRITICAL' ? 'PRIORITY' : niveau;
  }

  get grossLevelText(): string {
    return this.isRisk ? riskLevelLabel(this.grossLevel as RiskLevel)
      : opportunityLevelLabel(this.grossLevel as OpportunityLevel);
  }

  get residualScore(): number | null {
    const v = this.form.getRawValue();
    return v.residualSeverity && v.residualProbability ? v.residualSeverity * v.residualProbability : null;
  }

  get residualLevelText(): string {
    const s = this.residualScore;
    return s === null ? '' : riskLevelLabel(levelOf(s));
  }

  get residualLevel(): RiskLevel | null {
    const s = this.residualScore;
    return s === null ? null : levelOf(s);
  }

  /** Une résiduelle au-dessus de la brute n'est pas un traitement : on le dit avant l'envoi. */
  get residualTooHigh(): boolean {
    const s = this.residualScore;
    return s !== null && s > this.grossScore;
  }

  hasRequirement(code: RegisterRequirement): boolean {
    return (this.form.get('requirements')?.value as RegisterRequirement[]).includes(code);
  }

  toggleRequirement(code: RegisterRequirement, checked: boolean): void {
    const actuelles = this.form.get('requirements')!.value as RegisterRequirement[];
    const suivantes = checked ? [...new Set([...actuelles, code])] : actuelles.filter(c => c !== code);
    this.form.get('requirements')!.setValue(suivantes);
    this.form.markAsDirty();
  }

  filtered(liste: string[], controlName: string): string[] {
    const q = String(this.form.get(controlName)?.value ?? '').trim().toLowerCase();
    return liste.filter(v => !q || v.toLowerCase().includes(q)).slice(0, 20);
  }

  serverError(controlName: string): string | null {
    return this.form.get(controlName)?.getError('serveur') ?? null;
  }

  // ---------- chargement ----------

  private construire(): FormGroup {
    const commun = {
      title: ['', [Validators.required, Validators.maxLength(255)]],
      type: ['QUALITY', Validators.required],
      process: ['', [Validators.required, Validators.maxLength(120)]],
      site: ['', Validators.maxLength(120)],
      owner: ['', [Validators.required, Validators.maxLength(150)]],
      origin: ['DIRECT' as RegisterOrigin, Validators.required],
      originRef: ['', Validators.maxLength(120)],
      decision: ['UNDECIDED', Validators.required],
      requirements: [['ISO_9001_6_1'] as RegisterRequirement[]],
      status: ['']
    };
    if (this.kind === 'risk') {
      return this.fb.nonNullable.group({
        ...commun,
        // Obligatoires : sans cause on ne sait pas quoi traiter, sans effet on
        // ne sait pas ce que la gravité mesure. Le serveur l'exige aussi.
        cause: ['', [Validators.required, Validators.maxLength(4000)]],
        effect: ['', [Validators.required, Validators.maxLength(4000)]],
        grossSeverity: [3, Validators.required],
        grossProbability: [3, Validators.required],
        residualSeverity: [null as number | null],
        residualProbability: [null as number | null],
        nextReviewOn: [''],
        effectivenessCriterion: ['', Validators.maxLength(1000)]
      });
    }
    return this.fb.nonNullable.group({
      ...commun,
      targetDate: [''],
      context: ['', Validators.maxLength(4000)],
      benefit: ['', Validators.maxLength(4000)],
      gain: [3, Validators.required],
      feasibility: [3, Validators.required],
      benefitCriterion: ['', Validators.maxLength(1000)]
    });
  }

  private chargerFiche(id: string): void {
    this.loading = true;
    const fiche$: Observable<RiskView | OpportunityView> = this.isRisk
      ? this.service.risk(id).pipe(map(s => s.risk))
      : this.service.opportunity(id).pipe(map(s => s.opportunity));
    fiche$.pipe(finalize(() => this.loading = false)).subscribe({
      next: item => this.remplir(item),
      error: err => {
        this.failed = true;
        this.echouer(err);
      }
    });
  }

  private remplir(item: RiskView | OpportunityView): void {
    this.reference = item.reference;
    const commun = {
      title: item.title, type: item.type, process: item.process, site: item.site ?? '', owner: item.owner,
      origin: item.origin, originRef: item.originRef ?? '', decision: item.decision,
      requirements: [...item.requirements], status: item.status
    };
    if (this.isRisk) {
      const r = item as RiskView;
      this.originLocked = !!r.sourceId;
      this.form.patchValue({
        ...commun, cause: r.cause ?? '', effect: r.effect ?? '', grossSeverity: r.grossSeverity,
        grossProbability: r.grossProbability, residualSeverity: r.residualSeverity,
        residualProbability: r.residualProbability, nextReviewOn: r.nextReviewOn ?? '',
        effectivenessCriterion: r.effectivenessCriterion ?? ''
      });
    } else {
      const o = item as OpportunityView;
      this.form.patchValue({
        ...commun, targetDate: o.targetDate ?? '', context: o.context ?? '', benefit: o.benefit ?? '',
        gain: o.gain, feasibility: o.feasibility, benefitCriterion: o.benefitCriterion ?? ''
      });
    }
    this.verrouillerOrigine();
    this.form.markAsPristine();
  }

  private chargerBrouillon(origine: RiskSourceOrigin, source: string): void {
    this.loading = true;
    this.service.draft(origine, source).pipe(finalize(() => this.loading = false)).subscribe({
      next: d => {
        this.draft = d;
        this.originLocked = true;
        this.form.patchValue({
          title: d.title ?? '', cause: d.cause ?? '', effect: d.effect ?? '', origin: d.origin,
          originRef: d.originRef, process: d.process ?? this.form.get('process')!.value,
          grossSeverity: d.grossSeverity ?? 3, grossProbability: d.grossProbability ?? 3
        });
        this.verrouillerOrigine();
      },
      error: err => {
        this.failed = true;
        this.echouer(err);
      }
    });
  }

  /** L'origine d'un risque issu d'un objet est celle de l'objet : les deux champs passent en lecture seule. */
  private verrouillerOrigine(): void {
    if (this.originLocked) {
      this.form.get('origin')!.disable({ emitEvent: false });
      this.form.get('originRef')!.disable({ emitEvent: false });
    }
  }

  // ---------- enregistrement ----------

  cancel(): void {
    if (this.itemId) {
      this.router.navigate(this.isRisk ? ['/risques', this.itemId] : ['/risques', 'opportunites', this.itemId]);
    } else {
      this.router.navigate(this.isRisk ? ['/risques'] : ['/risques', 'opportunites']);
    }
  }

  submit(): void {
    if (!this.editable || this.saving) return;
    if (this.form.invalid || (this.isRisk && this.residualTooHigh)) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.draft && !this.draft.eligible) return;
    this.saving = true;
    const save$: Observable<{ id: string }> = this.isRisk
      ? (this.itemId ? this.service.reviseRisk(this.itemId, this.riskRequest())
        : this.service.createRisk(this.riskRequest()))
      : (this.itemId ? this.service.reviseOpportunity(this.itemId, this.opportunityRequest())
        : this.service.createOpportunity(this.opportunityRequest()));
    save$.pipe(finalize(() => this.saving = false)).subscribe({
      next: saved => {
        this.snack.open(this.isRisk
          ? $localize`:@@rr.form.risk-saved:Risque enregistré.`
          : $localize`:@@rr.form.opportunity-saved:Opportunité enregistrée.`, undefined, { duration: 2500 });
        this.form.markAsPristine();
        this.router.navigate(this.isRisk ? ['/risques', saved.id] : ['/risques', 'opportunites', saved.id]);
      },
      error: err => this.echouer(err)
    });
  }

  riskRequest(): RiskRequest {
    const v = this.form.getRawValue();
    return {
      title: v.title.trim(), type: v.type, process: v.process.trim(), site: v.site.trim() || null,
      owner: v.owner.trim(), cause: v.cause.trim() || null, effect: v.effect.trim() || null,
      origin: v.origin, originRef: v.originRef.trim() || null,
      sourceId: this.draft?.sourceId ?? null,
      grossSeverity: v.grossSeverity, grossProbability: v.grossProbability,
      residualSeverity: this.isEdit ? v.residualSeverity : null,
      residualProbability: this.isEdit ? v.residualProbability : null,
      decision: v.decision, status: this.isEdit && v.status ? v.status : undefined,
      requirements: v.requirements,
      nextReviewOn: this.isEdit ? (v.nextReviewOn || null) : null,
      effectivenessCriterion: this.isEdit ? (v.effectivenessCriterion.trim() || null) : null
    };
  }

  opportunityRequest(): OpportunityRequest {
    const v = this.form.getRawValue();
    return {
      title: v.title.trim(), type: v.type, process: v.process.trim(), site: v.site.trim() || null,
      owner: v.owner.trim(), targetDate: v.targetDate || null, context: v.context.trim() || null,
      benefit: v.benefit.trim() || null, origin: v.origin, originRef: v.originRef.trim() || null,
      gain: v.gain, feasibility: v.feasibility, decision: v.decision,
      status: this.isEdit && v.status ? v.status : undefined, requirements: v.requirements,
      benefitCriterion: this.isEdit ? (v.benefitCriterion.trim() || null) : null
    };
  }

  /**
   * Un 422 porte le champ fautif : on l'accroche au contrôle concerné, en plus
   * du message, pour que l'utilisateur sache où corriger.
   */
  private echouer(err: unknown): void {
    const field = err instanceof HttpErrorResponse ? err.error?.field : undefined;
    const control = typeof field === 'string' ? this.form.get(field) : null;
    if (control && err instanceof HttpErrorResponse) {
      control.setErrors({ serveur: err.error?.detail ?? true });
      control.markAsTouched();
    }
    this.snack.open(safeErrorMessage(err, $localize`:@@rr.form.save-failed:L'enregistrement a échoué.`),
      undefined, { duration: 5000 });
  }
}
