import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../../core/auth/auth.service';
import { StandardsService } from '../../standards.service';
import {
  AdoptionResponse, AiDraftResponse, AlignmentReport, AuditBlancReport, CertificationBlancReport,
  DocumentTemplate, DossierResponse, EvidenceResponse, EvidenceType, ProcessTemplate,
  RoadmapStageResponse, RoadmapSummary, StageStatus, StandardDetail, StandardRevision,
  StoryboardResponse
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
        this.svc.getStandardDetail(a.standardId).subscribe(s => this.standard = s);
        this.svc.listDocumentTemplates(a.standardId).subscribe(d => this.docTemplates = d);
        this.svc.listProcessTemplates(a.standardId).subscribe(p => this.processTemplates = p);
        this.svc.listRevisions(a.standardId).subscribe(r => this.revisions = r);
        this.loadReports();
        this.loading = false;
      },
      error: () => { this.error = $localize`:@@standards.detail.load-error:Impossible de charger l'adoption.`; this.loading = false; }
    });
  }

  private loadReports(): void {
    this.svc.getAlignment(this.adoptionId).subscribe(r => this.alignment = r);
    this.svc.getRoadmap(this.adoptionId).subscribe(r => this.roadmap = r);
    this.svc.getAuditBlanc(this.adoptionId).subscribe(r => this.audit = r);
    this.svc.listEvidence(this.adoptionId).subscribe(r => this.evidence = r);
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
        this.svc.getRoadmap(this.adoptionId).subscribe(r => this.roadmap = r);
      },
      error: () => this.snack.open($localize`:@@standards.detail.stage-update-error:Échec de la mise à jour`, $localize`:@@common.close:Fermer`, { duration: 3000 })
    });
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
