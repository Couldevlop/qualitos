import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Observable } from 'rxjs';

import { AuthService } from '../../../../core/auth/auth.service';
import {
  TreeNodeDialogComponent, TreeNodeDialogData, TreeNodeResult
} from '../tree-node-dialog/tree-node-dialog.component';
import { StandardsService } from '../../standards.service';
import {
  AdoptionResponse, AiDraftResponse, AlignmentReport, AuditBlancReport, CertificationBlancReport,
  ClauseDetail, ClauseRequest, DocumentTemplate, DossierResponse, EvidenceResponse, EvidenceType,
  ProcessTemplate, RequirementDetail, RequirementRequest, RoadmapStageResponse, RoadmapSummary,
  SectionDetail, SectionRequest, StageStatus, StandardDetail, StandardRevision, StoryboardResponse
} from '../../standards.types';

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
  evidence: EvidenceResponse[] = [];

  dossier?: DossierResponse;
  generatingDossier = false;

  storyboard?: StoryboardResponse;
  generatingStoryboard = false;

  certBlanc?: CertificationBlancReport;
  runningCertBlanc = false;

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
  /** Index de l'onglet « Mes preuves » dans le mat-tab-group (ordre statique). */
  private readonly EVIDENCE_TAB_INDEX = 4;

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
    private readonly snack: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.adoptionId = this.route.snapshot.paramMap.get('id')!;
    this.reloadAll();
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
    this.svc.getAuditBlanc(this.adoptionId).subscribe({
      next: r => this.audit = r,
      error: e => console.warn('[standards-detail] getAuditBlanc failed', e?.status)
    });
    this.svc.listEvidence(this.adoptionId).subscribe({
      next: r => this.evidence = r,
      error: e => console.warn('[standards-detail] listEvidence failed', e?.status)
    });
  }

  /** Ensemble des IDs d'exigences couvertes par au moins une preuve (vue Exigences). */
  get coveredRequirementIds(): Set<string> {
    return new Set(this.evidence.map(e => e.requirementId));
  }

  // ---- Roadmap ----

  changeStageStatus(stage: RoadmapStageResponse, status: StageStatus): void {
    if (stage.status === status) return;
    this.svc.updateStage(this.adoptionId, stage.id, { status }).subscribe({
      next: () => {
        this.snack.open($localize`:@@standards.detail.stage-updated:Étape ${stage.stepNumber}:step: → ${status}:status:`, $localize`:@@common.ok:OK`, { duration: 2000 });
        this.svc.getRoadmap(this.adoptionId).subscribe({
          next: r => this.roadmap = r,
          error: e => console.warn('[standards-detail] getRoadmap reload failed', e?.status)
        });
      },
      error: () => this.snack.open($localize`:@@standards.detail.stage-update-error:Échec de la mise à jour`, $localize`:@@common.close:Fermer`, { duration: 3000 })
    });
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

  // ---- Certification à blanc ----

  runCertificationBlanc(): void {
    this.runningCertBlanc = true;
    this.svc.runCertificationBlanc(this.adoptionId).subscribe({
      next: r => {
        this.certBlanc = r;
        this.runningCertBlanc = false;
        this.snack.open($localize`:@@standards.detail.cert-blanc-success:Certification à blanc simulée et ancrée`, $localize`:@@common.ok:OK`, { duration: 2500 });
      },
      error: () => {
        this.runningCertBlanc = false;
        this.snack.open($localize`:@@standards.detail.cert-blanc-error:Échec de la simulation`, $localize`:@@common.close:Fermer`, { duration: 3000 });
      }
    });
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
    return sev === 'CRITICAL' ? 'sev-crit' : sev === 'MAJOR' ? 'sev-maj' : 'sev-min';
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
