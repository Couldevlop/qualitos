import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AuthService } from '../../../../core/auth/auth.service';
import { StandardsService } from '../../standards.service';
import {
  AdoptionResponse, AlignmentReport, AuditBlancReport, CertificationBlancReport,
  DossierResponse, EvidenceResponse, EvidenceType, RoadmapStageResponse, RoadmapSummary,
  StageStatus, StandardDetail
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

  certBlanc?: CertificationBlancReport;
  runningCertBlanc = false;

  // Formulaire de liaison de preuve.
  linkRequirementId = '';
  linkEvidenceType: EvidenceType = 'DOCUMENT';
  linkNote = '';
  linkUri = '';
  linking = false;

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
        this.loadReports();
        this.loading = false;
      },
      error: () => { this.error = "Impossible de charger l'adoption."; this.loading = false; }
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
        this.snack.open(`Étape ${stage.stepNumber} → ${status}`, 'OK', { duration: 2000 });
        this.svc.getRoadmap(this.adoptionId).subscribe(r => this.roadmap = r);
      },
      error: () => this.snack.open('Échec de la mise à jour', 'Fermer', { duration: 3000 })
    });
  }

  // ---- Preuves ----

  linkEvidence(): void {
    if (!this.linkRequirementId) {
      this.snack.open('Sélectionnez une exigence', 'OK', { duration: 2500 });
      return;
    }
    const linkedBy = this.auth.snapshot()?.userId;
    if (!linkedBy) {
      this.snack.open('Session expirée', 'Fermer', { duration: 3000 });
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
        this.snack.open('Preuve liée — scores recalculés', 'OK', { duration: 2500 });
        this.loadReports();
      },
      error: err => {
        this.linking = false;
        const msg = err?.status === 409 ? 'Cette preuve est déjà liée à cette exigence'
          : 'Échec de la liaison de preuve';
        this.snack.open(msg, 'Fermer', { duration: 3000 });
      }
    });
  }

  unlinkEvidence(ev: EvidenceResponse): void {
    this.svc.unlinkEvidence(this.adoptionId, ev.id).subscribe({
      next: () => {
        this.snack.open('Preuve retirée', 'OK', { duration: 2000 });
        this.loadReports();
      },
      error: () => this.snack.open('Échec du retrait', 'Fermer', { duration: 3000 })
    });
  }

  // ---- Dossier ----

  generateDossier(): void {
    this.generatingDossier = true;
    this.svc.generateDossier(this.adoptionId).subscribe({
      next: d => {
        this.dossier = d;
        this.generatingDossier = false;
        this.snack.open('Dossier généré et ancré (SHA-256)', 'OK', { duration: 2500 });
      },
      error: () => {
        this.generatingDossier = false;
        this.snack.open('Échec de la génération du dossier', 'Fermer', { duration: 3000 });
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
        this.snack.open('Certification à blanc simulée et ancrée', 'OK', { duration: 2500 });
      },
      error: () => {
        this.runningCertBlanc = false;
        this.snack.open('Échec de la simulation', 'Fermer', { duration: 3000 });
      }
    });
  }

  // ---- helpers UI ----

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
