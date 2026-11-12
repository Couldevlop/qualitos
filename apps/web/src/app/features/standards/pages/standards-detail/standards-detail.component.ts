import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';

import { AuthService } from '../../../../core/auth/auth.service';
import { DirectoryUser } from '../../../../core/directory/directory.types';
import { safeErrorMessage } from '../../../../core/http/error-message';
import { StandardsExtrasService } from '../../../standards-extras/standards-extras.service';
import { MockAuditReport } from '../../../standards-extras/standards-extras.types';
import {
  TreeNodeDialogComponent, TreeNodeDialogData, TreeNodeResult
} from '../tree-node-dialog/tree-node-dialog.component';
import { StandardsService } from '../../standards.service';
import {
  AdoptionResponse, AiDraftResponse, AlignmentReport, AuditBlancReport,
  ClauseDetail, ClauseRequest, DocumentTemplate, DossierResponse, EvidenceResponse, EvidenceType,
  ProcessTemplate, RequirementDetail, RequirementRequest, RoadmapStageResponse, RoadmapSummary,
  SectionDetail, SectionRequest, StageStatus, StandardDetail, StandardRevision, StoryboardResponse,
  UpdateStageRequest
} from '../../standards.types';

/**
 * Les rôles que `MockAuditController` accepte pour lancer un audit blanc IA.
 * Écrits ici pour ne pas proposer un bouton qu'on sait refusé ; le serveur
 * reste seul juge.
 */
const MOCK_AUDIT_ROLES = [
  'quality_manager', 'director_quality', 'auditor', 'admin', 'admin_tenant', 'super_admin'
];

@Component({
  selector: 'qos-standards-detail',
  templateUrl: './standards-detail.component.html',
  styleUrls: ['./standards-detail.component.scss'],
  standalone: false
})
export class StandardsDetailComponent implements OnInit {

  adoptionId!: string;
  loading = true;
  error?: string;

  adoption?: AdoptionResponse;
  standard?: StandardDetail;
  alignment?: AlignmentReport;
  roadmap?: RoadmapSummary;
  audit?: AuditBlancReport;
  /** L'analyse d'écarts par règles n'a pas pu être lue : on le dit, on propose de relancer. */
  auditFailed = false;
  evidence: EvidenceResponse[] = [];

  /** Audit blanc IA (ADR 0033) : exécutions passées de CETTE adoption, la plus récente d'abord. */
  mockAuditRuns: MockAuditReport[] = [];
  mockAuditRunning = false;
  mockAuditError?: string;
  /** Faute de rôle lisible (dev, claims absents) on n'ampute pas l'écran : le serveur tranche. */
  canRunMockAudit = true;

  dossier?: DossierResponse;
  generatingDossier = false;

  storyboard?: StoryboardResponse;
  generatingStoryboard = false;

  docTemplates: DocumentTemplate[] = [];
  processTemplates: ProcessTemplate[] = [];
  revisions: StandardRevision[] = [];

  aiDraft?: AiDraftResponse;
  generatingDraftId?: string;

  // Formulaire de liaison de preuve.
  linkRequirementId = '';
  linkEvidenceType: EvidenceType = 'DOCUMENT';
  linkNote = '';
  linkUri = '';
  linking = false;

  // Onglet actif (piloté pour le saut « Couvrir cet écart »).
  selectedTab = 0;
  /**
   * Index de l'onglet « Mes preuves » dans le mat-tab-group (ordre statique) :
   * Exigences, Bibliotheque, Cartographie, Roadmap, Audit blanc, Mes preuves.
   * Il valait 4, soit l'onglet « Audit blanc » : « Couvrir cet ecart » laissait
   * donc le lecteur sur la page d'ou il venait de cliquer.
   */
  private readonly EVIDENCE_TAB_INDEX = 5;

  readonly stageStatuses: StageStatus[] = ['NOT_STARTED', 'IN_PROGRESS', 'DONE', 'SKIPPED'];
  readonly evidenceTypes: EvidenceType[] = [
    'DOCUMENT', 'AUDIT', 'CAPA', 'PDCA_CYCLE', 'ISHIKAWA', 'FIVES_AUDIT',
    'TRAINING_RECORD', 'KPI_RECORD', 'EXTERNAL_FILE', 'OTHER'
  ];

  constructor(
    private readonly route: ActivatedRoute,
    private readonly svc: StandardsService,
    private readonly auth: AuthService,
    private readonly dialog: MatDialog,
    private readonly snack: MatSnackBar,
    private readonly router: Router,
    private readonly extras: StandardsExtrasService
  ) {}

  ngOnInit(): void {
    this.adoptionId = this.route.snapshot.paramMap.get('id')!;
    this.canRunMockAudit = this.hasAuditRole();
    this.reloadAll();
  }

  /** Les rôles habilités à déclencher l'audit blanc IA (miroir de `MockAuditController`). */
  private hasAuditRole(): boolean {
    const roles = this.auth.snapshot()?.roles ?? [];
    return roles.length === 0 || roles.some(r => MOCK_AUDIT_ROLES.includes(r));
  }

  private reloadAll(): void {
    this.loading = true;
    this.svc.getAdoption(this.adoptionId).subscribe({
      next: a => {
        this.adoption = a;
        // Chaque sous-chargement porte son propre `error` arm : sans lui, un échec
        // (notamment un 504 sur les rapports adossés à l'IA) remontait en erreur
        // non gérée (console) et figeait la section. On dégrade en silence (warn).
        this.svc.getStandardDetail(a.standardId).subscribe({
          next: s => this.standard = s,
          error: e => console.warn('[standards-detail] getStandardDetail failed', e?.status)
        });
        this.svc.listDocumentTemplates(a.standardId).subscribe({
          next: d => this.docTemplates = d,
          error: e => console.warn('[standards-detail] listDocumentTemplates failed', e?.status)
        });
        this.svc.listProcessTemplates(a.standardId).subscribe({
          next: p => this.processTemplates = p,
          error: e => console.warn('[standards-detail] listProcessTemplates failed', e?.status)
        });
        this.svc.listRevisions(a.standardId).subscribe({
          next: r => this.revisions = r,
          error: e => console.warn('[standards-detail] listRevisions failed', e?.status)
        });
        this.loadReports();
        this.loading = false;
      },
      error: () => { this.error = $localize`:@@standards.detail.load-error:Impossible de charger l'adoption.`; this.loading = false; }
    });
  }

  private loadReports(): void {
    // getAlignment/getAuditBlanc sont adossés à l'IA (Ollama) → potentiellement
    // lents/504 à froid : chaque subscribe a un `error` arm pour ne jamais laisser
    // remonter une erreur non gérée (cf. recette : 504 → erreur console).
    this.svc.getAlignment(this.adoptionId).subscribe({
      next: r => this.alignment = r,
      error: e => console.warn('[standards-detail] getAlignment failed', e?.status)
    });
    this.svc.getRoadmap(this.adoptionId).subscribe({
      next: r => this.roadmap = r,
      error: e => console.warn('[standards-detail] getRoadmap failed', e?.status)
    });
    this.loadAuditBlanc();
    this.loadMockAuditRuns();
    this.svc.listEvidence(this.adoptionId).subscribe({
      next: r => this.evidence = r,
      error: e => console.warn('[standards-detail] listEvidence failed', e?.status)
    });
  }

  // ---- Audit blanc ----

  /**
   * L'analyse d'écarts par règles. Son échec laissait l'onglet VIDE, sans un mot
   * : on retient le refus pour pouvoir le dire et le relancer.
   */
  loadAuditBlanc(): void {
    this.auditFailed = false;
    this.svc.getAuditBlanc(this.adoptionId).subscribe({
      next: r => this.audit = r,
      error: e => {
        console.warn('[standards-detail] getAuditBlanc failed', e?.status);
        this.auditFailed = true;
      }
    });
  }

  /** Les audits blancs IA déjà passés sur cette adoption (relecture, deep link). */
  private loadMockAuditRuns(): void {
    this.extras.mockAuditHistory(this.adoptionId).subscribe({
      next: runs => this.mockAuditRuns = runs,
      error: e => console.warn('[standards-detail] mockAuditHistory failed', e?.status)
    });
  }

  /**
   * Lance un audit blanc IA sur cette norme adoptée (§8.4 onglet 7, ADR 0033) et
   * ouvre son rapport.
   *
   * <p>La capacité existait, sans porte depuis la fiche de la norme : il fallait
   * passer par le hub IMS et y re-sélectionner l'adoption qu'on avait sous les
   * yeux. Le rapport vit dans son écran dédié — un audit blanc se partage par
   * son URL et se relit plus tard.
   */
  runMockAudit(): void {
    if (this.mockAuditRunning) return;
    this.mockAuditRunning = true;
    this.mockAuditError = undefined;
    this.extras.runMockAudit(this.adoptionId)
      .pipe(finalize(() => this.mockAuditRunning = false))
      .subscribe({
        next: report => this.openMockAudit(report.id),
        error: err => this.mockAuditError = this.mockAuditErrorMessage(err)
      });
  }

  openMockAudit(runId: string): void {
    this.router.navigate(['/standards-ims', 'audit-blanc-ia', this.adoptionId, runId]);
  }

  /** Niveau de préparation d'une exécution, en pourcentage entier. */
  readinessLabel(readiness: number): string {
    return `${Math.round(readiness)} %`;
  }

  private mockAuditErrorMessage(err: unknown): string {
    const status = (err as { status?: number } | null)?.status;
    if (status === 409) {
      return $localize`:@@standards.detail.mock-audit-no-clause:Cette norme adoptée n'expose aucune clause exploitable : chargez son référentiel avant de lancer un audit blanc IA.`;
    }
    if (status === 403) {
      return $localize`:@@standards.detail.mock-audit-forbidden:Lancer un audit blanc IA relève du pilotage qualité : demandez-le au manager qualité ou à l'auditeur.`;
    }
    return safeErrorMessage(err,
      $localize`:@@standards.detail.mock-audit-failed:L'audit blanc IA n'a pas pu être lancé. Réessayez dans quelques minutes.`);
  }

  /** Ensemble des IDs d'exigences couvertes par au moins une preuve (vue Exigences). */
  get coveredRequirementIds(): Set<string> {
    return new Set(this.evidence.map(e => e.requirementId));
  }

  // ---- Roadmap ----

  changeStageStatus(stage: RoadmapStageResponse, status: StageStatus): void {
    if (stage.status === status) return;
    this.patchStage(stage, { status },
      $localize`:@@standards.detail.stage-updated:Étape ${stage.stepNumber}:step: → ${status}:status:`);
  }

  /**
   * Le responsable d'une étape est une personne de l'annuaire, choisie dans la
   * liste déroulante — pas un rôle écrit à la main. Le rôle type du modèle
   * (« Pilote certif », « Direction ») reste affiché en indication : il dit
   * quelle fonction est attendue, l'annuaire dit qui l'assume ici.
   */
  changeStageOwner(stage: RoadmapStageResponse, user: DirectoryUser | null): void {
    // Vider la case ne retire pas le responsable : le serveur n'applique que les
    // champs renseignes, et un `null` y serait un appel sans effet. On s'abstient
    // donc plutot que de laisser croire a un retrait qui n'a pas eu lieu.
    const assigneeId = user?.accountId;
    if (!assigneeId || stage.assigneeId === assigneeId) return;
    this.patchStage(stage, { assigneeId },
      $localize`:@@standards.detail.stage-owner-updated:Responsable de l'étape ${stage.stepNumber}:step: enregistré`);
  }

  /**
   * L'échéance remplace la durée indicative du modèle : une durée (« 3-5 sem »)
   * ne dit pas quand l'étape doit être finie, et c'est la seule chose qu'un
   * pilote de certification a besoin de lire dans ce tableau.
   */
  changeStageDeadline(stage: RoadmapStageResponse, valeur: Date | null): void {
    // Meme raison que pour le responsable : une echeance ne s'effface pas d'ici.
    const plannedEndDate = StandardsDetailComponent.enDateIso(valeur);
    if (!plannedEndDate || stage.plannedEndDate === plannedEndDate) return;
    this.patchStage(stage, { plannedEndDate },
      $localize`:@@standards.detail.stage-deadline-updated:Échéance de l'étape ${stage.stepNumber}:step: enregistrée`);
  }

  /** L'échéance du serveur en {@code Date} locale, pour le calendrier. */
  dateDeEcheance(stage: RoadmapStageResponse): Date | null {
    return StandardsDetailComponent.enDateLocale(stage.plannedEndDate);
  }

  /** Une seule écriture, un seul rechargement : les trois cases le partagent. */
  private patchStage(
    stage: RoadmapStageResponse,
    patch: UpdateStageRequest,
    message: string
  ): void {
    this.svc.updateStage(this.adoptionId, stage.id, patch).subscribe({
      next: () => {
        this.snack.open(message, $localize`:@@common.ok:OK`, { duration: 2000 });
        this.svc.getRoadmap(this.adoptionId).subscribe({
          next: r => this.roadmap = r,
          error: e => console.warn('[standards-detail] getRoadmap reload failed', e?.status)
        });
      },
      error: () => this.snack.open($localize`:@@standards.detail.stage-update-error:Échec de la mise à jour`, $localize`:@@common.close:Fermer`, { duration: 3000 })
    });
  }

  /**
   * Le calendrier Material porte un {@code Date} ; le serveur attend
   * « aaaa-mm-jj ». La conversion lit les composantes LOCALES : passer par
   * {@code toISOString} reculerait la date d'un jour à l'ouest de Greenwich.
   */
  private static enDateIso(valeur: Date | null): string | null {
    if (!(valeur instanceof Date) || Number.isNaN(valeur.getTime())) return null;
    const mois = String(valeur.getMonth() + 1).padStart(2, '0');
    const jour = String(valeur.getDate()).padStart(2, '0');
    return `${valeur.getFullYear()}-${mois}-${jour}`;
  }

  /** L'inverse, composante par composante — même piège de fuseau. */
  private static enDateLocale(valeur: string | null | undefined): Date | null {
    if (!valeur) return null;
    const parties = /^(\d{4})-(\d{2})-(\d{2})/.exec(valeur);
    if (!parties) return null;
    return new Date(+parties[1], +parties[2] - 1, +parties[3]);
  }

  // ---- Arborescence d'un référentiel du tenant (§8) ----

  /**
   * Recharge la fiche après chaque écriture au lieu de retoucher l'arbre en
   * mémoire. Reconstruire côté client ferait diverger l'écran de la base au
   * premier cas non prévu — ordre d'insertion, code normalisé par le serveur.
   */
  private reloadStandard(): void {
    const standardId = this.standard?.id ?? this.adoption?.standardId;
    if (!standardId) return;
    this.svc.getStandardDetail(standardId).subscribe({
      next: s => this.standard = s,
      error: e => console.warn('[standards-detail] reload standard failed', e?.status)
    });
  }

  addSection(): void {
    this.editNode({ level: 'SECTION' }, req =>
      this.svc.addSection(this.standard!.id, req as SectionRequest));
  }

  editSection(sec: SectionDetail): void {
    this.editNode({ level: 'SECTION', node: sec }, req =>
      this.svc.updateSection(this.standard!.id, sec.id, req as SectionRequest));
  }

  addClause(sec: SectionDetail): void {
    this.editNode({ level: 'CLAUSE' }, req =>
      this.svc.addClause(this.standard!.id, sec.id, req as ClauseRequest));
  }

  editClause(cl: ClauseDetail): void {
    this.editNode({ level: 'CLAUSE', node: cl }, req =>
      this.svc.updateClause(this.standard!.id, cl.id, req as ClauseRequest));
  }

  addRequirement(cl: ClauseDetail): void {
    this.editNode({ level: 'REQUIREMENT' }, req =>
      this.svc.addRequirement(this.standard!.id, cl.id, req as RequirementRequest));
  }

  editRequirement(r: RequirementDetail): void {
    this.editNode({ level: 'REQUIREMENT', node: r }, req =>
      this.svc.updateRequirement(this.standard!.id, r.id, req as RequirementRequest));
  }

  /**
   * Supprimer une section emporte ses clauses, et une clause ses exigences : on
   * demande confirmation en DISANT ce qui part, plutôt qu'un « êtes-vous sûr ? »
   * que personne ne lit.
   */
  deleteSection(sec: SectionDetail): void {
    const count = sec.clauses.length;
    this.confirmThenWrite(
      $localize`:@@standards.tree.confirm-section:Supprimer la section ${sec.code}:code: et les ${count}:count: clauses qu'elle contient ?`,
      () => this.svc.deleteSection(this.standard!.id, sec.id));
  }

  deleteClause(cl: ClauseDetail): void {
    const count = cl.requirements.length;
    this.confirmThenWrite(
      $localize`:@@standards.tree.confirm-clause:Supprimer la clause ${cl.code}:code: et les ${count}:count: exigences qu'elle contient ?`,
      () => this.svc.deleteClause(this.standard!.id, cl.id));
  }

  deleteRequirement(r: RequirementDetail): void {
    this.confirmThenWrite(
      $localize`:@@standards.tree.confirm-requirement:Supprimer l'exigence ${r.code}:code: ?`,
      () => this.svc.deleteRequirement(this.standard!.id, r.id));
  }

  private editNode(data: TreeNodeDialogData,
                   write: (req: TreeNodeResult) => Observable<void>): void {
    this.dialog.open(TreeNodeDialogComponent, { data, autoFocus: 'dialog' })
      .afterClosed()
      .subscribe(req => {
        if (req) this.write(write(req));
      });
  }

  private confirmThenWrite(question: string, write: () => Observable<void>): void {
    if (!confirm(question)) return;
    this.write(write());
  }

  private write(call: Observable<void>): void {
    call.subscribe({
      next: () => this.reloadStandard(),
      error: e => this.snack.open(this.treeErrorMessage(e?.status),
        $localize`:@@common.close:Fermer`, { duration: 4000 })
    });
  }

  /**
   * 409 et 403 appellent deux gestes différents — changer de code, ou renoncer
   * parce que la norme vient de la plateforme. Les confondre laisserait
   * l'utilisateur relancer indéfiniment une action qui n'aboutira jamais.
   */
  private treeErrorMessage(status: number | undefined): string {
    if (status === 409) {
      return $localize`:@@standards.tree.error-conflict:Ce code est déjà pris à ce niveau.`;
    }
    if (status === 403) {
      return $localize`:@@standards.tree.error-platform:Une norme de la plateforme ne se modifie pas.`;
    }
    return $localize`:@@standards.tree.error-generic:Modification impossible pour le moment.`;
  }

  // ---- Preuves ----

  /** Saute à l'onglet « Mes preuves » avec l'exigence pré-sélectionnée (boucle de remédiation). */
  coverRequirement(requirementId: string): void {
    this.linkRequirementId = requirementId;
    this.selectedTab = this.EVIDENCE_TAB_INDEX;
  }

  linkEvidence(): void {
    if (!this.linkRequirementId) {
      this.snack.open($localize`:@@standards.detail.select-requirement:Sélectionnez une exigence`, $localize`:@@common.ok:OK`, { duration: 2500 });
      return;
    }
    const linkedBy = this.auth.snapshot()?.userId;
    if (!linkedBy) {
      this.snack.open($localize`:@@standards.detail.session-expired-short:Session expirée`, $localize`:@@common.close:Fermer`, { duration: 3000 });
      return;
    }
    this.linking = true;
    this.svc.linkEvidence(this.adoptionId, {
      requirementId: this.linkRequirementId,
      evidenceType: this.linkEvidenceType,
      note: this.linkNote || undefined,
      evidenceUri: this.linkUri || undefined,
      linkedBy
    }).subscribe({
      next: () => {
        this.linking = false;
        this.linkNote = ''; this.linkUri = ''; this.linkRequirementId = '';
        this.snack.open($localize`:@@standards.detail.evidence-linked:Preuve liée — scores recalculés`, $localize`:@@common.ok:OK`, { duration: 2500 });
        this.loadReports();
      },
      error: err => {
        this.linking = false;
        const msg = err?.status === 409
          ? $localize`:@@standards.detail.evidence-conflict:Cette preuve est déjà liée à cette exigence`
          : $localize`:@@standards.detail.evidence-link-error:Échec de la liaison de preuve`;
        this.snack.open(msg, $localize`:@@common.close:Fermer`, { duration: 3000 });
      }
    });
  }

  unlinkEvidence(ev: EvidenceResponse): void {
    this.svc.unlinkEvidence(this.adoptionId, ev.id).subscribe({
      next: () => {
        this.snack.open($localize`:@@standards.detail.evidence-unlinked:Preuve retirée`, $localize`:@@common.ok:OK`, { duration: 2000 });
        this.loadReports();
      },
      error: () => this.snack.open($localize`:@@standards.detail.evidence-unlink-error:Échec du retrait`, $localize`:@@common.close:Fermer`, { duration: 3000 })
    });
  }

  // ---- Dossier ----

  generateDossier(): void {
    this.generatingDossier = true;
    this.svc.generateDossier(this.adoptionId).subscribe({
      next: d => {
        this.dossier = d;
        this.generatingDossier = false;
        this.snack.open($localize`:@@standards.detail.dossier-success:Dossier généré et ancré (SHA-256)`, $localize`:@@common.ok:OK`, { duration: 2500 });
      },
      error: () => {
        this.generatingDossier = false;
        this.snack.open($localize`:@@standards.detail.dossier-error:Échec de la génération du dossier`, $localize`:@@common.close:Fermer`, { duration: 3000 });
      }
    });
  }

  generateStoryboard(): void {
    this.generatingStoryboard = true;
    this.svc.generateStoryboard(this.adoptionId).subscribe({
      next: s => {
        this.storyboard = s;
        this.generatingStoryboard = false;
      },
      error: () => {
        this.generatingStoryboard = false;
        this.snack.open($localize`:@@standards.detail.storyboard-unavailable:Récit IA indisponible (ai-service / Ollama)`, $localize`:@@common.close:Fermer`, { duration: 3500 });
      }
    });
  }

  downloadDossier(): void {
    if (!this.dossier) return;
    const blob = new Blob([this.dossier.htmlContent], { type: 'text/html;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = this.dossier.fileName;
    a.click();
    URL.revokeObjectURL(url);
  }

  // ---- Bibliothèque documentaire ----

  downloadTemplate(t: DocumentTemplate): void {
    if (!this.adoption) return;
    this.svc.downloadDocumentTemplate(this.adoption.standardId, t.id).subscribe({
      next: resp => {
        const blob = resp.body;
        if (!blob) { return; }
        const cd = resp.headers.get('Content-Disposition') || '';
        const m = /filename="?([^"]+)"?/.exec(cd);
        const filename = m ? m[1] : `${t.code}.md`;
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = filename; a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.snack.open($localize`:@@standards.detail.template-download-error:Téléchargement du modèle impossible`, $localize`:@@common.close:Fermer`, { duration: 3000 })
    });
  }

  // ---- Génération IA d'un brouillon (§8.8) ----

  generateAiDraft(t: DocumentTemplate): void {
    if (!this.adoption) return;
    this.generatingDraftId = t.id;
    this.aiDraft = undefined;
    this.svc.generateAiDraft(this.adoption.standardId, t.id).subscribe({
      next: r => {
        this.aiDraft = r;
        this.generatingDraftId = undefined;
        this.snack.open($localize`:@@standards.detail.ai-draft-success:Brouillon généré (${r.provider}:provider:, ${r.latencyMs}:latency: ms)`, $localize`:@@common.ok:OK`, { duration: 2500 });
      },
      error: () => {
        this.generatingDraftId = undefined;
        this.snack.open($localize`:@@standards.detail.ai-draft-unavailable:Génération IA indisponible (ai-service / Ollama)`, $localize`:@@common.close:Fermer`, { duration: 3500 });
      }
    });
  }

  // ---- helpers UI ----

  obligationClass(o: string): string {
    return o === 'MANDATORY' ? 'ob-MUST' : o === 'RECOMMENDED' ? 'ob-SHOULD' : 'ob-MAY';
  }

  obligationLabel(o: string): string {
    return o === 'MANDATORY'
      ? $localize`:@@standards.detail.obligation-mandatory:Obligatoire`
      : o === 'RECOMMENDED'
        ? $localize`:@@standards.detail.obligation-recommended:Recommandé`
        : $localize`:@@standards.detail.obligation-optional:Optionnel`;
  }

  revisionClass(status: string): string {
    return status === 'CURRENT' ? 'rev-current' : status === 'PLANNED' ? 'rev-planned' : 'rev-superseded';
  }

  clauseList(csv?: string): string[] {
    return csv ? csv.split(',').map(c => c.trim()).filter(c => c.length > 0) : [];
  }

  severityClass(sev: string): string {
    return sev === 'MAJOR' ? 'sev-crit' : sev === 'MINOR' ? 'sev-maj' : 'sev-min';
  }

  /**
   * Le terme de la certification, en clair.
   *
   * <p>Le tableau affichait le code brut du serveur — « CRITICAL », « MAJOR » —
   * et « critique » n'existe pas dans le vocabulaire d'un organisme
   * certificateur : un auditeur classe en non-conformité <b>majeure</b>
   * (bloquante jusqu'à sa levée), <b>mineure</b> (plan d'actions attendu) ou en
   * <b>observation</b> (piste d'amélioration, pas une non-conformité). Afficher
   * « critique » faisait préparer une réponse à un reproche qui ne viendra pas.
   */
  severityLabel(sev: string): string {
    return sev === 'MAJOR'
      ? $localize`:@@standards.detail.finding-major:Non-conformité majeure`
      : sev === 'MINOR'
        ? $localize`:@@standards.detail.finding-minor:Non-conformité mineure`
        : $localize`:@@standards.detail.finding-observation:Observation`;
  }

  ncClass(type: string): string {
    return type === 'MAJOR' ? 'sev-crit' : type === 'MINOR' ? 'sev-maj' : 'sev-min';
  }

  scoreClass(score: number): string {
    return score >= 80 ? 'ok' : score >= 50 ? 'warn' : 'bad';
  }

  decisionClass(decision?: string): string {
    if (decision === 'CERTIFIABLE') return 'ok';
    if (decision === 'CERTIFIABLE_SOUS_RESERVE') return 'warn';
    return 'bad';
  }
}
